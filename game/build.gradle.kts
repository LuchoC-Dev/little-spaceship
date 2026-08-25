// Presentation layer: libGDX lives here, never in core.
// Sources arrive in phase 03, owned by the game-presentation agent.
val gdxVersion: String by rootProject.extra

dependencies {
    implementation(project(":core"))
    implementation("com.badlogicgames.gdx:gdx:$gdxVersion")
}

// `tools.audio` (GenerateAudio, Synth, Wav) is a design-time generator: it uses java.nio.file,
// which does not exist on TeaVM. It used to sit under `main` and was harmless only while `web`'s
// gdxTeaVM {} block stayed commented out — TeaVM compiles every class reachable from `main`, so
// it would have broken the moment the web build ran, even though nothing in the game ever calls
// it at runtime. It lives in its own source set so `web`'s dependency on `:game` (which pulls in
// only the `main` sourceSet) never sees it.
sourceSets {
    create("tools") {
        java.srcDir("src/tools/java")
    }
}

// Design-time only: regenerates the procedural WAV assets under assets/audio. Never part of
// `build` or `test` — see GenerateAudio's own javadoc for why the output is committed instead.
tasks.register<JavaExec>("generateAudio") {
    group = "content"
    description = "Regenerates the procedural sound effects and music loops under assets/audio."
    mainClass.set("dev.luchoc.littlespaceship.game.tools.audio.GenerateAudio")
    classpath = sourceSets["tools"].runtimeClasspath
    args = listOf(rootProject.file("assets/audio").absolutePath)
}
