plugins {
    java
    id("org.springframework.boot") version "4.1.1"
    id("io.spring.dependency-management") version "1.1.7"
    id("org.sonarqube") version "6.0.1.5171"
}

group = "com.example"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {

    // ============================================================
    // WEB
    // ============================================================

    implementation("org.springframework.boot:spring-boot-starter-web")


    // ============================================================
    // SECURITY / JWT
    // ============================================================

    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")


    // ============================================================
    // PERSISTENCE
    // ============================================================

    implementation("org.springframework.boot:spring-boot-starter-data-jpa")

    runtimeOnly("org.postgresql:postgresql")


    // ============================================================
    // VALIDATION
    // ============================================================

    implementation("org.springframework.boot:spring-boot-starter-validation")


    // ============================================================
    // DATABASE MIGRATIONS
    // ============================================================

    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")


    // ============================================================
    // API DOCUMENTATION
    // ============================================================

    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.1.0")


    // ============================================================
    // TESTING
    // ============================================================

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")

    testImplementation("org.testcontainers:testcontainers-junit-jupiter:2.0.3")
    testImplementation("org.testcontainers:testcontainers-postgresql:2.0.3")
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}