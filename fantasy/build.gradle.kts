plugins {
    id("java-library")
    alias(libs.plugins.kotlin.jvm)
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    compileOnly(project(":api"))
    compileOnly(project(":core"))
    compileOnly(project(":version"))
    compileOnly(libs.paper.api)
    compileOnly(libs.enderalib) { isTransitive = false }
    implementation(kotlin("stdlib-jdk8"))
}

tasks {
    compileJava {
        options.release.set(21)
    }
    jar {
        archiveBaseName.set("OriginsReforged-Fantasy")
    }
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        freeCompilerArgs.add("-Xjvm-default=all")
    }
}
