plugins {
    kotlin("jvm") version "2.3.0"
    id("org.jetbrains.compose") version "1.7.3"
    id("org.jetbrains.kotlin.plugin.compose") version "2.3.0"
    kotlin("plugin.serialization") version "2.3.0"
    id("io.gitlab.arturbosch.detekt") version "1.23.4"
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

// Kotlin 컴파일러 옵션: 타입 안정성 및 조기 에러 감지
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        // 경고를 무시하지 않고 명확히 처리하도록 강제
        // allWarningsAsErrors.set(true)  // 준비되면 활성화

        // Null safety 강화
        freeCompilerArgs.add("-Xjsr305=strict")

        // Progressive mode: 미래 Kotlin 버전의 breaking changes 미리 적용
        // progressiveMode.set(true)
    }
}

// Detekt 설정: 코드 품질 검사
detekt {
    buildUponDefaultConfig = true
    allRules = false

    // 설정 파일이 있으면 사용
    val configFile = file("$projectDir/config/detekt/detekt.yml")
    if (configFile.exists()) {
        config.setFrom(configFile)
    }

    // Baseline 파일은 optional
    val baselineFile = file("$projectDir/config/detekt/baseline.xml")
    if (baselineFile.exists()) {
        baseline = baselineFile
    }

    // Detekt 실패 시 빌드를 중단하지 않음 (경고로만 처리)
    ignoreFailures = true
}

tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
    reports {
        html.required.set(true)  // HTML 리포트 (브라우저에서 확인)
        xml.required.set(true)   // XML 리포트 (파싱 가능)
        txt.required.set(true)   // 텍스트 리포트 (콘솔 출력)
        sarif.required.set(true) // SARIF 리포트 (GitHub 통합)
    }

    // JVM 메모리 증가 (큰 프로젝트 분석용)
    jvmTarget = "17"
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
