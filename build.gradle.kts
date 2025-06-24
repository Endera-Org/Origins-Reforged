plugins {
    id("java")
    id("com.gradleup.shadow") version "8.3.6"
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.17" apply false
    kotlin("jvm") version "2.1.20"
}

group = "ru.turbovadim"
version = "3.0.0-alpha10"

repositories {
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    implementation("com.github.stleary:JSON-java:20241224")
    implementation("net.objecthunter:exp4j:0.4.8")
    implementation(project(":core"))
    implementation(project(":version"))
    implementation(project(":1.20", "reobf"))
    implementation(project(":1.20.1", "reobf"))
    implementation(project(":1.20.2", "reobf"))
    implementation(project(":1.20.3", "reobf"))
    implementation(project(":1.20.4", "reobf"))
    implementation(project(":1.20.6", "reobf"))
    implementation(project(":1.21", "reobf"))
    implementation(project(":1.21.1", "reobf"))
    implementation(project(":1.21.3", "reobf"))
    implementation(project(":1.21.4", "reobf"))
    implementation(project(":1.21.5", "reobf"))
    implementation(project(":1.21.6", "reobf"))
    implementation("net.kyori:adventure-platform-bukkit:4.3.4")
}

tasks {
    compileJava {
        options.release.set(17)
    }
}

allprojects {
    tasks.withType<ProcessResources> {
        inputs.property("version", rootProject.version)
        filesMatching("**plugin.yml") {
            expand("version" to rootProject.version)
        }
    }
}


tasks {

    shadowJar {
        archiveFileName.set("${rootProject.name}-${rootProject.version}.jar")
        from(sourceSets.main.get().output)
        dependencies {
            exclude(dependency("com.github.Turbovadim:EnderaLib"))
            exclude {
                it.moduleGroup == "org.jetbrains.kotlin" || it.moduleGroup == "org.jetbrains.kotlinx"
            }
        }
    }

    test {
        useJUnitPlatform()
    }

}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}