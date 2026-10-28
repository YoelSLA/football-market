package footballmarket.repositories;

import static org.assertj.core.api.Assertions.assertThat;

import footballmarket.support.TestcontainersConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/** Contrato de la función/index instalados por Flyway en PostgreSQL aislado. */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@Tag("integration")
class PlayerCatalogSearchMigrationTest {
  @Autowired private JdbcTemplate jdbc;

  @Nested
  @DisplayName("Normalización de búsqueda del catálogo")
  class Normalization {
    @ParameterizedTest
    @CsvSource({"José,jose", "JOSÉ,jose", "Álvarez,alvarez", "Jósé,jose", "Á, a"})
    @DisplayName("Normaliza mayúsculas y marcas Unicode sin modificar el texto original")
    void normalizesAccents(String original, String expected) {
      // Act
      String normalized =
          jdbc.queryForObject("select public.catalog_normalize_name(?)", String.class, original);
      // Assert
      assertThat(normalized).isEqualTo(expected);
    }

    @Test
    @DisplayName("El índice GIN utiliza la función inmutable y pg_trgm")
    void indexesNormalizedExpression() {
      // Act
      String definition =
          jdbc.queryForObject(
              "select indexdef from pg_indexes where schemaname='public' and indexname='ix_players_catalog_name_trgm'",
              String.class);
      String volatility =
          jdbc.queryForObject(
              "select provolatile::text from pg_proc where oid='public.catalog_normalize_name(text)'::regprocedure",
              String.class);
      // Assert
      assertThat(definition).contains("USING gin", "catalog_normalize_name", "gin_trgm_ops");
      assertThat(volatility).isEqualTo("i");
    }

    @Test
    @DisplayName("Permite nombres cortos y conserva comodines cuando se escapan explícitamente")
    void matchesLiteralPattern() {
      // Act
      Boolean literal =
          jdbc.queryForObject(
              "select public.catalog_normalize_name(?) like ? escape '!'",
              Boolean.class,
              "Á_%!",
              "%a!_!%!!%");
      String empty =
          jdbc.queryForObject("select public.catalog_normalize_name(?)", String.class, "  ");
      // Assert
      assertThat(literal).isTrue();
      assertThat(empty).isEmpty();
    }
  }
}
