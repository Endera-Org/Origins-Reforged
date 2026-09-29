plugins {
    id("java")
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    `maven-publish`
}

group = "ru.turbovadim"
version = rootProject.version

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven { url = uri("https://jitpack.io") }
    maven { url = uri("https://repo.codemc.io/repository/maven-snapshots/") }
    maven { url = uri("https://repo.codemc.io/repository/maven-releases/") }
}

dependencies {
    compileOnly(libs.paper.api)
    compileOnly(project(":version"))
    compileOnly(libs.packetevents)
    compileOnly(libs.enderalib)
    implementation(libs.kotlinx.serialization.json)
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

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "ru.turbovadim"
            artifactId = "origins-reforged-api"
            version = project.version.toString()

            from(components["java"])
        }
    }
}
