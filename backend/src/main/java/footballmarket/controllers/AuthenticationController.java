package footballmarket.controllers;

import footballmarket.controllers.dtos.requests.LoginRequestDTO;
import footballmarket.controllers.dtos.requests.RegisterRequestDTO;
import footballmarket.controllers.dtos.responses.CurrentUserResponseDTO;
import footballmarket.controllers.dtos.responses.ErrorResponseDTO;
import footballmarket.controllers.dtos.responses.LoginResponseDTO;
import footballmarket.controllers.mappers.UserMapper;
import footballmarket.services.AuthenticationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

/** Expone el registro, inicio de sesión y consulta de la identidad actual. */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(
    name = "Autenticación",
    description = "Endpoints relacionados con el registro e inicio de sesión de usuarios.")
public class AuthenticationController {

  private final AuthenticationService authenticationService;

  /**
   * Consulta la identidad actual del usuario de la sesión JWT validada.
   *
   * @param jwt principal autenticado por Spring Security
   * @return identificador, nombre de usuario, email y rol persistidos del usuario
   */
  @Operation(
      summary = "Consultar usuario actual",
      description =
          "Recupera el id, el nombre de usuario, el email y el rol actuales del usuario persistido"
              + " asociado a la sesión. El rol es informativo: el backend evalúa toda autorización a"
              + " partir del rol persistido.")
  @SecurityRequirement(name = "bearerAuth")
  @ApiResponse(responseCode = "200", description = "Identidad actual del usuario.")
  @ApiResponse(
      responseCode = "401",
      description = "La sesión no está autenticada.",
      content = @Content)
  @GetMapping("/me")
  public ResponseEntity<CurrentUserResponseDTO> getCurrentUser(@AuthenticationPrincipal Jwt jwt) {
    return ResponseEntity.ok(
        UserMapper.toCurrentUserResponse(
            this.authenticationService.getCurrentUser(jwt.getSubject())));
  }

  @Operation(
      summary = "Registrar usuario",
      description =
          "Registra un nuevo usuario en el sistema con un nombre de usuario único y con el rol"
              + " USER. El rol no se recibe del cliente y no puede solicitarse SUPERUSER desde este"
              + " endpoint.")
  @ApiResponse(responseCode = "201", description = "Usuario registrado correctamente.")
  @ApiResponse(
      responseCode = "400",
      description =
          "Los datos proporcionados no son válidos, por ejemplo un nombre de usuario vacío, fuera"
              + " del rango de longitud o fuera del formato admitido (INVALID_REQUEST).",
      content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
  @ApiResponse(
      responseCode = "409",
      description =
          "El email ya está registrado (EMAIL_ALREADY_REGISTERED) o el nombre de usuario ya está en"
              + " uso (USERNAME_ALREADY_TAKEN).",
      content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
  @PostMapping("/register")
  public ResponseEntity<Void> register(@Valid @RequestBody RegisterRequestDTO request) {

    this.authenticationService.register(UserMapper.toModel(request));

    return ResponseEntity.status(HttpStatus.CREATED).build();
  }

  @Operation(
      summary = "Iniciar sesión",
      description =
          "Autentica al usuario con su correo electrónico y su contraseña y genera un token JWT. El"
              + " nombre de usuario no es una credencial.")
  @ApiResponse(responseCode = "200", description = "Inicio de sesión exitoso.")
  @ApiResponse(
      responseCode = "400",
      description = "Los datos proporcionados no son válidos (INVALID_REQUEST).",
      content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
  @ApiResponse(
      responseCode = "401",
      description = "Las credenciales proporcionadas son incorrectas (INVALID_CREDENTIALS).",
      content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
  @PostMapping("/login")
  public ResponseEntity<LoginResponseDTO> login(@Valid @RequestBody LoginRequestDTO request) {

    String token = this.authenticationService.login(request.email(), request.password());

    LoginResponseDTO response = new LoginResponseDTO(token);

    return ResponseEntity.ok(response);
  }
}
