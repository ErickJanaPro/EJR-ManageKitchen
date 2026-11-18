plugins {
    id("java")
    id("org.springframework.boot") version "3.2.4"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "com.ejr.managekitchen"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // Permet de créer des API REST et de servir des fichiers statiques (HTML/CSS/JS)
    implementation("org.springframework.boot:spring-boot-starter-web")

    // Outils de test intégrés à Spring Boot
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.withType<Test> {
    useJUnitPlatform()
}