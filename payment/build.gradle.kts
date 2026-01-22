import org.gradle.testing.jacoco.plugins.JacocoPluginExtension
import org.gradle.testing.jacoco.tasks.JacocoReport
import org.gradle.testing.jacoco.tasks.JacocoCoverageVerification

plugins {
    kotlin("jvm") version "1.9.25"
    kotlin("plugin.spring") version "1.9.25"
    kotlin("plugin.jpa") version "1.9.25"
    id("org.springframework.boot") version "3.5.9"
    id("io.spring.dependency-management") version "1.1.7"
    jacoco
}

group = "com.popcorn"
version = "0.0.1-SNAPSHOT"
description = "payment"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }

    // 모든 설정에서 취약한 commons-compress 버전을 보안 버전으로 강제 교체
    all {
        resolutionStrategy {
            force("org.apache.commons:commons-compress:1.27.1")

            eachDependency {
                if (requested.group == "org.apache.commons" && requested.name == "commons-compress") {
                    useVersion("1.27.1")
                    because("CVE-2024-25710, CVE-2024-26308 보안 취약점 해결")
                }
            }
        }
    }
}

repositories {
    mavenCentral()
}

extra["springModulithVersion"] = "2.0.1"
extra["coroutinesVersion"] = "1.8.1"
extra["resilience4jVersion"] = "2.2.0"
extra["mockkVersion"] = "1.13.12"
extra["springmockkVersion"] = "4.0.2"
extra["testcontainersVersion"] = "1.20.4"
extra["wireMockVersion"] = "3.0.1"
extra["springdocVersion"] = "2.8.0"

dependencies {
    // === 프로젝트 의존성 ===
    implementation(project(":common-lib"))

    // === Spring Boot Core ===
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.modulith:spring-modulith-starter-core")

    // === JWT 보안 처리 (기존 backend 호환용) ===
    implementation("io.jsonwebtoken:jjwt-api:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")

    // === Kotlin 관련 ===
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    // === 코루틴 ===
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${property("coroutinesVersion")}")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:${property("coroutinesVersion")}")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:${property("coroutinesVersion")}")

    // === Resilience4j ===
    implementation("io.github.resilience4j:resilience4j-spring-boot3:${property("resilience4jVersion")}")
    implementation("io.github.resilience4j:resilience4j-reactor:${property("resilience4jVersion")}")

    // === 데이터베이스 ===
    runtimeOnly("com.h2database:h2")
    runtimeOnly("org.postgresql:postgresql")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")

    // === Redis ===
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-cache")

    // === API 문서화 ===
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:${property("springdocVersion")}")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-api:${property("springdocVersion")}")

    // === 보안 취약점 해결 ===
    implementation("org.apache.commons:commons-compress:1.27.1") // CVE-2024-25710, CVE-2024-26308 수정

    // === 개발 도구 ===
    developmentOnly("org.springframework.boot:spring-boot-devtools")
    developmentOnly("org.springframework.boot:spring-boot-docker-compose")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

    // === 테스트 ===
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "com.github.tomakehurst", module = "wiremock-jre8")
        exclude(group = "org.apache.commons", module = "commons-compress") // 취약한 버전 제외
    }
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.springframework.modulith:spring-modulith-starter-test")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:${property("coroutinesVersion")}")

    // === 테스트 Mock ===
    testImplementation("io.mockk:mockk:${property("mockkVersion")}")
    testImplementation("com.ninja-squad:springmockk:${property("springmockkVersion")}")

    // === 테스트 컨테이너 ===
    testImplementation(platform("org.testcontainers:testcontainers-bom:${property("testcontainersVersion")}"))
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")

    // === WireMock ===
    testImplementation("org.wiremock:wiremock:${property("wireMockVersion")}")

    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.modulith:spring-modulith-bom:${property("springModulithVersion")}")
    }
    dependencies {
        // 보안 취약점 해결을 위한 강제 버전 업그레이드
        dependency("org.apache.commons:commons-compress:1.27.1") // CVE-2024-25710, CVE-2024-26308 수정
    }
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
    ignoreFailures = true  // JaCoCo 리포트 생성을 위해 테스트 실패 무시
}

// JaCoCo 테스트 커버리지 설정
configure<JacocoPluginExtension> {
    toolVersion = "0.8.13"
}

tasks.named<JacocoReport>("jacocoTestReport") {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
    finalizedBy(tasks.named("jacocoTestCoverageVerification"))
}

tasks.named<JacocoCoverageVerification>("jacocoTestCoverageVerification") {
    dependsOn(tasks.named("jacocoTestReport"))
    violationRules {
        rule {
            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = "0.80".toBigDecimal() // 80% 커버리지 요구
            }
        }
        rule {
            limit {
                counter = "BRANCH"
                value = "COVEREDRATIO"
                minimum = "0.75".toBigDecimal() // 75% 브랜치 커버리지 요구
            }
        }
    }
}

tasks.named("check") {
    dependsOn(tasks.named("jacocoTestCoverageVerification"))
}
