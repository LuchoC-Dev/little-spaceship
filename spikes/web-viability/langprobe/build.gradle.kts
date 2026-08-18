import org.teavm.gradle.api.JSModuleType
import org.teavm.gradle.api.OptimizationLevel

plugins {
    id("org.teavm") version "0.15.0"
}

// La version de bytecode se controla con -PjavaRelease para poder comparar
// que digiere TeaVM en 17 y en 21.
val javaRelease = (providers.gradleProperty("javaRelease").orNull ?: "17").toInt()

java {
    sourceCompatibility = JavaVersion.toVersion(javaRelease)
    targetCompatibility = JavaVersion.toVersion(javaRelease)
}

// El codigo especifico de Java 21 solo entra cuando se compila con esa version.
if (javaRelease >= 21) {
    sourceSets["main"].java.srcDir("src/java21/java")
}

teavm {
    js {
        mainClass.set(if (javaRelease >= 21) "langprobe.Main21" else "langprobe.Main")
        obfuscated.set(false)
        optimization.set(OptimizationLevel.NONE)
        moduleType.set(JSModuleType.COMMON_JS)
        targetFileName.set("langprobe.js")
        addedToWebApp.set(false)
    }
}
