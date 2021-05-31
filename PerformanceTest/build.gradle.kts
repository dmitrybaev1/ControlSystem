import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.5.10"
    application
}

group = "me.dbaev"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies{
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.4.31")
    implementation("org.eclipse.paho:org.eclipse.paho.client.mqttv3:1.0.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.3")
}

tasks.withType<KotlinCompile>() {
    kotlinOptions.jvmTarget = "1.8"
}

application {
    mainClassName = "MainKt"
}