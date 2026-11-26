package footballmarket.common.presentation;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Configura la auditoría transversal de los endpoints REST propios. */
@Configuration
public class HttpMvcConfiguration implements WebMvcConfigurer {

  private final RequestAuditInterceptor requestAuditInterceptor;

  public HttpMvcConfiguration(RequestAuditInterceptor requestAuditInterceptor) {
    this.requestAuditInterceptor = requestAuditInterceptor;
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(this.requestAuditInterceptor).addPathPatterns("/api/**");
  }
}
