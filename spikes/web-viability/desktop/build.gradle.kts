val gdxVersion: String by rootProject.extra

dependencies {
    implementation(project(":core"))
    implementation("com.badlogicgames.gdx:gdx:$gdxVersion")
    implementation("com.badlogicgames.gdx:gdx-backend-lwjgl3:$gdxVersion")
    implementation("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-desktop")
}

tasks.register<JavaExec>("run") {
    group = "application"
    description = "Ejecuta el spike en desktop"
    mainClass.set("spike.desktop.DesktopLauncher")
    classpath = sourceSets["main"].runtimeClasspath
    workingDir = rootProject.file("assets")
}

tasks.register<JavaExec>("bench") {
    group = "application"
    description = "Corre el benchmark en desktop y publica el informe por consola"
    mainClass.set("spike.desktop.DesktopLauncher")
    classpath = sourceSets["main"].runtimeClasspath
    workingDir = rootProject.file("assets")
    args("--bench")
}
