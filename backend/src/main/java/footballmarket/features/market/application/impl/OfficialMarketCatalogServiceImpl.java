package footballmarket.features.market.application.impl;

import footballmarket.features.market.application.OfficialMarketCatalogService;
import footballmarket.features.market.application.OfficialMarketQuery;
import footballmarket.features.market.application.internal.OfficialMarketAssembler;
import footballmarket.features.market.domain.records.OfficialMarketCatalog;
import footballmarket.features.market.domain.records.OfficialMarketPlayer;
import footballmarket.features.market.domain.records.OfficialMarketPriceRange;
import footballmarket.features.market.infraestructure.OfficialMarketRepository;
import footballmarket.features.market.infraestructure.projections.OfficialMarketPlayerProjection;
import footballmarket.features.market.infraestructure.projections.OfficialMarketPriceRangeProjection;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementación del servicio de catálogo del Mercado Oficial con cotizaciones integradas.
 *
 * <p>Aplica una estrategia desacoplada de conteo y datos para maximizar el rendimiento y delega
 * directamente en {@link OfficialMarketRepository} sin introducir dependencias Service → Service.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OfficialMarketCatalogServiceImpl implements OfficialMarketCatalogService {

  private final OfficialMarketRepository officialMarketRepository;

  @Override
  public OfficialMarketCatalog getOfficialCatalog(OfficialMarketQuery query) {
    OfficialMarketPriceRangeProjection priceRangeProjection =
        this.officialMarketRepository.findGlobalOfficialMarketPriceRange();
    OfficialMarketPriceRange priceRange =
        priceRangeProjection != null
            ? new OfficialMarketPriceRange(
                priceRangeProjection.getMin(), priceRangeProjection.getMax())
            : new OfficialMarketPriceRange(null, null);

    long totalElements = this.countTotalElements(query);
    if (totalElements == 0) {
      return new OfficialMarketCatalog(
          new PageImpl<>(Collections.emptyList(), PageRequest.of(query.page(), query.size()), 0),
          priceRange);
    }

    int offset = query.page() * query.size();
    if (offset >= totalElements) {
      return new OfficialMarketCatalog(
          new PageImpl<>(
              Collections.emptyList(), PageRequest.of(query.page(), query.size()), totalElements),
          priceRange);
    }

    List<OfficialMarketPlayerProjection> projections =
        this.officialMarketRepository.findOfficialMarketPlayers(
            query.name(),
            query.leagueId(),
            query.teamId(),
            query.position(),
            query.nationality(),
            query.hasQuote(),
            query.minPrice(),
            query.maxPrice(),
            query.sortProperty(),
            query.sortDirection(),
            query.size(),
            offset);

    List<OfficialMarketPlayer> players =
        projections.stream().map(OfficialMarketAssembler::toDomain).toList();

    return new OfficialMarketCatalog(
        new PageImpl<>(players, PageRequest.of(query.page(), query.size()), totalElements),
        priceRange);
  }

  private long countTotalElements(OfficialMarketQuery query) {
    if (query.hasFinancialFilters()) {
      return this.officialMarketRepository.countOfficialMarketPlayersWithFinancialFilters(
          query.name(),
          query.leagueId(),
          query.teamId(),
          query.position(),
          query.nationality(),
          query.hasQuote(),
          query.minPrice(),
          query.maxPrice());
    }

    return this.officialMarketRepository.countOfficialMarketPlayersWithoutFinancialFilters(
        query.name(), query.leagueId(), query.teamId(), query.position(), query.nationality());
  }
}
