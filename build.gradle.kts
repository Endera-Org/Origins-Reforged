plugins {
    java
    alias(libs.plugins.shadow)
}

dependencies {
    implementation(libs.json)
    implementation(libs.exp4j)
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
    implementation(libs.adventure.platform.bukkit)
}

tasks.shadowJar {
    archiveFileName.set("${rootProject.name}-${rootProject.version}.jar")
    dependencies {
        exclude(dependency("com.github.Endera-Org:EnderaLib"))
        exclude {
            it.moduleGroup == "org.jetbrains.kotlin" || it.moduleGroup == "org.jetbrains.kotlinx"
        }
    }
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}
