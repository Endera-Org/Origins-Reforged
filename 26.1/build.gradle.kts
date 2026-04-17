import io.papermc.paperweight.userdev.ReobfArtifactConfiguration

plugins {
    id("java")
    id("io.papermc.paperweight.userdev")
    kotlin("jvm") version "2.3.20"
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")

    maven(url = "https://s01.oss.sonatype.org/content/repositories/snapshots/") {
        name = "sonatype-oss-snapshots"
    }
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    implementation(project(":version"))
    paperweight.paperDevBundle("26.1.2.build.+")
    implementation(kotlin("stdlib-jdk8"))
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
    disableAutoTargetJvm()
}

tasks {
    compileJava {
        options.release.set(25)
    }
}

tasks.test {
    useJUnitPlatform()
}

tasks.reobfJar {
    enabled = false
}

configurations.runtimeElements.configure {
    outgoing.artifacts.clear()
    outgoing.artifact(tasks.jar)
    attributes {
        attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 21)
    }
}