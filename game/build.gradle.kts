// Presentation layer: libGDX lives here, never in core.
// Sources arrive in phase 03, owned by the game-presentation agent.
val gdxVersion: String by rootProject.extra

dependencies {
    implementation(project(":core"))
    implementation("com.badlogicgames.gdx:gdx:$gdxVersion")
}
