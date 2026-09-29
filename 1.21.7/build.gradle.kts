plugins {
    id("java")
    alias(libs.plugins.paperweight.userdev)
    alias(libs.plugins.kotlin.jvm)
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")

    maven(url = "https://s01.oss.sonatype.org/content/repositories/snapshots/") {
        name = "sonatype-oss-snapshots"
    }
}

dependencies {
    testImplementation(libs.junit.jupiter)
    implementation(project(":version"))
    paperweight.paperDevBundle("1.21.7-R0.1-SNAPSHOT")
    implementation(kotlin("stdlib-jdk8"))
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
    disableAutoTargetJvm()
}

tasks {
    compileJava {
        options.release.set(21)
    }
}

tasks.test {
    useJUnitPlatform()
}
