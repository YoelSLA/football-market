import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle
import org.gradle.testing.jacoco.plugins.JacocoTaskExtension

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
    implementation(libs.playwright)
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

// Validación estática: no carga clases, no descubre tests ni arranca infraestructura.
val validateTestClassification = tasks.register("validateTestClassification") {
    group = "verification"
    description = "Comprueba una única categoría explícita por clase contenedora de tests."
    inputs.files(sourceSets.test.get().allJava).withPathSensitivity(PathSensitivity.RELATIVE)

    doLast {
        val categories = setOf("unit", "lightweight", "integration", "e2e")
        val testAnnotation = Regex("@(?:org\\.junit\\.(?:jupiter\\.(?:api|params)|platform\\.suite\\.api)\\.)?(?:Test|ParameterizedTest|RepeatedTest|TestFactory|TestTemplate|Suite)\\b")
        val tagAnnotation = Regex("@(?:org\\.junit\\.jupiter\\.api\\.)?Tag\\s*\\(\\s*\"([^\"]+)\"\\s*\\)")
        val tagDeclaration = Regex("@(?:org\\.junit\\.jupiter\\.api\\.)?Tags?\\b")
        // Preserva literales y elimina solo comentarios; evita interpretar su documentación.
        val tokens = Regex("\"\"\"[\\s\\S]*?\"\"\"|\"(?:\\\\.|[^\"\\\\])*\"|'(?:\\\\.|[^'\\\\])*'|//[^\\r\\n]*|/\\*[\\s\\S]*?\\*/")
        val classDeclaration = Regex("\\bclass\\s+\\w+")
        val errors = mutableListOf<String>()
        var classified = 0

        inputs.files.files.sortedBy { it.path }.forEach { source ->
            val text = tokens.replace(source.readText()) { token ->
                if (token.value.startsWith("//") || token.value.startsWith("/*")) {
                    " ".repeat(token.value.length)
                } else token.value
            }
            val code = tokens.replace(text) { " ".repeat(it.value.length) }
            if (source.name.endsWith("Test.java") || source.name.endsWith("Tests.java") ||
                testAnnotation.containsMatchIn(code)) {
                // Una única clase de nivel superior: no atribuir su tag a otra clase del archivo.
                val declarations = classDeclaration.findAll(code).filter { match ->
                    var depth = 0
                    code.take(match.range.first).forEach { character ->
                        if (character == '{') depth++
                        if (character == '}') depth--
                    }
                    depth == 0
                }.toList()
                val declaration = declarations.singleOrNull()
                val tags = tagAnnotation.findAll(text).filter { code[it.range.first] == '@' }.toList()
                val category = tags.singleOrNull()?.groupValues?.get(1)
                if (declaration == null || category !in categories ||
                    tagDeclaration.findAll(code).count() != 1 ||
                    tags.singleOrNull()?.range?.first?.let { it < declaration.range.first } != true) {
                    errors.add("${source.path}: declará una sola @Tag literal (unit, lightweight, integration o e2e) en la clase contenedora, sin tags en métodos ni @Nested.")
                } else {
                    classified++
                    if (category in setOf("unit", "lightweight") &&
                        Regex("\\b(?:SpringBootTest|Testcontainers|PostgreSQLContainer|TestcontainersConfiguration)\\b").containsMatchIn(code)) {
                        errors.add("${source.path}: una suite rápida no puede declarar infraestructura de contexto completo o PostgreSQL.")
                    }
                }
            } else if (tagDeclaration.containsMatchIn(code)) {
                errors.add("${source.path}: no declarar tags en helpers ni clases base; clasificar cada test concreto.")
            }
        }
        if (errors.isNotEmpty()) throw GradleException(errors.joinToString("\n"))
        logger.lifecycle("Clasificación válida: $classified clases de test.")
    }
}

val ciTests = providers.environmentVariable("CI").map { it.equals("true", ignoreCase = true) }.orElse(false)
tasks.withType<Test>().configureEach {
    val runningInCi = ciTests.get()
    useJUnitPlatform()
    systemProperty("user.timezone", "UTC")
    // En CI se ejecutan tests frescos, sin invalidar compilación ni dependencias.
    outputs.upToDateWhen { !runningInCi }
    outputs.doNotCacheIf("Los tests deben ejecutarse efectivamente en CI") { runningInCi }
}

tasks.named<Test>("test") {
    // Sin filtros: incluye también tests nuevos todavía sin clasificar.
    finalizedBy(tasks.jacocoTestReport)
}

val testSuites = mapOf(
    "unitTest" to "unit",
    "lightweightTest" to "lightweight",
    "integrationTest" to "integration",
    "e2eTest" to "e2e"
)
testSuites.forEach { (taskName, category) ->
    tasks.register<Test>(taskName) {
        group = "verification"
        description = "Ejecuta exclusivamente los tests de categoría $category, sin cobertura."
        dependsOn(validateTestClassification)
        testClassesDirs = sourceSets.test.get().output.classesDirs
        classpath = sourceSets.test.get().runtimeClasspath
        useJUnitPlatform {
            includeTags(category)
            // Una clasificación contradictoria falla en la validación; nunca duplica suites.
            excludeTags(*(setOf("unit", "lightweight", "integration", "e2e") - category).toTypedArray())
        }
        extensions.configure<JacocoTaskExtension> { isEnabled = false }
    }
}

tasks.register("fastTest") {
    group = "verification"
    description = "Ejecuta unitTest y lightweightTest una vez, sin PostgreSQL ni cobertura."
    dependsOn("unitTest", "lightweightTest")
}

tasks.named("check") { dependsOn(validateTestClassification) }

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
// CLIENTE OPERATIVO DE ESTADÍSTICAS
// ============================================================

// Invocación manual del cliente de scraping: sin arranque de Spring y sin opciones sensibles.
// El JWT se obtiene de FOOTBALL_MARKET_TOKEN; clientArgs solo admite opciones no secretas.
tasks.register<JavaExec>("statisticsSyncClient") {
    group = "application"
    description = "Ejecuta el cliente operativo de scraping de estadísticas WhoScored"
    mainClass.set("footballmarket.tools.PlayerStatisticsSyncClient")
    classpath = sourceSets["main"].runtimeClasspath
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
    dependsOn(validateTestClassification)
    dependsOn(tasks.jacocoTestReport)
}
