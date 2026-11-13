package footballmarket.features.market.application.impl;

import footballmarket.features.market.application.OfficialMarketCatalogService;
import footballmarket.features.market.application.OfficialMarketQuery;
import footballmarket.features.market.domain.records.OfficialMarketPlayer;
import footballmarket.features.market.infraestructure.OfficialMarketRepository;
import footballmarket.features.market.infraestructure.projections.OfficialMarketPlayerProjection;
import footballmarket.features.market.presentation.OfficialMarketMapper;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
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
  public Page<OfficialMarketPlayer> getOfficialCatalog(OfficialMarketQuery query) {
    long totalElements = this.countTotalElements(query);
    if (totalElements == 0) {
      return new PageImpl<>(Collections.emptyList(), PageRequest.of(query.page(), query.size()), 0);
    }

    int offset = query.page() * query.size();
    if (offset >= totalElements) {
      return new PageImpl<>(
          Collections.emptyList(), PageRequest.of(query.page(), query.size()), totalElements);
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
        projections.stream().map(OfficialMarketMapper::toDomain).toList();

    return new PageImpl<>(players, PageRequest.of(query.page(), query.size()), totalElements);
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
