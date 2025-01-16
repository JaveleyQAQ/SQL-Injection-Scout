plugins {
    // Provides Kotlin Language Support
    // https://plugins.gradle.org/plugin/org.jetbrains.kotlin.jvm
    kotlin("jvm") version "2.0.20"

    // Provides the shadowJar task in Gradle
    // https://plugins.gradle.org/plugin/com.github.johnrengelman.shadow
    id("com.github.johnrengelman.shadow") version "8.1.1"
}


group = "com.yournamehere.montoya"

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    archiveFileName.set("${project.findProperty("extensionName")}-${project.findProperty("projectVersion")}-all.jar")
}

repositories {
    //add maven local in case you want to build some reusable libraries and host them within your home directory
//    mavenLocal()
    mavenCentral()

    mavenCentral()
    maven(url="https://jitpack.io") {
        content {

            includeGroup("com.github.milchreis")
            includeGroup("com.github.ncoblentz")
            includeGroup("com.github.CoreyD97")
//
//
        }
    }

}


dependencies {
    testImplementation(kotlin("test"))

    // Include the Montoya API from Maven Central:
    // https://central.sonatype.com/artifact/net.portswigger.burp.extensions/montoya-api
    // Check for latest version: https://central.sonatype.com/artifact/net.portswigger.burp.extensions/montoya-api/versions
    implementation("net.portswigger.burp.extensions:montoya-api:2024.11")
    implementation ("io.github.java-diff-utils:java-diff-utils:4.12")
    implementation ("com.fifesoft:rsyntaxtextarea:3.5.3")
    implementation("com.github.milchreis:uibooster:1.21.1")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.15.2")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.15.2")


//    implementation("com.github.Google.Diff-Match-Patch:diff-match-patch:20121119")
//    implementation("com.github.ncoblentz:BurpMontoyaLibrary:0.1.12")

    // Enable these if you want to use https://github.com/ncoblentz/BurpMontoyaLibrary
    implementation("com.github.ncoblentz:BurpMontoyaLibrary:0.1.26")
    //https://github.com/Milchreis/UiBooster/releases (a dependency of BurpMontoyaLibrary)
    //implementation("com.github.milchreis:uibooster:1.21.1")
//    implementation ("com.github.CoreyD97:Burp-Montoya-Utilities:1.0.0")
//    implementation ("org.swinglabs:swingx:1.6.1")
    implementation ("com.github.CoreyD97:BurpExtenderUtilities:c03c1a2a1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}

tasks.processResources {
    from("gradle.properties")
}