plugins {
    kotlin("jvm") version "2.2.0"
    application
}

group = "inoagent.maxapi"
version = "1.0"

repositories {
    mavenCentral()
}

val junitJupiterVersion = "5.9.3"
dependencies {
    implementation("net.jpountz.lz4:lz4:1.3.0")
    implementation("org.msgpack:msgpack-core:0.9.10")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json-jvm:1.9.0")
    testImplementation("org.junit.jupiter:junit-jupiter-api:${junitJupiterVersion}")
    testImplementation("org.junit.jupiter:junit-jupiter-params:${junitJupiterVersion}")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:${junitJupiterVersion}")
}

kotlin {
    jvmToolchain(21)
}

application {
    mainClass.set("inoagent.maxapi.MainKt")
}

tasks.test {
    useJUnitPlatform()
}