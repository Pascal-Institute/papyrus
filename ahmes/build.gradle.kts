plugins {
    kotlin("jvm") version "2.1.0"
    kotlin("plugin.serialization") version "2.1.0"
    `java-library`
    `maven-publish`
}

group = "com.pascal.institute"
version = "1.0.0"

repositories {
    mavenCentral()
    google()
}

dependencies {
    // Kotlin
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")

    // HTML Parsing
    implementation("org.jsoup:jsoup:1.18.3")

    // XPath/XSLT for XBRL extraction
    implementation("net.sf.saxon:Saxon-HE:12.4")

    // JSON mapping (companyfacts, submissions)
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.17.1")

    // JavaMoney for precise financial calculations (AGENTS.md Principle 4)
    implementation("org.javamoney:moneta:1.4.2")
    implementation("javax.money:money-api:1.1")

    // PDF Support
    implementation("org.apache.pdfbox:pdfbox:2.0.32")

    // Unified content extraction (PDF/HTML/TXT)
    implementation("org.apache.tika:tika-core:1.28.5")
    implementation("org.apache.tika:tika-parsers:1.28.5")

    // DJL (Deep Java Library) for local AI inference
    implementation("ai.djl:api:0.25.0")
    implementation("ai.djl.pytorch:pytorch-engine:0.25.0")
    implementation("ai.djl.pytorch:pytorch-model-zoo:0.25.0")
    implementation("ai.djl.huggingface:tokenizers:0.25.0")

    // Logging
    implementation("org.slf4j:slf4j-api:2.0.16")

    // Testing
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

java {
    withSourcesJar()
    withJavadocJar()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "com.pascal.institute"
            artifactId = "ahmes"
            version = project.version.toString()

            from(components["java"])

            pom {
                name.set("Ahmes SEC Parser Library")
                description.set("SEC Filing parser library for extracting financial data from SEC reports (10-K, 10-Q, 8-K, S-1, DEF 14A, 20-F)")
                url.set("https://github.com/Pascal-Institute/papyrus")

                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }

                developers {
                    developer {
                        id.set("pascal-institute")
                        name.set("Pascal Institute")
                    }
                }
            }
        }
    }

    repositories {
        maven {
            name = "local"
            url = uri("${rootProject.projectDir}/build/repo")
        }
    }
}

kotlin {
    jvmToolchain(17)
}
