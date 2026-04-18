import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("java-library")
    kotlin("jvm") version "2.3.20"
    kotlin("plugin.serialization") version "2.2.21" apply true
}

repositories {
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
    val exposedVersion = "1.2.0"

    api(project(":api"))
    implementation("org.jetbrains:annotations:23.0.0")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.1")
    compileOnly("com.viaversion:viaversion-api:5.0.0")
    compileOnly("io.papermc.paper:paper-api:1.21.1-R0.1-SNAPSHOT") // Paper
    compileOnly("com.github.MilkBowl:VaultAPI:1.7.1")
    compileOnly("com.github.aromaa:WorldGuardExtraFlags:v4.2.4")
    compileOnly("org.geysermc.geyser:api:2.9.0-SNAPSHOT")
    compileOnly("org.geysermc.floodgate:api:2.2.2-SNAPSHOT")
    compileOnly("com.github.authme:authmereloaded:5.6.0-beta2")
    compileOnly("me.clip:placeholderapi:2.11.5")
    implementation("org.json:json:20250517")
    compileOnly("net.objecthunter:exp4j:0.4.8")

    implementation("com.noxcrew.interfaces:interfaces:2.1.0-SNAPSHOT")
    compileOnly("com.github.retrooper:packetevents-spigot:2.12.0-SNAPSHOT")

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
    implementation("com.github.Endera-Org:EnderaLib:1.5.0") {
        isTransitive = false
    }
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")

    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    implementation("com.zaxxer:HikariCP:6.2.1")
    implementation("com.h2database:h2:2.3.232")

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
