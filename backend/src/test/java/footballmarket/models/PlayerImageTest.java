package footballmarket.models;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class PlayerImageTest {
  @Nested
  @DisplayName("Prioridad y conservación de imágenes")
  class Images {
    @Test
    @DisplayName("Prioriza recorte y deja miniatura distinta como alternativa")
    void prioritizesCutout() {
      Player player = new Player("Name", "Team", "League", "Forward");
      player.applyImages("https://thesportsdb.com/cutout", "https://thesportsdb.com/thumb");
      assertThat(player.getImageUrl()).isEqualTo("https://thesportsdb.com/cutout");
      assertThat(player.getFallbackImageUrl()).isEqualTo("https://thesportsdb.com/thumb");
    }

    @Test
    @DisplayName("Promueve la miniatura a principal cuando falta el recorte")
    void promotesThumbnail() {
      Player player = new Player("Name", "Team", "League", "Forward");
      player.applyImages(null, "https://thesportsdb.com/thumb");
      assertThat(player.getImageUrl()).isEqualTo("https://thesportsdb.com/thumb");
      assertThat(player.getFallbackImageUrl()).isNull();
    }

    @Test
    @DisplayName("Evita duplicar imágenes iguales")
    void avoidsDuplicates() {
      Player player = new Player("Name", "Team", "League", "Forward");
      player.applyImages("https://thesportsdb.com/photo", "https://thesportsdb.com/photo");
      assertThat(player.getFallbackImageUrl()).isNull();
    }

    @Test
    @DisplayName("Conserva imágenes previas al refrescar sin sustitución válida")
    void preservesPreviousUrls() {
      Player player = new Player("Name", "Team", "League", "Forward");
      player.applyImages("https://thesportsdb.com/a", "https://thesportsdb.com/b");
      player.applyImages(null, null);
      assertThat(player.getImageUrl()).isEqualTo("https://thesportsdb.com/a");
      assertThat(player.getFallbackImageUrl()).isEqualTo("https://thesportsdb.com/b");
    }

    @Test
    @DisplayName("Eleva un recorte nuevo sin perder la miniatura previa")
    void upgradesCutout() {
      Player player = new Player("Name", "Team", "League", "Forward");
      player.applyImages(null, "https://thesportsdb.com/thumb");
      player.applyImages("https://thesportsdb.com/cutout", null);
      assertThat(player.getImageUrl()).isEqualTo("https://thesportsdb.com/cutout");
      assertThat(player.getFallbackImageUrl()).isEqualTo("https://thesportsdb.com/thumb");
    }
  }
}
