import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("java-library")
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

repositories {
    mavenLocal()
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    //maven { url = uri("https://oss.sonatype.org/content/repositories/snapshots") } // Spigot
    //maven { url = uri("https://hub.spigotmc.org/nexus/content/repositories/snapshots/") } // Spigot
    maven { url = uri("https://repo.extendedclip.com/content/repositories/placeholderapi/") }
    maven { url = uri("https://jitpack.io") }
    maven { url = uri("https://repo.opencollab.dev/main/") }
    maven { url = uri("https://repo.viaversion.com") }

    maven { url = uri("https://repo.codemc.io/repository/maven-snapshots/") }
    maven { url = uri("https://repo.codemc.io/repository/maven-releases/") }

    maven("https://maven.noxcrew.com/public")
}

dependencies {
    api(project(":api"))
    implementation(libs.jetbrains.annotations)
    testImplementation(libs.junit.jupiter)
    compileOnly(libs.viaversion.api)
    compileOnly(libs.paper.api) // Paper
    compileOnly(libs.vault.api)
    compileOnly(libs.geyser.api)
    compileOnly(libs.floodgate.api)
    compileOnly(libs.authme)
    compileOnly(libs.placeholderapi)
    implementation(libs.json)
    compileOnly(libs.exp4j)

    implementation(libs.interfaces)
    compileOnly(libs.packetevents)

    compileOnly(project(":version"))
    compileOnly(project(":1.21.1"))
    compileOnly(project(":1.21.3"))
    compileOnly(project(":1.21.4"))
    compileOnly(project(":1.21.6"))
    compileOnly(project(":1.21.7"))
    compileOnly(project(":1.21.10"))
    compileOnly(project(":1.21.11"))
    compileOnly(files("libs/worldguard.jar"))
    compileOnly(files("libs/worldedit.jar"))
    implementation(kotlin("stdlib-jdk8"))
    implementation(libs.enderalib) {
        isTransitive = false
    }
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)

    implementation(libs.bundles.exposed)
    implementation(libs.hikaricp)
    implementation(libs.h2)

}

tasks {
    compileJava {
        options.release.set(21)
    }
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}
kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xjvm-default=all") // or "-Xjvm-default=all-compatibility"
    }
}
val compileKotlin: KotlinCompile by tasks
