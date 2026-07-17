plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.9.22"
    id("org.jetbrains.intellij") version "1.17.3"
}

group = "com.example"
version = "0.1.0"

repositories {
    mavenCentral()
}

// Pinned explicitly to JDK 17: without this, Gradle compiles with whatever
// JDK it happens to run on, which can produce bytecode the IntelliJ Platform
// 2023.3 sandbox (JDK 17) cannot load.
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    jvmToolchain(17)
}

intellij {
    version.set("2023.3")
    type.set("IC") // IntelliJ IDEA Community

    // com.intellij.java is required: PsiMethod and JavaPsiFacade belong to
    // the bundled Java plugin, not the base platform.
    plugins.set(listOf("com.intellij.java", "Kotlin"))
}

tasks {
    patchPluginXml {
        sinceBuild.set("233")
        untilBuild.set("241.*")
    }
}