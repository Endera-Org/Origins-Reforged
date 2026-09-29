// Standalone origin pack jar (OriginsReforged-<Name>.jar) loaded by the core plugin at runtime.
plugins {
    id("origins.kotlin")
}

val libs = versionCatalogs.named("libs")

dependencies {
    compileOnly(project(":api"))
    compileOnly(project(":core"))
    compileOnly(project(":version"))
    compileOnly(libs.findLibrary("paper-api").get())
    compileOnly(libs.findLibrary("packetevents").get())
    compileOnly(libs.findLibrary("enderalib").get()) { isTransitive = false }
}

tasks.jar {
    archiveBaseName.set("OriginsReforged-${project.name.replaceFirstChar(Char::uppercase)}")
}
