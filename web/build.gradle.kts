// TeaVM launcher. Sources arrive in phase 03, owned by game-presentation; the
// gdxTeaVM block below is wired but stays commented out until WebLauncher
// exists, because the plugin needs a real main class to configure a target.
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
// -Prelease. The two imports go back to the top of the file when this is
// uncommented; Kotlin only accepts them there.
//
// import org.teavm.gradle.api.OptimizationLevel
// import org.teavm.gradle.api.SourceFilePolicy
//
// val release = providers.gradleProperty("release").isPresent
//
// gdxTeaVM {
//     assets.from(rootProject.file("assets"))
//
//     js {
//         mainClass.set("dev.luchoc.littlespaceship.web.WebLauncher")
//         htmlTitle.set("little-spaceship")
//         optimization.set(if (release) OptimizationLevel.AGGRESSIVE else OptimizationLevel.NONE)
//         obfuscated.set(release)
//         debugInformation.set(!release)
//         sourceMap.set(!release)
//         if (!release) {
//             sourceFilePolicy.set(SourceFilePolicy.COPY)
//         }
//     }
// }
