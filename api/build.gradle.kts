plugins {
    id("java")
    kotlin("jvm") version "2.2.21"
    kotlin("plugin.serialization") version "2.2.21" apply true
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
    compileOnly("io.papermc.paper:paper-api:1.20.1-R0.1-SNAPSHOT")
    compileOnly(project(":version"))
    compileOnly("com.github.retrooper:packetevents-spigot:2.10.0-SNAPSHOT")
    compileOnly("com.github.Endera-Org:EnderaLib:1.4.7")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
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
