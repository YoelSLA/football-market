import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle

plugins {
    java
    jacoco

    id("org.springframework.boot") version "4.1.1"
    id("io.spring.dependency-management") version "1.1.7"
    id("org.sonarqube") version "7.3.1.8318"
    id("com.diffplug.spotless") version "7.2.1"
}

group = "com.example"
version = "0.0.1-SNAPSHOT"

// Generación local únicamente: no ejecuta Flyway ni abre conexiones a la base.
tasks.register("createMigration") {
    val migrationDescription = providers.gradleProperty("migrationDescription")
    val migrationDirectory = layout.projectDirectory.dir("src/main/resources/db/migration")
    val migrationLock = layout.projectDirectory.file(".gradle/create-migration.lock")
    group = "database"
    description = "Crea una migración SQL con timestamp UTC y versión estrictamente creciente."
    doLast {
        val name = migrationDescription.orNull ?: throw GradleException(
            "Indicá -PmigrationDescription=descripcion_en_ingles."
        )
        if (!Regex("[a-z][a-z0-9]*(?:_[a-z0-9]+)*").matches(name) || name.length > 100) {
            throw GradleException(
                "Descripción inválida: usá snake_case ASCII en minúsculas, empezando con letra, de hasta 100 caracteres."
            )
        }

        val directory = migrationDirectory.asFile.toPath()
        val lockPath = migrationLock.asFile.toPath()
        Files.createDirectories(lockPath.parent)
        FileChannel.open(lockPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE).use { channel ->
            channel.lock().use {
                val pattern = Regex("V([0-9]+)__[A-Za-z0-9]+(?:_[A-Za-z0-9]+)*\\.sql")
                val formatter = DateTimeFormatter.ofPattern("uuuuMMddHHmmss")
                    .withResolverStyle(ResolverStyle.STRICT)
                val versions = mutableSetOf<java.math.BigInteger>()
                Files.walk(directory).use { paths ->
                    paths.filter { Files.isRegularFile(it) }.forEach { path ->
                        val filename = path.fileName.toString()
                        if (filename.startsWith("V")) {
                            val match = pattern.matchEntire(filename)
                                ?: throw GradleException("Nombre de migración inválido: $filename")
                            val rawVersion = match.groupValues[1]
                            if (rawVersion.length == 14) {
                                try {
                                    LocalDateTime.parse(rawVersion, formatter)
                                } catch (exception: java.time.DateTimeException) {
                                    throw GradleException("Timestamp inválido: $filename", exception)
                                }
                            }
                            if (!versions.add(rawVersion.toBigInteger())) {
                                throw GradleException("Versión de migración duplicada: $rawVersion")
                            }
                        }
                    }
                }
                val timestamp = LocalDateTime.now(ZoneOffset.UTC).format(formatter)
                val latest = versions.maxOrNull()
                if (latest != null && timestamp.toBigInteger() <= latest) {
                    throw GradleException(
                        "El timestamp UTC $timestamp no supera la versión local $latest. " +
                            "Esperá al próximo segundo o revisá el reloj y las migraciones integradas."
                    )
                }
                val target = directory.resolve("V${timestamp}__${name}.sql")
                Files.writeString(target, "-- $name\n", StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)
                logger.lifecycle("Migración creada: $target")
            }
        }
    }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

// ============================================================
// DEPENDENCIAS
// ============================================================

dependencies {

    implementation(libs.jjwt.api)
    implementation(libs.springdoc.openapi)
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    developmentOnly("me.paulschwarz:springboot4-dotenv:5.1.0")

    implementation("org.flywaydb:flyway-database-postgresql")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-web")

    compileOnly("org.projectlombok:lombok")

    implementation("org.postgresql:postgresql")
    runtimeOnly(libs.jjwt.impl)
    runtimeOnly(libs.jjwt.jackson)

    annotationProcessor("org.projectlombok:lombok")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
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

    // Genera el reporte de cobertura después de ejecutar los tests.
    finalizedBy(tasks.jacocoTestReport)
}

// ============================================================
// JACOCO
// ============================================================

tasks.jacocoTestReport {
    dependsOn(tasks.test)

    reports {
        // SonarQube utiliza el reporte XML para importar la cobertura.
        xml.required.set(true)

        // Reporte visual para consultar la cobertura localmente.
        html.required.set(true)

        csv.required.set(false)
    }
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

        // Reporte de cobertura generado por JaCoCo.
        property(
            "sonar.coverage.jacoco.xmlReportPaths",
            "build/reports/jacoco/test/jacocoTestReport.xml"
        )
    }
}

// Sonar debe ejecutarse después de que JaCoCo haya generado
// el reporte XML de cobertura.
tasks.named("sonar") {
    dependsOn(tasks.jacocoTestReport)
}
