package footballmarket.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "football-data")
public record FootballDataProperties(String apiKey, String baseUrl) {}
