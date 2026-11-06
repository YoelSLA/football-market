package footballmarket.features.auth.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import footballmarket.features.auth.infraestructure.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;

@ExtendWith(MockitoExtension.class)
@org.junit.jupiter.api.Tag("lightweight")
class UserRepositoryIdentityValidatorTest {

  @Mock private UserRepository userRepository;
  @InjectMocks private UserRepositoryIdentityValidator validator;

  @Nested
  @DisplayName("Validación del sujeto")
  class SubjectValidation {

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t", "\n"})
    @DisplayName("Devuelve false para sujetos nulos o en blanco sin consultar el repositorio")
    void devuelveFalseParaSujetoNuloOEnBlanco(String subject) {
      assertThat(validator.exists(subject)).isFalse();
      verifyNoInteractions(userRepository);
    }

    @Test
    @DisplayName("Normaliza el correo a minúsculas al consultar el repositorio")
    void normalizaCorreoAMinusculas() {
      when(userRepository.existsByEmail("user@example.com")).thenReturn(true);

      assertThat(validator.exists("USER@EXAMPLE.COM")).isTrue();
      verify(userRepository).existsByEmail("user@example.com");
    }

    @Test
    @DisplayName("Devuelve false si el usuario no existe en la base de datos")
    void devuelveFalseSiUsuarioNoExiste() {
      when(userRepository.existsByEmail("missing@example.com")).thenReturn(false);

      assertThat(validator.exists("missing@example.com")).isFalse();
      verify(userRepository).existsByEmail("missing@example.com");
    }

    @Test
    @DisplayName("Propaga excepciones de infraestructura de persistencia sin capturarlas")
    void propagaExcepcionesDeInfraestructura() {
      when(userRepository.existsByEmail("db-error@example.com"))
          .thenThrow(new DataAccessResourceFailureException("Base de datos no disponible"));

      assertThatThrownBy(() -> validator.exists("db-error@example.com"))
          .isInstanceOf(DataAccessResourceFailureException.class);
    }
  }
}
