import org.jetbrains.kotlin.gradle.dsl.JvmDefaultMode
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    `java-library`
    id("org.jetbrains.kotlin.jvm")
}

// Kotlin stdlib comes from EnderaLib at runtime, so compile against that version's API only.
val kotlinRuntime = versionCatalogs.named("libs").findVersion("kotlin-runtime").get().requiredVersion

kotlin {
    jvmToolchain(21)
    coreLibrariesVersion = kotlinRuntime
    compilerOptions {
        apiVersion.set(KotlinVersion.fromVersion(kotlinRuntime.substringBeforeLast('.')))
        jvmDefault.set(JvmDefaultMode.NO_COMPATIBILITY)
    }
}

tasks.processResources {
    val version = project.version.toString()
    inputs.property("version", version)
    filesMatching("plugin.yml") {
        expand("version" to version)
    }
}
