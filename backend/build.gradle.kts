plugins {
    java
    id("org.springframework.boot") version "4.1.1"
    id("io.spring.dependency-management") version "1.1.7"
    id("org.sonarqube") version "7.3.1.8318"
    id("com.diffplug.spotless") version "7.2.1"
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

val springdocVersion = project.property("springdocVersion")

dependencies {

    // ============================================================
    // IMPLEMENTATION
    // ============================================================

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")

    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")

    implementation(
        "org.springdoc:springdoc-openapi-starter-webmvc-ui:$springdocVersion"
    )


    // ============================================================
    // LOMBOK
    // ============================================================

    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")


    // ============================================================
    // RUNTIME ONLY
    // ============================================================

    runtimeOnly("org.postgresql:postgresql")


    // ============================================================
    // TEST IMPLEMENTATION
    // ============================================================

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")

    testImplementation("org.testcontainers:testcontainers-junit-jupiter")
    testImplementation("org.testcontainers:testcontainers-postgresql")

    testCompileOnly("org.projectlombok:lombok")
    testAnnotationProcessor("org.projectlombok:lombok")
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}


// ============================================================
// SPOTLESS
// ============================================================

spotless {
    java {
        target("src/**/*.java")

        googleJavaFormat()

        removeUnusedImports()

        importOrder()

        trimTrailingWhitespace()

        endWithNewline()
    }

    format("misc") {
        target(
            "*.md",
            "*.yml",
            "*.yaml",
            ".gitignore",
            "*.gradle.kts"
        )

        trimTrailingWhitespace()

        endWithNewline()
    }
}


// ============================================================
// SONARQUBE
// ============================================================

sonar {
    properties {
        property("sonar.projectKey", "YoelSLA_football-market")
        property("sonar.organization", "yoelsla")
    }
}