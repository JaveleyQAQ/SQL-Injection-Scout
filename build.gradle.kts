plugins {
    kotlin("jvm") version "2.0.20"
    id("com.github.johnrengelman.shadow") version "8.1.1"


}

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    archiveFileName.set("${project.findProperty("extensionName")} ${project.findProperty("projectVersion")}.jar")
}

repositories {

    mavenCentral()
    maven(url="https://jitpack.io") {
        content {
//            includeGroup("com.github.milchreis")
//            includeGroup("com.github.ncoblentz")
        }
    }

}

dependencies {
    testImplementation(kotlin("test"))
    // Check for latest version: https://central.sonatype.com/artifact/net.portswigger.burp.extensions/montoya-api/versions
    implementation("net.portswigger.burp.extensions:montoya-api:2024.11")
    implementation("com.github.ncoblentz:BurpMontoyaLibrary:0.1.26")
    implementation ("io.github.java-diff-utils:java-diff-utils:4.12")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
    implementation("com.google.code.gson:gson:2.13.2")
}

kotlin {
    jvmToolchain(21)
}

tasks.processResources {
    from("gradle.properties")
}

tasks.test {
    useJUnitPlatform()
}