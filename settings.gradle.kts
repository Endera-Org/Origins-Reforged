pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://repo.papermc.io/repository/maven-public/")
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}


rootProject.name = "Origins-Reforged"
include("api")
include("core")
include("builtin")
include("monsters")
include("mobs")
include("fantasy")
include("version")
include("1.21.1")
include("1.21.3")
include("1.21.4")
include("1.21.6")
include("1.21.7")
include("1.21.10")
include("1.21.11")
