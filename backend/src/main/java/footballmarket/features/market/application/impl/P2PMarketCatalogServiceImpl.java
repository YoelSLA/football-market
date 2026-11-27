package footballmarket.features.market.application.impl;

import footballmarket.common.config.QueryCacheConfiguration;
import footballmarket.features.auth.domain.User;
import footballmarket.features.auth.infraestructure.UserRepository;
import footballmarket.features.market.application.P2PMarketCatalogService;
import footballmarket.features.market.application.P2PMarketQuery;
import footballmarket.features.market.application.internal.P2PMarketAssembler;
import footballmarket.features.market.domain.records.P2PMarketCatalog;
import footballmarket.features.market.domain.records.P2PMarketPlayer;
import footballmarket.features.market.domain.records.P2PMarketPriceRange;
import footballmarket.features.market.infraestructure.P2PMarketRepository;
import footballmarket.features.market.infraestructure.projections.P2PMarketPlayerProjection;
import footballmarket.features.market.infraestructure.projections.P2PMarketPriceRangeProjection;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.interceptor.SimpleKey;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementación del servicio de catálogo del Mercado P2P con publicaciones agrupadas por jugador.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class P2PMarketCatalogServiceImpl implements P2PMarketCatalogService {

  private final UserRepository users;
  private final P2PMarketRepository p2pMarketRepository;
  private final Clock marketClock;
  private final CacheManager cacheManager;

  @Override
  public P2PMarketCatalog getP2PCatalog(String viewerEmail, P2PMarketQuery query) {
    Long viewerId = this.resolveViewerId(viewerEmail);
    Cache cache = this.cacheManager.getCache(QueryCacheConfiguration.P2P_MARKET);
    SimpleKey cacheKey = new SimpleKey(viewerId, query);
    P2PMarketCatalog cached = cache.get(cacheKey, P2PMarketCatalog.class);
    if (cached != null) {
      return cached;
    }

    OffsetDateTime now = OffsetDateTime.now(this.marketClock);

    P2PMarketPriceRangeProjection priceRangeProjection =
        this.p2pMarketRepository.findGlobalP2PMarketPriceRange(viewerId, now);
    P2PMarketPriceRange priceRange =
        priceRangeProjection != null
            ? new P2PMarketPriceRange(priceRangeProjection.getMin(), priceRangeProjection.getMax())
            : new P2PMarketPriceRange(null, null);

    long totalElements =
        this.p2pMarketRepository.countP2PMarketPlayers(
            viewerId,
            now,
            query.search(),
            query.leagueId(),
            query.teamId(),
            query.position(),
            query.minPrice(),
            query.maxPrice());

    if (totalElements == 0) {
      Page<P2PMarketPlayer> page =
          new PageImpl<>(Collections.emptyList(), PageRequest.of(query.page(), query.size()), 0);
      P2PMarketCatalog catalog = new P2PMarketCatalog(page, priceRange);
      cache.put(cacheKey, catalog);
      return catalog;
    }

    int offset = query.page() * query.size();
    if (offset >= totalElements) {
      Page<P2PMarketPlayer> page =
          new PageImpl<>(
              Collections.emptyList(), PageRequest.of(query.page(), query.size()), totalElements);
      P2PMarketCatalog catalog = new P2PMarketCatalog(page, priceRange);
      cache.put(cacheKey, catalog);
      return catalog;
    }

    List<P2PMarketPlayerProjection> projections =
        this.p2pMarketRepository.findP2PMarketPlayers(
            viewerId,
            now,
            query.search(),
            query.leagueId(),
            query.teamId(),
            query.position(),
            query.minPrice(),
            query.maxPrice(),
            query.sortProperty(),
            query.sortDirection(),
            query.size(),
            offset);

    List<P2PMarketPlayer> players = projections.stream().map(P2PMarketAssembler::toDomain).toList();

    Page<P2PMarketPlayer> page =
        new PageImpl<>(players, PageRequest.of(query.page(), query.size()), totalElements);
    P2PMarketCatalog catalog = new P2PMarketCatalog(page, priceRange);
    cache.put(cacheKey, catalog);
    return catalog;
  }

  private Long resolveViewerId(String viewerEmail) {
    if (viewerEmail == null || viewerEmail.isBlank()) {
      return null;
    }
    return this.users
        .findByEmail(viewerEmail.toLowerCase(Locale.ROOT))
        .map(User::getId)
        .orElse(null);
  }
}
