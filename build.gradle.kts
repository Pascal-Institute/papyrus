plugins {
    kotlin("jvm") version "2.1.0"
    id("org.jetbrains.compose") version "1.7.3"
    id("org.jetbrains.kotlin.plugin.compose") version "2.1.0"
    kotlin("plugin.serialization") version "2.1.0"
}

repositories {
    mavenCentral()
    google()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

dependencies {
    implementation(project(":ahmes"))
    implementation(compose.desktop.currentOs)
    implementation(compose.materialIconsExtended)
    implementation("io.ktor:ktor-client-core:2.3.13")
    implementation("io.ktor:ktor-client-cio:2.3.13")
    implementation("io.ktor:ktor-client-content-negotiation:2.3.13")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.13")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.9.0")

    // Unified content extraction (PDF/HTML/TXT)
    implementation("org.apache.tika:tika-core:1.28.5")
    implementation("org.apache.tika:tika-parsers:1.28.5")

    // HTML Parsing
    implementation("org.jsoup:jsoup:1.18.3")

    // JSON mapping (companyfacts, submissions)
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.17.1")

    // JavaMoney for precise financial calculations (AGENTS.md Principle 4)
    implementation("org.javamoney:moneta:1.4.2")
    implementation("javax.money:money-api:1.1")

    // Logging
    implementation("org.slf4j:slf4j-simple:2.0.16")

    // Testing
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

compose.desktop {
    application {
        mainClass = "papyrus.MainKt"
        nativeDistributions {
            targetFormats(org.jetbrains.compose.desktop.application.dsl.TargetFormat.Dmg, org.jetbrains.compose.desktop.application.dsl.TargetFormat.Msi, org.jetbrains.compose.desktop.application.dsl.TargetFormat.Deb)
            packageName = "papyrus"
            packageVersion = "1.0.0"
        }
    }
}
