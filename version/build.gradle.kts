plugins {
    id("java")
    alias(libs.plugins.kotlin.jvm)
}

repositories {
    mavenCentral()
    maven { url = uri("https://repo.papermc.io/repository/maven-public/") }
}

dependencies {
    implementation(libs.jetbrains.annotations)
    testImplementation(libs.junit.jupiter)
    compileOnly(libs.paper.api)
    implementation(kotlin("stdlib-jdk8"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}