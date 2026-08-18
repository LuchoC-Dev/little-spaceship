// Spike de viabilidad web — proyecto descartable.
// No es la base del juego: solo mide si el stack aguanta lo que el MVP necesita.

val gdxVersion by extra("1.14.2")
val gdxTeaVMVersion by extra("1.6.1")

subprojects {
    apply(plugin = "java")

    repositories {
        mavenCentral()
    }

    extensions.configure<JavaPluginExtension> {
        // TeaVM no consume bytecode de las versiones más nuevas de Java.
        // 17 es el punto seguro compilando con el JDK 25 instalado.
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
