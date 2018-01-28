import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    application
    kotlin("jvm") version "1.2.20"
}

group = "info.vividcode.example"
version = "1.0-SNAPSHOT"

application {
    mainClassName = "info.vividcode.sample.wdip.MainKt"
}

dependencies {
    compile(kotlin("stdlib-jdk8"))
}

repositories {
    jcenter()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}
