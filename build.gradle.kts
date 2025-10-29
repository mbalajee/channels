plugins {
    kotlin("jvm") version "1.9.23"
    application
}

group = "in.learn"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    // Coroutines core for channels and concurrency
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
    // Coroutines test utilities
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}
application {
    mainClass.set("com.example.channel.MainKt")
}
