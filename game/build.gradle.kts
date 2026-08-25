// Presentation layer: libGDX lives here, never in core.
// Sources arrive in phase 03, owned by the game-presentation agent.
val gdxVersion: String by rootProject.extra

dependencies {
    implementation(project(":core"))
    implementation("com.badlogicgames.gdx:gdx:$gdxVersion")
}

// Design-time only: regenerates the procedural WAV assets under assets/audio. Never part of
// `build` or `test` — see GenerateAudio's own javadoc for why the output is committed instead.
tasks.register<JavaExec>("generateAudio") {
    group = "content"
    description = "Regenerates the procedural sound effects and music loops under assets/audio."
    mainClass.set("dev.luchoc.littlespaceship.game.tools.audio.GenerateAudio")
    classpath = sourceSets["main"].runtimeClasspath
    args = listOf(rootProject.file("assets/audio").absolutePath)
}
