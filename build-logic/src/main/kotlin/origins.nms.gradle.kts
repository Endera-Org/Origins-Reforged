// Version-specific NMS module. Each module only declares its paperDevBundle.
plugins {
    id("origins.kotlin")
    id("io.papermc.paperweight.userdev")
}

dependencies {
    implementation(project(":version"))
}

java {
    disableAutoTargetJvm()
}

// 26.x servers are unobfuscated and run on Java 25. The plain jar is published
// labelled as Java 21 so the Java 21 root build can shade it; it is only loaded on 26.x.
if (name.substringBefore('.').toInt() >= 26) {
    kotlin {
        jvmToolchain(25)
    }

    tasks.reobfJar {
        enabled = false
    }

    configurations.runtimeElements {
        outgoing.artifacts.clear()
        outgoing.artifact(tasks.jar)
        attributes {
            attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 21)
        }
    }
}
