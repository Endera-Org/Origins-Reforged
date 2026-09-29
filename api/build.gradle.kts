plugins {
    id("origins.kotlin")
    alias(libs.plugins.kotlin.serialization)
    `maven-publish`
}

dependencies {
    compileOnly(libs.paper.api)
    compileOnly(project(":version"))
    compileOnly(libs.packetevents)
    compileOnly(libs.enderalib)
    implementation(libs.kotlinx.serialization.json)
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "ru.turbovadim"
            artifactId = "origins-reforged-api"
            version = project.version.toString()

            from(components["java"])
        }
    }
}
