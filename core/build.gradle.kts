import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("java")
    kotlin("jvm") version "2.2.0"
    kotlin("plugin.serialization") version "2.2.0" apply true
}

repositories {
    mavenCentral()
    maven { url = uri("https://repo.papermc.io/repository/maven-public/") } // Paper
    //maven { url = uri("https://oss.sonatype.org/content/repositories/snapshots") } // Spigot
    //maven { url = uri("https://hub.spigotmc.org/nexus/content/repositories/snapshots/") } // Spigot
    maven { url = uri("https://repo.extendedclip.com/content/repositories/placeholderapi/") }
    maven { url = uri("https://jitpack.io") }
    maven { url = uri("https://repo.opencollab.dev/main/") }
    maven { url = uri("https://repo.viaversion.com") }

    maven { url = uri("https://repo.codemc.io/repository/maven-snapshots/") }
    maven { url = uri("https://repo.codemc.io/repository/maven-releases/") }
}

dependencies {
    val exposedVersion = "0.61.0"

    implementation("org.jetbrains:annotations:23.0.0")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.1")
    compileOnly("com.viaversion:viaversion-api:5.0.0")
    compileOnly("io.papermc.paper:paper-api:1.20.1-R0.1-SNAPSHOT") // Paper
    compileOnly("com.github.MilkBowl:VaultAPI:1.7.1")
    compileOnly("com.github.aromaa:WorldGuardExtraFlags:v4.2.4")
    compileOnly("org.geysermc.geyser:api:2.2.0-SNAPSHOT")
    compileOnly("org.geysermc.floodgate:api:2.2.2-SNAPSHOT")
    compileOnly("com.github.authme:authmereloaded:5.6.0-beta2")
    compileOnly("me.clip:placeholderapi:2.11.5")
    compileOnly("com.github.stleary:JSON-java:20241224")
    compileOnly("net.objecthunter:exp4j:0.4.8")

    compileOnly("com.github.retrooper:packetevents-spigot:2.9.1-SNAPSHOT")

    compileOnly(project(":version"))
    compileOnly(project(":1.20"))
    compileOnly(project(":1.20.1"))
    compileOnly(project(":1.20.2"))
    compileOnly(project(":1.20.3"))
    compileOnly(project(":1.20.4"))
    compileOnly(project(":1.20.6"))
    compileOnly(project(":1.21"))
    compileOnly(project(":1.21.1"))
    compileOnly(project(":1.21.3"))
    compileOnly(project(":1.21.4"))
    compileOnly(project(":1.21.6"))
    compileOnly(project(":1.21.7"))
    compileOnly(files("libs/worldguard.jar"))
    compileOnly(files("libs/worldedit.jar"))
    implementation(kotlin("stdlib-jdk8"))
    implementation("com.github.Endera-Org:EnderaLib:1.4.5") {
        isTransitive = false
    }
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.1")

    compileOnly("org.jetbrains.exposed:exposed-core:$exposedVersion")
    compileOnly("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    compileOnly("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    compileOnly("com.zaxxer:HikariCP:6.2.1")

}

tasks {
    compileJava {
        options.release.set(17)
    }
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(17)
}
kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xjvm-default=all") // or "-Xjvm-default=all-compatibility"
    }
}
val compileKotlin: KotlinCompile by tasks