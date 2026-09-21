package footballmarket.controllers;

import footballmarket.controllers.dtos.requests.LoginRequestDTO;
import footballmarket.controllers.dtos.requests.RegisterRequestDTO;
import footballmarket.controllers.dtos.responses.CurrentUserResponseDTO;
import footballmarket.controllers.dtos.responses.LoginResponseDTO;
import footballmarket.controllers.mappers.UserMapper;
import footballmarket.services.AuthenticationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
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
   * @return identificador y email persistidos del usuario
   */
  @Operation(
      summary = "Consultar usuario actual",
      description = "Recupera el id y email actuales del usuario persistido asociado a la sesión.")
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
      description = "Registra un nuevo usuario en el sistema.")
  @ApiResponse(responseCode = "201", description = "Usuario registrado correctamente.")
  @ApiResponse(responseCode = "400", description = "Los datos proporcionados no son válidos.")
  @ApiResponse(responseCode = "409", description = "El email ya está registrado.")
  @PostMapping("/register")
  public ResponseEntity<Void> register(@Valid @RequestBody RegisterRequestDTO request) {

    this.authenticationService.register(UserMapper.toModel(request));

    return ResponseEntity.status(HttpStatus.CREATED).build();
  }

  @Operation(
      summary = "Iniciar sesión",
      description = "Autentica al usuario y genera un token JWT.")
  @ApiResponse(responseCode = "200", description = "Inicio de sesión exitoso.")
  @ApiResponse(responseCode = "400", description = "Los datos proporcionados no son válidos.")
  @ApiResponse(
      responseCode = "401",
      description = "Las credenciales proporcionadas son incorrectas.")
  @PostMapping("/login")
  public ResponseEntity<LoginResponseDTO> login(@Valid @RequestBody LoginRequestDTO request) {

    String token = this.authenticationService.login(request.email(), request.password());

    LoginResponseDTO response = new LoginResponseDTO(token);

    return ResponseEntity.ok(response);
  }
}
