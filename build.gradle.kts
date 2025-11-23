plugins {
    `java`
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "mello"
version = "1.0-SNAPSHOT"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://maven.elmakers.com/repository/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.3-R0.1-SNAPSHOT")
    compileOnly("org.dynmap:dynmap-api:1.9")
    implementation("com.google.code.gson:gson:2.11.0")
    implementation("org.mindrot:jbcrypt:0.4")
    implementation("com.auth0:java-jwt:4.4.0")
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks {
    // processResources no nível tasks (não dentro de tasks.test)
    processResources {
        filesMatching("plugin.yml") {
            expand("version" to project.version)
        }
    }

    jar {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }

    // shadowJar é fornecido pelo plugin shadow; configurar aqui
    named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
        archiveFileName.set("${project.name}.jar")
    }

    // configura o test para usar junit platform
    test {
        useJUnitPlatform()
    }
}
