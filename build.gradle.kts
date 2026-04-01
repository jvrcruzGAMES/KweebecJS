plugins {
    java
    id("com.gradleup.shadow") version "9.3.1"
}

group = "games.jvrcruz"
version = "1.0.1-BETA"

repositories {
    mavenCentral()
}

val hytaleInstallPath: String by project
val hytaleServerJarPath: String by project

val resolvedServerJar = hytaleServerJarPath.ifBlank { "$hytaleInstallPath/Server/HytaleServer.jar" }

dependencies {
    compileOnly(files(resolvedServerJar))
    implementation("org.mozilla:rhino:1.9.1")
    compileOnly(files("$hytaleInstallPath/Assets.zip"))
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

tasks.shadowJar {
    archiveClassifier.set("")
}

tasks.register<Copy>("deployMod") {
    group = "hytale"
    description = "Builds the mod and copies it to the project-local server mods folder."
    dependsOn(tasks.shadowJar)
    from(tasks.shadowJar.flatMap { it.archiveFile })
    into("$projectDir/.hytale-server/mods")
}

tasks.register("cleanDeploy") {
    group = "hytale"
    description = "Cleans, rebuilds, and deploys the mod."
    dependsOn("clean", "deployMod")
}

tasks.named("deployMod") {
    mustRunAfter("clean")
}
