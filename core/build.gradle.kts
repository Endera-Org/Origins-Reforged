plugins {
    id("origins.kotlin")
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    api(project(":api"))
    compileOnly(libs.jetbrains.annotations)
    compileOnly(libs.viaversion.api)
    compileOnly(libs.paper.api)
    compileOnly(libs.vault.api)
    compileOnly(libs.geyser.api)
    compileOnly(libs.floodgate.api)
    compileOnly(libs.authme)
    compileOnly(libs.placeholderapi)

    implementation(libs.interfaces)
    compileOnly(libs.packetevents)

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
    // EnderaLib provides Kotlin, coroutines, serialization, Exposed, Hikari and H2 at runtime
    compileOnly(libs.enderalib)
}
