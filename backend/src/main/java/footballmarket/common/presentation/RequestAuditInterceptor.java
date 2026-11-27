package footballmarket.common.presentation;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.MethodParameter;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

/** Registra de forma acotada la auditoría de cada petición REST atendida por Spring MVC. */
@Component
public final class RequestAuditInterceptor implements HandlerInterceptor {

  private static final Logger log = LoggerFactory.getLogger(RequestAuditInterceptor.class);
  private static final String STARTED_AT_ATTRIBUTE =
      RequestAuditInterceptor.class.getName() + ".startedAt";
  private static final String TIMESTAMP_ATTRIBUTE =
      RequestAuditInterceptor.class.getName() + ".timestamp";
  private static final Pattern SENSITIVE_PARAMETER =
      Pattern.compile("(?i).*(password|passwd|token|secret|credential|authorization|api.?key).*");

  @Override
  public boolean preHandle(
      HttpServletRequest request, HttpServletResponse response, Object handler) {
    request.setAttribute(STARTED_AT_ATTRIBUTE, System.nanoTime());
    request.setAttribute(TIMESTAMP_ATTRIBUTE, Instant.now());
    return true;
  }

  @Override
  public void afterCompletion(
      HttpServletRequest request,
      HttpServletResponse response,
      Object handler,
      Exception exception) {
    Object startedAtValue = request.getAttribute(STARTED_AT_ATTRIBUTE);
    if (!(startedAtValue instanceof Long startedAt)) {
      return;
    }

    Object timestamp = request.getAttribute(TIMESTAMP_ATTRIBUTE);
    Object matchingPattern = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
    String operation =
        matchingPattern == null ? request.getRequestURI() : matchingPattern.toString();
    String user = authenticatedUser();
    Map<String, List<String>> parameters = auditedParameters(request, handler);
    long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);

    log.info(
        "HTTP request audit: timestamp={} user={} method={} operation={} parameters={} durationMs={}",
        timestamp,
        user,
        request.getMethod(),
        operation,
        parameters,
        durationMs);
  }

  private String authenticatedUser() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null
        || !authentication.isAuthenticated()
        || authentication instanceof AnonymousAuthenticationToken) {
      return "anonymous";
    }
    String name = authentication.getName();
    return StringUtils.hasText(name) ? name : "anonymous";
  }

  private Map<String, List<String>> auditedParameters(HttpServletRequest request, Object handler) {
    Set<String> recognizedNames = recognizedParameterNames(handler);
    Map<String, List<String>> parameters = new TreeMap<>();
    request
        .getParameterMap()
        .forEach(
            (name, values) -> {
              if (recognizedNames.contains(name) && isAuditableParameter(name) && values != null) {
                parameters.put(name, List.of(values));
              }
            });
    return parameters;
  }

  private Set<String> recognizedParameterNames(Object handler) {
    if (!(handler instanceof HandlerMethod handlerMethod)) {
      return Set.of();
    }

    Set<String> names = new HashSet<>();
    for (MethodParameter parameter : handlerMethod.getMethodParameters()) {
      RequestParam requestParam = parameter.getParameterAnnotation(RequestParam.class);
      if (requestParam != null) {
        String name =
            StringUtils.hasText(requestParam.name())
                ? requestParam.name()
                : parameter.getParameterName();
        if (StringUtils.hasText(name)) {
          names.add(name);
        }
        continue;
      }

      if (parameter.hasParameterAnnotation(ModelAttribute.class)
          && parameter.getParameterType().isRecord()) {
        for (var component : parameter.getParameterType().getRecordComponents()) {
          names.add(component.getName());
        }
      }
    }
    return names;
  }

  private boolean isAuditableParameter(String name) {
    String normalizedName = name.replace("_", "").replace("-", "").toLowerCase(Locale.ROOT);
    boolean identifier =
        normalizedName.equals("id")
            || normalizedName.endsWith("id")
            || normalizedName.endsWith("ids")
            || normalizedName.endsWith("identifier")
            || normalizedName.endsWith("identifiers");
    return !identifier && !SENSITIVE_PARAMETER.matcher(name).matches();
  }
}
