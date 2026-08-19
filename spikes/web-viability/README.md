# Spike de viabilidad web

Prueba **descartable**. No es la base del juego y no debe convertirse en ella: existe solo para decidir si `Java + libGDX + Gradle + gdx-teavm` aguanta lo que el MVP necesita, antes de comprometerse con la plataforma.

Cuando la decisión esté tomada y registrada, esta carpeta puede borrarse.

## Qué mide

| Área | Cómo |
|---|---|
| Rendimiento | Escalones de 250 a 4000 entidades móviles con colisión contra la nave |
| Input | Teclado y mouse **aditivos**, tal como se decidió para el MVP |
| Audio | Efecto puntual y cambio de música en caliente, como al entrar el boss |
| Pixel-art | Tablero de ajedrez con nearest-neighbor: cualquier suavizado se ve al instante |
| Assets | Tiempo de carga, medido y mostrado en el HUD |
| Build web | JavaScript y WebAssembly desde el mismo core |

## Stack

| Componente | Versión |
|---|---|
| libGDX | 1.14.2 |
| gdx-teavm (plugin) | 1.6.1 |
| TeaVM | 0.15.0 |
| Gradle | 9.7.0 (wrapper) |
| Java | compilado a bytecode 17 |

## Comandos

```bash
# Línea base en desktop, con informe por consola
./gradlew :desktop:bench

# Desktop interactivo
./gradlew :desktop:run

# Web: build de desarrollo (source maps, sin ofuscar)
./gradlew gdx_teavm_web_js_build
./gradlew gdx_teavm_web_wasm_build

# Web: build de release, que es el que hay que medir
./gradlew gdx_teavm_web_js_build -Prelease
./gradlew gdx_teavm_web_wasm_build -Prelease

# Web con servidor incluido
./gradlew gdx_teavm_web_js_run     # http://localhost:8181
./gradlew gdx_teavm_web_wasm_run   # http://localhost:8282
```

La salida queda en `web/build/dist/js/webapp` y `web/build/dist/wasm/webapp`.

## Controles

| Tecla | Acción |
|---|---|
| Flechas | mover |
| Shift | movimiento lento |
| Espacio | efecto de sonido |
| M | cambiar de música |
| 1..5 | fijar la carga de entidades |
| +/- | subir o bajar un escalón |
| TAB | capturar o liberar el puntero |
| B | lanzar el benchmark automático |
| R | reiniciar métricas |

El benchmark recorre los cinco escalones solo, midiendo 3 segundos por escalón tras 1 segundo de calentamiento. En desktop publica el informe por consola; en navegador lo deja dibujado en pantalla.

## Benchmark de colisiones

El módulo `collisionbench` mide colisiones n × m reales (proyectiles × enemigos), en Java puro para poder correr el mismo código en ambos runtimes:

```bash
./gradlew :collisionbench:benchJvm                  # JVM
./gradlew :collisionbench:generateJavaScript
cd collisionbench/build/generated/teavm/js && node run.cjs   # JavaScript
```

Compara la estrategia ingenua contra una rejilla uniforme sobre los mismos datos.

## Sonda de concurrencia

El módulo `threadprobe` es TeaVM puro, sin libGDX, para poder medir el modelo de concurrencia en Node sin GPU ni ventanas:

```bash
./gradlew :threadprobe:generateJavaScript
cd threadprobe/build/generated/teavm/js && node run.cjs
```

Resultado: el target web **no ofrece paralelismo real**. Detalle en `docs/planning/11-technical-prototype-results.md`.

## Notas encontradas durante el spike

**`assets/startup-logo.png` es obligatorio.** El preloader del backend lo carga siempre. Si falta, la app revienta con `Cannot read properties of null` al terminar la precarga. El archivo incluido aquí se extrajo del propio `backend-web`.

**El canvas necesita tamaño explícito.** Con `config.width = 0` y `config.height = 0` el backend hereda el tamaño del contenedor, que arranca en 0×0, y el preloader queda sin stage válido.

**Chrome headless no sirve para validar esto.** Con `--headless=new` y SwiftShader la app falla en el dispose del preloader aunque en navegador real funcione perfectamente. Cualquier verificación de runtime web hay que hacerla con GPU real.
