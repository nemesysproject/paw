buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:9.2.1")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.2.10")
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
    }
}

tasks.register<Delete>("clean").configure {
    delete("build")
}

// Configuración para unificar Java y Kotlin en la versión 11 (Sintaxis Kotlin DSL)
plugins {
    kotlin("jvm") version "2.2.10" apply false
}

kotlin {
    jvmToolchain(11)
}
