plugins {
    java
    id("org.springframework.boot") version "4.1.1"
    id("io.spring.dependency-management") version "1.1.7"
    id("org.sonarqube") version "7.3.1.8318"
    id("com.diffplug.spotless") version "7.2.1"
    id("org.asciidoctor.jvm.convert") version "4.0.5"
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

// ============================================================
// SPRING REST DOCS + ASCIIDOCTOR
// ============================================================

val snippetsDir = file("build/generated-snippets")

dependencies {

    implementation(libs.jjwt.api)
    implementation(libs.springdoc.openapi)
    developmentOnly("me.paulschwarz:springboot4-dotenv:5.1.0")

    implementation("org.flywaydb:flyway-database-postgresql")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-web")

    compileOnly("org.projectlombok:lombok")

    runtimeOnly("org.postgresql:postgresql")
    runtimeOnly(libs.jjwt.impl)
    runtimeOnly(libs.jjwt.jackson)

    annotationProcessor("org.projectlombok:lombok")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.springframework.restdocs:spring-restdocs-mockmvc")
    testImplementation("org.testcontainers:testcontainers-junit-jupiter")
    testImplementation("org.testcontainers:testcontainers-postgresql")
}

// ============================================================
// TEST
// ============================================================

tasks.named<Test>("test") {
    useJUnitPlatform()

    // Fuerza la JVM de los tests a utilizar UTC.
    // Esto evita que pgjdbc tome la zona horaria local
    // de Windows (America/Buenos_Aires) como TimeZone.
    systemProperty("user.timezone", "UTC")

    // Los tests de Spring REST Docs generan los snippets acá.
    outputs.dir(snippetsDir)
}

// ============================================================
// ASCIIDOCTOR
// ============================================================

tasks.named<org.asciidoctor.gradle.jvm.AsciidoctorTask>("asciidoctor") {
    // Primero ejecuta los tests para generar los snippets.
    dependsOn(tasks.test)

    // Los snippets son una entrada de esta tarea.
    inputs.dir(snippetsDir)

    // Permite usar {snippets} dentro de index.adoc.
    attributes(
        mapOf(
            "snippets" to snippetsDir
        )
    )
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
