import org.teavm.gradle.api.OptimizationLevel
import org.teavm.gradle.api.SourceFilePolicy

val gdxVersion: String by rootProject.extra

plugins {
    id("com.github.xpenatan.gdx-teavm") version "1.6.1"
}

dependencies {
    implementation(project(":core"))
    implementation("com.badlogicgames.gdx:gdx:$gdxVersion")
}

// El plugin 1.6.1 no admite targets nombrados, asi que el modo release se
// selecciona con -Prelease. Hay que medir ambos: el build de desarrollo dice
// como se trabaja, el de release dice que recibe el jugador.
val release = providers.gradleProperty("release").isPresent

gdxTeaVM {
    assets.from(rootProject.file("assets"))

    js {
        mainClass.set("spike.web.WebLauncher")
        htmlTitle.set("Spike de viabilidad - JS")
        optimization.set(if (release) OptimizationLevel.AGGRESSIVE else OptimizationLevel.NONE)
        obfuscated.set(release)
        debugInformation.set(!release)
        sourceMap.set(!release)
        if (!release) {
            sourceFilePolicy.set(SourceFilePolicy.COPY)
        }
        serverPort.set(8181)
    }

    wasm {
        mainClass.set("spike.web.WebLauncher")
        htmlTitle.set("Spike de viabilidad - Wasm")
        optimization.set(if (release) OptimizationLevel.AGGRESSIVE else OptimizationLevel.NONE)
        obfuscated.set(release)
        debugInformation.set(!release)
        serverPort.set(8282)
    }
}
