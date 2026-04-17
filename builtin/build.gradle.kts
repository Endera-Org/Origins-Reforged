plugins {
    id("java-library")
    kotlin("jvm") version "2.3.20"
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven { url = uri("https://jitpack.io") }
    maven { url = uri("https://repo.codemc.io/repository/maven-snapshots/") }
    maven { url = uri("https://repo.codemc.io/repository/maven-releases/") }
}

dependencies {
    api(project(":api"))
    compileOnly(project(":core"))
    compileOnly(project(":version"))
    compileOnly("io.papermc.paper:paper-api:1.21.1-R0.1-SNAPSHOT")
    compileOnly("com.github.retrooper:packetevents-spigot:2.12.0-SNAPSHOT")
    implementation(kotlin("stdlib-jdk8"))
}

tasks {
    compileJava {
        options.release.set(21)
    }
}

kotlin {
    jvmToolchain(21)
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xjvm-default=all")
    }
}
