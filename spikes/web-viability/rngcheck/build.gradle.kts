import org.teavm.gradle.api.JSModuleType
import org.teavm.gradle.api.OptimizationLevel

plugins {
    id("org.teavm") version "0.15.0"
}

teavm {
    js {
        mainClass.set("rngcheck.Main")
        obfuscated.set(false)
        optimization.set(OptimizationLevel.AGGRESSIVE)
        moduleType.set(JSModuleType.COMMON_JS)
        targetFileName.set("rngcheck.js")
        addedToWebApp.set(false)
    }
}

tasks.register<JavaExec>("runJvm") {
    mainClass.set("rngcheck.Main")
    classpath = sourceSets["main"].runtimeClasspath
}
