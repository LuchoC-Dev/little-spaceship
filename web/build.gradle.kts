// TeaVM launcher.
import org.teavm.gradle.api.OptimizationLevel
import org.teavm.gradle.api.SourceFilePolicy

val gdxVersion: String by rootProject.extra

plugins {
    id("com.github.xpenatan.gdx-teavm") version "1.6.1"
}

dependencies {
    implementation(project(":game"))
    implementation("com.badlogicgames.gdx:gdx:$gdxVersion")
}

// The plugin adds backend-web on its own once a js {} or wasm {} target is
// declared, so it is never listed as a dependency by hand.
//
// The 1.6.1 plugin has no named targets, so release mode is selected with
// -Prelease.
val release = providers.gradleProperty("release").isPresent

gdxTeaVM {
    assets.from(rootProject.file("assets"))

    js {
        mainClass.set("dev.luchoc.littlespaceship.web.WebLauncher")
        htmlTitle.set("little-spaceship")
        optimization.set(if (release) OptimizationLevel.AGGRESSIVE else OptimizationLevel.NONE)
        obfuscated.set(release)
        debugInformation.set(!release)
        sourceMap.set(!release)
        if (!release) {
            sourceFilePolicy.set(SourceFilePolicy.COPY)
        }
    }
}
