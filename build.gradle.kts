plugins {
    kotlin("jvm") version "1.6.0"
    `maven-publish`
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

allprojects {
    repositories {
        mavenCentral()
        maven(url = "https://papermc.io/repo/repository/maven-public/")
    }
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")

    repositories {
        maven("https://papermc.io/repo/repository/maven-public/")
    }

    dependencies {
        compileOnly("io.papermc.paper:paper-api:1.18.1-R0.1-SNAPSHOT")
        compileOnly(kotlin("stdlib"))

//    implementation("io.github.monun:tap-api:4.3.1")
        implementation("io.github.monun:kommand-api:2.8.0")
        implementation("it.unimi.dsi:fastutil:8.5.4")
    }
}