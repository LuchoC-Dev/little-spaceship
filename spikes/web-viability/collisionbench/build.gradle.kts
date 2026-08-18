import org.teavm.gradle.api.JSModuleType
import org.teavm.gradle.api.OptimizationLevel

plugins {
    id("org.teavm") version "0.15.0"
}

// Java puro, sin libGDX: el mismo codigo se ejecuta en la JVM y en Node,
// de modo que la comparacion mide el runtime y no dos implementaciones.
teavm {
    js {
        mainClass.set("colbench.Main")
        obfuscated.set(false)
        // AGGRESSIVE es lo que se publicaria; medir sin optimizar enganaria.
        optimization.set(OptimizationLevel.AGGRESSIVE)
        moduleType.set(JSModuleType.COMMON_JS)
        targetFileName.set("colbench.js")
        addedToWebApp.set(false)
    }
}

tasks.register<JavaExec>("benchJvm") {
    group = "verification"
    description = "Corre el benchmark de colisiones sobre la JVM"
    mainClass.set("colbench.Main")
    classpath = sourceSets["main"].runtimeClasspath
}
