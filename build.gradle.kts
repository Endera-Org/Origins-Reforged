plugins {
    java
    alias(libs.plugins.shadow)
}

dependencies {
    implementation(project(":api"))
    implementation(project(":core"))
    implementation(project(":builtin"))
    implementation(project(":version"))
    implementation(project(":1.21.1"))
    implementation(project(":1.21.3"))
    implementation(project(":1.21.4"))
    implementation(project(":1.21.6"))
    implementation(project(":1.21.7"))
    implementation(project(":1.21.10"))
    implementation(project(":1.21.11"))
    implementation(project(":26.1"))
    implementation(project(":26.2"))
    implementation(project(":26.3"))
}

tasks.shadowJar {
    manifest.attributes["paperweight-mappings-namespace"] = "mojang"
    archiveFileName.set("${rootProject.name}-${rootProject.version}.jar")
    dependencies {
        // Provided at runtime by EnderaLib (Kotlin) and Paper (slf4j); the rest are compile-time annotations
        exclude {
            it.moduleGroup in setOf(
                "org.jetbrains.kotlin",
                "org.jetbrains.kotlinx",
                "org.slf4j",
                "org.jetbrains",
                "org.checkerframework",
                "com.google.errorprone",
            )
        }
    }
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}
