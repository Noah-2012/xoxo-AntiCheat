plugins {
    java
}

group = "com.xoxoax"
version = "1.0.0"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
    maven {
        name = "papermc"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
}

dependencies {
    // Paper API — compileOnly because Paper provides it at runtime
    compileOnly("io.papermc.paper:paper-api:1.21.10-R0.1-SNAPSHOT")
}

tasks.jar {
    archiveFileName.set("xoxo-AntiCheat-${project.version}.jar")
}
