import org.teavm.gradle.api.JSModuleType
import org.teavm.gradle.api.OptimizationLevel

plugins {
    id("org.teavm") version "0.15.0"
}

// Modulo de sonda: TeaVM puro, sin libGDX. Se ejecuta en Node para poder
// medir el modelo de concurrencia sin depender de GPU ni de una ventana.
teavm {
    js {
        mainClass.set("probe.Main")
        obfuscated.set(false)
        optimization.set(OptimizationLevel.NONE)
        // CommonJS para poder invocar main() desde Node.
        moduleType.set(JSModuleType.COMMON_JS)
        targetFileName.set("probe.js")
        addedToWebApp.set(false)
    }
}
