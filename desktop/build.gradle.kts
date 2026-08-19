// LWJGL3 launcher. Sources arrive in phase 03, owned by game-presentation.
val gdxVersion: String by rootProject.extra

dependencies {
    implementation(project(":game"))
    implementation("com.badlogicgames.gdx:gdx:$gdxVersion")
    implementation("com.badlogicgames.gdx:gdx-backend-lwjgl3:$gdxVersion")
    implementation("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-desktop")
}
