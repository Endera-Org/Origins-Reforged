plugins {
    id("java-library")
    alias(libs.plugins.kotlin.jvm)
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven { url = uri("https://jitpack.io") }
    maven { url = uri("https://repo.codemc.io/repository/maven-snapshots/") }
    maven { url = uri("https://repo.codemc.io/repository/maven-releases/") }
}

dependencies {
    compileOnly(project(":api"))
    compileOnly(project(":core"))
    compileOnly(project(":version"))
    compileOnly(libs.paper.api)
    compileOnly(libs.packetevents)
    compileOnly(libs.enderalib) { isTransitive = false }
    implementation(kotlin("stdlib-jdk8"))
}

tasks {
    compileJava {
        options.release.set(21)
    }
    jar {
        archiveBaseName.set("OriginsReforged-Monsters")
    }
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        freeCompilerArgs.add("-Xjvm-default=all")
    }
}
