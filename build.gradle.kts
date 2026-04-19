import org.gradle.api.attributes.java.TargetJvmVersion

plugins {
    kotlin("jvm") version "1.9.22"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "mc.arch.skin.bridge"
version = "0.1 BETA"

base {
    archivesName.set("EaglerXskinbridge")
}

configurations.all {
    attributes {
        attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 25)
    }
}

repositories {
    mavenLocal()
    mavenCentral() {
        metadataSources {
            mavenPom()
            artifact()
        }
    }
    maven("https://repo.papermc.io/repository/maven-public/") {
        metadataSources {
            mavenPom()
            artifact()
        }
    }
    maven("https://repo.lax1dude.net/repository/releases/") {
        metadataSources {
            mavenPom()
            artifact()
        }
    }
    maven("https://repo.codemc.io/repository/maven-public/") {
        metadataSources {
            mavenPom()
            artifact()
        }
    }
}

dependencies {
    implementation(kotlin("stdlib"))
    compileOnly("com.velocitypowered:velocity-api:3.3.0-SNAPSHOT")
    compileOnly(files("lib/EaglerXServer.jar"))
    compileOnly("net.skinsrestorer:skinsrestorer-api:15.12.0")
    implementation("com.moandjiezana.toml:toml4j:0.7.2")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.google.code.gson:gson:2.10.1")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
}

tasks.shadowJar {
    archiveClassifier.set("")
    mergeServiceFiles()
}
