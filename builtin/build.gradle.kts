plugins {
    id("origins.kotlin")
}

dependencies {
    api(project(":api"))
    compileOnly(project(":core"))
    compileOnly(project(":version"))
    compileOnly(libs.paper.api)
    compileOnly(libs.packetevents)
    compileOnly(libs.enderalib) { isTransitive = false }
}
