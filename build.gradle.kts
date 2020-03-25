
group = "org.jetbrains.kotlin.script.examples"
version = "1.0-SNAPSHOT"

plugins {
    kotlin("jvm") version "1.3.70-eap-184"
}

val kotlinVersion: String by extra("1.3.70-eap-184")

repositories {
    maven("https://dl.bintray.com/kotlin/kotlin-eap")
    maven("https://dl.bintray.com/jakubriegel/kotlin-shell")
    jcenter()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-scripting-jvm-host:$kotlinVersion")
    implementation("org.jetbrains.kotlin:kotlin-scripting-dependencies:$kotlinVersion")
    implementation("org.jetbrains.kotlin:kotlin-scripting-compiler:$kotlinVersion")
    implementation("org.jetbrains.kotlin:kotlin-script-util:$kotlinVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.3")
    implementation("eu.jrie.jetbrains:kotlin-shell-core:0.2")
    implementation("org.slf4j:slf4j-nop:1.7.26")
}

sourceSets {
    test {
        dependencies {
            testImplementation("junit:junit:4.12")
        }
    }
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
}