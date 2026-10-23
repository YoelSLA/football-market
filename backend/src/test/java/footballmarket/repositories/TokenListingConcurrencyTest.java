package footballmarket.repositories;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import footballmarket.models.TokenListing;
import footballmarket.models.User;
import footballmarket.models.enums.TokenListingStatus;
import footballmarket.models.records.BuyListingRequest;
import footballmarket.models.records.SellOrderRequest;
import footballmarket.services.exceptions.InsufficientStockException;
import footballmarket.services.exceptions.ListingNotAvailableException;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Carreras reales en PostgreSQL, con liberación dentro de transacciones y sin esperas temporales.
 */
@org.junit.jupiter.api.Tag("integration")
class TokenListingConcurrencyTest extends ListingRepositorySupport {
  /** Señales de coordinación acotadas; nunca simulan el paso del tiempo de negocio. */
  private void awaitSignal(CountDownLatch signal) {
    try {
      if (!signal.await(15, TimeUnit.SECONDS)) {
        throw new AssertionError("No se alcanzó el punto de coordinación de la carrera");
      }
    } catch (InterruptedException interrupted) {
      Thread.currentThread().interrupt();
      throw new AssertionError("La coordinación fue interrumpida", interrupted);
    }
  }

  /** Observa una espera real entre las sesiones participantes por una consulta de tenencias. */
  private void awaitHoldingWait(int waiter, int blocker, Future<?> pending) {
    long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(15);
    while (System.nanoTime() < deadline) {
      Boolean blocked =
          jdbc.queryForObject(
              "select exists (select 1 from pg_stat_activity a where a.pid = ?"
                  + " and a.wait_event_type = 'Lock' and ? = any(pg_blocking_pids(a.pid))"
                  + " and a.query ilike '%player_tokens%')",
              Boolean.class, waiter, blocker);
      if (Boolean.TRUE.equals(blocked)) {
        return;
      }
      if (pending.isDone() || Thread.currentThread().isInterrupted()) {
        throw new AssertionError(
            "La operación terminó o se interrumpió sin esperar por la tenencia");
      }
    }
    throw new AssertionError("PostgreSQL no acreditó la espera por la tenencia antes del timeout");
  }

  @Nested
  @DisplayName("Regresión C1: compra, vencimiento y nuevas reservas")
  class ExpirationReservationRace {
    @Test
    @DisplayName(
        "Una compra que vence esperando la tenencia no consume los tokens de la nueva reserva")
    void rechazaCompraVencidaDuranteEspera() throws Exception {
      // Arrange
      TokenListing old = publish(5);
      long sellerBefore = balance(seller);
      long buyerBefore = balance(buyer);
      AtomicReference<Instant> now = new AtomicReference<>(NOW.toInstant());
      when(marketClock.instant()).thenAnswer(invocation -> now.get());
      AtomicInteger publisherPid = new AtomicInteger();
      AtomicInteger buyerPid = new AtomicInteger();
      CountDownLatch holdingLocked = new CountDownLatch(1);
      CountDownLatch buyerStarted = new CountDownLatch(1);
      CountDownLatch releasePublisher = new CountDownLatch(1);
      try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
        try {
          Future<Long> publication =
              executor.submit(
                  () ->
                      transaction()
                          .execute(
                              status -> {
                                publisherPid.set(
                                    jdbc.queryForObject("select pg_backend_pid()", Integer.class));
                                holdings.lockHoldingsInAscendingOwnerOrder(
                                    player.getId(), List.of(seller.getId()));
                                holdingLocked.countDown();
                                awaitSignal(releasePublisher);
                                return publish(20).getId();
                              }));
          awaitSignal(holdingLocked);
          Future<String> purchase =
              executor.submit(
                  () -> {
                    try {
                      return transaction()
                          .execute(
                              status -> {
                                buyerPid.set(
                                    jdbc.queryForObject("select pg_backend_pid()", Integer.class));
                                buyerStarted.countDown();
                                buyService.buyListing(
                                    buyer.getEmail(), new BuyListingRequest(old.getId()));
                                return "BOUGHT";
                              });
                    } catch (ListingNotAvailableException expected) {
                      return "EXPIRED";
                    }
                  });
          awaitSignal(buyerStarted);

          // Act: el observador es una tercera sesión, fuera de las transacciones competidoras.
          awaitHoldingWait(buyerPid.get(), publisherPid.get(), purchase);
          now.set(old.getExpiresAt().toInstant());
          releasePublisher.countDown();
          Long newId = publication.get(30, TimeUnit.SECONDS);
          String outcome = purchase.get(30, TimeUnit.SECONDS);

          // Assert
          assertThat(outcome).isEqualTo("EXPIRED");
          assertThat(newId).isNotEqualTo(old.getId());
          // Verify: el retorno de ambos futures implica commit/rollback ya finalizados.
          TokenListing stored = listings.findById(old.getId()).orElseThrow();
          assertThat(stored.getStatus()).isEqualTo(TokenListingStatus.ACTIVE);
          assertThat(stored.effectiveStatus(old.getExpiresAt()))
              .isEqualTo(TokenListingStatus.EXPIRED);
          assertThat(stored.getClosedAt()).isNull();
          assertThat(listings.findById(newId).orElseThrow().getQuantity()).isEqualTo(20);
          assertThat(listings.reservedQuantity(seller.getId(), player.getId(), old.getExpiresAt()))
              .isEqualTo(20);
          assertThat(quantity(seller)).isEqualTo(20);
          assertThat(holdings.findByPlayerIdAndOwnerId(player.getId(), buyer.getId())).isEmpty();
          assertThat(balance(seller)).isEqualTo(sellerBefore);
          assertThat(balance(buyer)).isEqualTo(buyerBefore);
          assertThat(
                  jdbc.queryForObject(
                      "select count(*) from market_transactions where listing_id = ?",
                      Long.class,
                      old.getId()))
              .isZero();
        } finally {
          releasePublisher.countDown();
        }
      }
    }

    @Test
    @DisplayName(
        "Una compra ya validada conserva los tokens hasta confirmar aunque venza y bloquea la nueva reserva")
    void conservaTenenciaHastaCommit() throws Exception {
      // Arrange
      TokenListing old = publish(5);
      long sellerBefore = balance(seller);
      long buyerBefore = balance(buyer);
      AtomicReference<Instant> now = new AtomicReference<>(NOW.toInstant());
      when(marketClock.instant()).thenAnswer(invocation -> now.get());
      AtomicInteger buyerPid = new AtomicInteger();
      AtomicInteger publisherPid = new AtomicInteger();
      CountDownLatch purchaseReady = new CountDownLatch(1);
      CountDownLatch publisherStarted = new CountDownLatch(1);
      CountDownLatch releaseBuyer = new CountDownLatch(1);
      try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
        try {
          Future<Long> purchase =
              executor.submit(
                  () ->
                      transaction()
                          .execute(
                              status -> {
                                buyerPid.set(
                                    jdbc.queryForObject("select pg_backend_pid()", Integer.class));
                                Long transactionId =
                                    buyService
                                        .buyListing(
                                            buyer.getEmail(), new BuyListingRequest(old.getId()))
                                        .getId();
                                purchaseReady.countDown();
                                awaitSignal(releaseBuyer);
                                return transactionId;
                              }));
          awaitSignal(purchaseReady);
          now.set(old.getExpiresAt().toInstant());
          Future<String> publication =
              executor.submit(
                  () -> {
                    try {
                      return transaction()
                          .execute(
                              status -> {
                                publisherPid.set(
                                    jdbc.queryForObject("select pg_backend_pid()", Integer.class));
                                publisherStarted.countDown();
                                publish(20);
                                return "PUBLISHED";
                              });
                    } catch (InsufficientStockException expected) {
                      return "STOCK";
                    }
                  });
          awaitSignal(publisherStarted);

          // Act
          awaitHoldingWait(publisherPid.get(), buyerPid.get(), publication);
          releaseBuyer.countDown();
          Long transactionId = purchase.get(30, TimeUnit.SECONDS);
          String outcome = publication.get(30, TimeUnit.SECONDS);
          // Assert
          assertThat(outcome).isEqualTo("STOCK");
          assertThat(transactionId).isNotNull();
          // Verify
          TokenListing stored = listings.findById(old.getId()).orElseThrow();
          assertThat(stored.getStatus()).isEqualTo(TokenListingStatus.SOLD);
          assertThat(stored.getClosedAt()).isEqualTo(NOW);
          assertThat(quantity(seller)).isEqualTo(15);
          assertThat(quantity(buyer)).isEqualTo(5);
          assertThat(balance(seller)).isEqualTo(sellerBefore + 500);
          assertThat(balance(buyer)).isEqualTo(buyerBefore - 500);
          assertThat(
                  jdbc.queryForObject(
                      "select sum(quantity) from player_tokens where player_id = ?",
                      Long.class,
                      player.getId()))
              .isEqualTo(100);
          assertThat(
                  jdbc.queryForObject(
                      "select count(*) from market_transactions where listing_id = ?",
                      Long.class,
                      old.getId()))
              .isEqualTo(1);
          assertThat(listings.findBySellerId(seller.getId())).hasSize(1);
          TokenListing positiveControl =
              listingService.publish(seller.getEmail(), new SellOrderRequest(player.getId(), 15));
          assertThat(positiveControl.getQuantity()).isEqualTo(15);
          assertThat(listings.reservedQuantity(seller.getId(), player.getId(), old.getExpiresAt()))
              .isEqualTo(15);
        } finally {
          releaseBuyer.countDown();
        }
      }
    }
  }

  private List<String> race(Callable<String> first, Callable<String> second) throws Exception {
    CyclicBarrier barrier = new CyclicBarrier(2);
    try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
      Future<String> left = executor.submit(() -> compete(barrier, first));
      Future<String> right = executor.submit(() -> compete(barrier, second));
      return List.of(left.get(30, TimeUnit.SECONDS), right.get(30, TimeUnit.SECONDS));
    }
  }

  private String compete(CyclicBarrier barrier, Callable<String> operation) {
    return transaction()
        .execute(
            status -> {
              try {
                barrier.await(15, TimeUnit.SECONDS);
                return operation.call();
              } catch (InsufficientStockException rejected) {
                status.setRollbackOnly();
                return "STOCK";
              } catch (ListingNotAvailableException rejected) {
                status.setRollbackOnly();
                return "UNAVAILABLE";
              } catch (Exception failure) {
                throw new AssertionError(
                    "La carrera no terminó con un resultado contractual", failure);
              }
            });
  }

  @Nested
  @DisplayName("Liquidaciones concurrentes")
  class Purchases {
    @Test
    @DisplayName(
        "Compra y cancelación concurrentes dejan un único cierre coherente, sin movimientos parciales")
    void compiteConCancelacion() throws Exception {
      // Arrange
      TokenListing listing = publish(5);
      long sellerBefore = balance(seller);
      long buyerBefore = balance(buyer);
      // Act
      List<String> outcomes =
          race(
              () -> {
                buyService.buyListing(buyer.getEmail(), new BuyListingRequest(listing.getId()));
                return "BUY";
              },
              () -> {
                listingService.cancel(seller.getEmail(), listing.getId());
                return "CANCEL";
              });
      // Assert
      assertThat(outcomes).contains("UNAVAILABLE");
      // Verify
      String state = listings.findById(listing.getId()).orElseThrow().getStatus().name();
      long audits =
          jdbc.queryForObject(
              "select count(*) from market_transactions where listing_id = ?",
              Long.class,
              listing.getId());
      if ("SOLD".equals(state)) {
        assertThat(outcomes).containsExactlyInAnyOrder("BUY", "UNAVAILABLE");
        assertThat(audits).isEqualTo(1);
        assertThat(quantity(seller)).isEqualTo(15);
        assertThat(quantity(buyer)).isEqualTo(5);
        assertThat(balance(seller)).isEqualTo(sellerBefore + 500);
        assertThat(balance(buyer)).isEqualTo(buyerBefore - 500);
      } else {
        assertThat(state).isEqualTo("CANCELLED");
        assertThat(outcomes).containsExactlyInAnyOrder("CANCEL", "UNAVAILABLE");
        assertThat(audits).isZero();
        assertThat(quantity(seller)).isEqualTo(20);
        assertThat(quantity(buyer)).isZero();
        assertThat(balance(seller)).isEqualTo(sellerBefore);
        assertThat(balance(buyer)).isEqualTo(buyerBefore);
      }
    }

    @Test
    @DisplayName("Dos compradores solo pueden confirmar una compra y una auditoría")
    void liquidaUnaVez() throws Exception {
      // Arrange
      TokenListing listing = publish(5);
      User otherBuyer = market.registerBuyer();
      long sellerBefore = balance(seller);
      // Act
      List<String> outcomes =
          race(
              () -> {
                buyService.buyListing(buyer.getEmail(), new BuyListingRequest(listing.getId()));
                return "OK";
              },
              () -> {
                buyService.buyListing(
                    otherBuyer.getEmail(), new BuyListingRequest(listing.getId()));
                return "OK";
              });
      // Assert
      assertThat(outcomes).containsExactlyInAnyOrder("OK", "UNAVAILABLE");
      // Verify
      assertThat(
              jdbc.queryForObject(
                  "select count(*) from market_transactions where listing_id = ?",
                  Long.class,
                  listing.getId()))
          .isEqualTo(1);
      assertThat(quantity(buyer) + quantity(otherBuyer)).isEqualTo(5);
      assertThat(quantity(seller)).isEqualTo(15);
      assertThat(balance(seller)).isEqualTo(sellerBefore + 500);
      assertThat(balance(buyer) + balance(otherBuyer)).isEqualTo(19500);
      assertThat(listings.findById(listing.getId()).orElseThrow().getStatus().name())
          .isEqualTo("SOLD");
    }
  }

  @Nested
  @DisplayName("Publicaciones concurrentes")
  class Publication {
    @Test
    @DisplayName("Solo una reserva se confirma cuando las dos juntas exceden la tenencia")
    void evitaSobreventa() throws Exception {
      // Arrange
      long sellerBefore = balance(seller);
      long buyerBefore = balance(buyer);
      long audits = transactions.findByPlayerIdOrderByExecutedAtDescIdDesc(player.getId()).size();
      // Act
      List<String> outcomes =
          race(
              () -> {
                publish(11);
                return "OK";
              },
              () -> {
                publish(11);
                return "OK";
              });
      // Assert
      assertThat(outcomes).containsExactlyInAnyOrder("OK", "STOCK");
      // Verify
      assertThat(reserved()).isEqualTo(11);
      assertThat(listings.findBySellerId(seller.getId())).hasSize(1);
      assertThat(quantity(seller)).isEqualTo(20);
      assertThat(balance(seller)).isEqualTo(sellerBefore);
      assertThat(balance(buyer)).isEqualTo(buyerBefore);
      assertThat(transactions.findByPlayerIdOrderByExecutedAtDescIdDesc(player.getId()))
          .hasSize((int) audits);
    }
  }
}
