# Resultado del prototipo técnico

Ejecutado el 18/08/2026 sobre el spike de `spikes/web-viability`, siguiendo el criterio de aprobación fijado en `06-plataforma-y-validacion-tecnica.md`.

## Veredicto

**🟢 Web aprobada.** La candidata pasa a decisión de plataforma:

> **Java + libGDX + Gradle + gdx-teavm → navegador**, con desktop compartiendo el mismo core.

Ningún resultado obligó a deformar el diseño del juego, que era la condición para abandonar el target web.

## Stack verificado

| Componente | Versión |
|---|---|
| libGDX | 1.14.2 |
| gdx-teavm | 1.6.1 (plugin) / `backend-web` |
| TeaVM | 0.15.0 |
| Gradle | 9.7.0 |
| Java | JDK 25 instalado, compilando a bytecode 17 |

El artefacto `backend-teavm` fue renombrado a `backend-web`; el plugin lo añade solo al declarar `js {}` o `wasm {}`.

## Rendimiento

Escalón máximo del spike: **4000 entidades móviles** con colisión contra la nave. Es un orden de magnitud por encima de lo que el nivel 1 necesita.

| Target | FPS | mín | p1 | draw | update |
|---|---|---|---|---|---|
| Desktop (JVM, sin vsync) | 307 | 65 | 72 | 0,60 ms | 0,02 ms |
| Web JavaScript (release) | 60 | 45 | 50 | 10,34 ms | 0,05 ms |
| Web WebAssembly (release) | 61 | 42 | 56 | 10,05 ms | 0,03 ms |

Lectura: en navegador el presupuesto de fotograma a 60 fps es 16,6 ms y el dibujo consume 10 ms **en el peor caso sintético**.

La columna `update` de esta tabla mide el caso barato —n entidades contra un solo punto— y por sí sola no prueba que la simulación sea despreciable. La sección siguiente repite la medición con colisiones n × m reales.

Desktop tiene margen sobrado; sirve de línea base y confirma que el techo web es del navegador, no del diseño del core.

## JavaScript contra WebAssembly

| | JavaScript | WebAssembly |
|---|---|---|
| Tamaño | 662 KB | 729 KB |
| Comprimido | 192 KB | 245 KB |
| Rendimiento | equivalente | equivalente |

**No hay ganador.** Wasm rinde marginalmente mejor en el percentil 1 y peor en el mínimo; la diferencia está dentro del ruido. JavaScript pesa menos y su debugging es mejor: TeaVM ofrece source maps y depuración desde IntelliJ únicamente para el target JS.

Recomendación: **publicar JS**, manteniendo Wasm disponible porque el mismo proyecto genera ambos sin costo. La decisión puede revisarse cuando exista gameplay real.

## Resto del semáforo

| Área | Estado | Nota |
|---|---|---|
| Renderizado 2D y pixel-art | 🟢 | Escalado entero y nearest-neighbor sin deformación |
| Teclado y mouse simultáneos | 🟢 | El esquema aditivo decidido para el MVP funciona |
| Audio | 🟢 | Efectos y cambio de música en caliente, vía Howler |
| Carga de assets | 🟢 | 62-64 ms en el spike |
| Build web | 🟢 | JS y Wasm desde el mismo core, sin ramas por plataforma |
| Tamaño de descarga | 🟢 | 192 KB comprimido deja mucho margen para arte y audio |
| Core compartido con desktop | 🟢 | El launcher web no necesitó ninguna rama específica |
| Captura del puntero | 🟡 | No verificada; hace falta para el mouse relativo |
| Firefox, Edge y Safari | 🟡 | Solo se probó Chrome |
| Dependencias Java arbitrarias | 🟡 | Sigue vigente: cada una debe evaluarse por compatibilidad TeaVM |

## Hallazgos que afectan a la implementación

**`assets/startup-logo.png` es obligatorio.** El preloader del backend lo carga siempre y, si falta, la aplicación falla al terminar la precarga con un error que no menciona el logo. Hay que incluirlo desde el primer día del proyecto real.

**El canvas necesita tamaño explícito.** Con `config.width = 0` y `config.height = 0` el backend hereda el tamaño del contenedor, que arranca en 0×0 y deja el preloader sin stage. Habrá que decidir la política de redimensionado del canvas junto con la de escalado.

**Chrome headless no sirve para validar el runtime web.** Con SwiftShader la aplicación falla aunque en navegador real funcione. Cualquier verificación automatizada del target web —CI incluida— necesita GPU real o se limitará a comprobar que el build compila.

## Build tool

Queda resuelto lo que estaba pendiente desde el inicio: **Gradle**, no Maven.

No es una preferencia estética. El plugin de gdx-teavm es un plugin de Gradle, resuelve por sí solo el backend, los assets, el `index.html` y el servidor local, y genera las tareas de JS y Wasm. Reproducir eso con Maven sería integración manual sin ganancia.

## Consecuencias sobre los valores del MVP

La resolución lógica propuesta de **480×270** con campo de juego de 208 px se usó en todo el spike y funcionó sin problemas de escalado. Se confirma como punto de partida.

## Colisiones n x m: corrección de la primera medición

La primera prueba de rendimiento tenía un defecto: comparaba n entidades contra **un solo punto**, la nave. Son 4000 comprobaciones contra 1, no los pares proyectil × enemigo que un shoot 'em up necesita. Medía el caso barato.

Repetido con un benchmark de colisiones real (`spikes/web-viability/collisionbench`), en Java puro para que el mismo código corra en la JVM y en Node y la comparación mida el runtime, no dos implementaciones.

Cada escenario incluye proyectiles del jugador × enemigos, proyectiles enemigos × jugador y el movimiento de todo.

### JavaScript (TeaVM, optimización agresiva)

| Escenario | Pares por tick | Naive | Rejilla |
|---|---|---|---|
| MVP realista — 80 balas, 40 enemigos, 300 balas enemigas | 3.500 | 0,028 ms | 0,027 ms |
| Denso — 200, 100, 800 | 20.800 | 0,098 ms | 0,023 ms |
| Muy denso — 500, 200, 2000 | 102.000 | 0,423 ms | 0,072 ms |
| Absurdo — 1000, 500, 4000 | 504.000 | 2,108 ms | 0,207 ms |

### JVM, mismo código

| Escenario | Naive | Rejilla |
|---|---|---|
| MVP realista | 0,037 ms | 0,022 ms |
| Denso | 0,032 ms | 0,037 ms |
| Muy denso | 0,160 ms | 0,057 ms |
| Absurdo | 0,997 ms | 0,227 ms |

### Lectura

El presupuesto de fotograma a 60 fps es 16,6 ms.

- El escenario del MVP consume **0,17 %** del fotograma. No es medible al lado del dibujado.
- Medio millón de pares por tick, muy por encima de cualquier cosa que este juego vaya a producir, cuesta **2,1 ms sin optimizar** y **0,21 ms con rejilla**: el 12,7 % y el 1,2 % del fotograma.
- JavaScript es aproximadamente el doble de lento que la JVM en el bucle ingenuo, y prácticamente igual con rejilla.

La conclusión no cambia: **el cuello de botella es el dibujado, no la simulación**. Con 4000 entidades el dibujo costaba 10 ms; la lógica, con colisiones reales incluidas, se mantiene en fracciones de milisegundo.

### Dónde está la palanca real

Si algún día la colisión pesara, la solución es **algorítmica, no concurrente**. Una rejilla uniforme dio hasta **10× de mejora** en el peor escenario, más de lo que podrían dar ocho hilos, que además el navegador no ofrece.

El orden correcto de optimización para este juego es:

1. batching y atlas de texturas, porque el costo está en dibujar;
2. estructuras espaciales para la colisión, si alguna vez hiciera falta;
3. concurrencia, que en web no está disponible y en desktop no resolvería ninguno de los dos puntos anteriores.

## Concurrencia: qué permite realmente el target web

Medido el 18/08/2026 con una sonda de TeaVM puro (`spikes/web-viability/threadprobe`), ejecutada en Node para aislar el modelo de concurrencia de libGDX y de la GPU.

### Lo que no existe

Estas APIs **no están en la biblioteca de TeaVM 0.15.0**. No emiten un aviso: **rompen la compilación**.

| API | Estado |
|---|---|
| `java.util.concurrent.Executors` | no existe |
| `java.util.concurrent.ExecutorService` | no existe |
| `java.util.concurrent.CompletableFuture` | no existe |
| `java.util.concurrent.locks.ReentrantLock` | no existe |

### Lo que existe pero no hace lo que parece

`Thread` compila y `start()` no lanza excepción, así que un diseño multihilo *parece* funcionar. La medición dice otra cosa:

| Momento | Ticks del trabajador |
|---|---|
| Tras 20 millones de iteraciones del hilo principal, sin ceder | **0** |
| Tras un solo `Thread.sleep(50)` del hilo principal | 2000, completado |

El trabajador **no avanzó ni una vez** mientras el hilo principal trabajaba. Solo progresó cuando el principal cedió el control.

El modelo es **concurrencia cooperativa**, no paralelismo. TeaVM la emula con corrutinas sobre el único hilo de JavaScript. Dos hilos nunca ejecutan a la vez, así que repartir trabajo entre ellos no reduce el tiempo total: lo aumenta, por el costo de la conmutación.

`synchronized`, `AtomicInteger` y `ConcurrentHashMap` sí funcionan, pero protegen contra una concurrencia que no puede ocurrir.

**Trampa a evitar:** `Runtime.getRuntime().availableProcessors()` devuelve **8** en el navegador. Dimensionar cualquier cosa con ese número produce un diseño que se cree paralelo y es secuencial.

### Por qué esto no es un problema para este juego

El propio benchmark lo dice: con 4000 entidades, la lógica de juego consume **0,03-0,05 ms** por fotograma y el dibujado **10 ms**. El presupuesto se va íntegro en dibujar.

Paralelizar la simulación optimizaría el 0,3 % del fotograma. No hay ningún problema de rendimiento medido que el multihilo resuelva.

### Decisión

**El multihilo queda descartado** (18/08/2026). No es una limitación que se sufra: no hay ningún problema medido que resolvería.

### Consecuencia para la arquitectura

El core debe diseñarse **single-thread**, con un bucle de actualización determinista. Eso además:

- hace la simulación reproducible, que es lo que permite testear sistemas de juego de verdad;
- elimina una clase entera de bugs de concurrencia;
- mantiene desktop y web ejecutando exactamente el mismo código.

Si en el futuro apareciera una tarea realmente paralelizable, la salida no es `Thread` sino Web Workers con paso de mensajes y sin memoria compartida, que gdx-teavm no abstrae y habría que integrar a mano. No conviene diseñar para eso sin un caso real.

## Versión de Java

Medido con una sonda de características del lenguaje (`spikes/web-viability/langprobe`), compilada a TeaVM y ejecutada en Node. Solo características estables; nada marcado como preview.

### Java 17 — todo funciona

| Característica | Estado |
|---|---|
| `record`, con `equals`, `hashCode` y `toString` | ok |
| `sealed interface` | ok |
| pattern matching para `instanceof` | ok |
| `switch` como expresión | ok |
| text blocks | ok |
| `List.of` / `Map.of` | ok |
| `Optional` | ok |
| lambdas e interfaces funcionales | ok |
| `String.format` | ok |

### Java 21 — también funciona

Los patrones sobre registros y el `switch` exhaustivo sobre jerarquías selladas se ejecutan correctamente. El pipeline completo —libGDX 1.14.2 más gdx-teavm más TeaVM— **compila** con bytecode 21 y produce un artefacto de tamaño equivalente.

### Decisión: Java 17

Se elige 17 por el criterio de acoplarse a lo que el runtime tiene probado, no a lo más nuevo:

- Todo el spike se ejecutó en navegador real con bytecode 17. De 21 está verificada la compilación del pipeline, pero no la ejecución del juego completo en navegador.
- 17 es el baseline habitual del ecosistema libGDX.
- Lo que 21 aporta sobre 17 es modesto para este proyecto: con `record`, `sealed` y pattern matching de `instanceof` ya está cubierto casi todo lo que el dominio necesita.

Ambas son LTS, así que no hay urgencia. Subir a 21 más adelante es cambiar dos líneas del build, y ya sabemos que compila.

## Lo que sigue pendiente

- Captura del puntero para el mouse relativo, que es la única decisión de control sin verificar.
- Firefox, Edge y Safari.
- Comportamiento del audio tras la política de interacción del usuario del navegador. El flujo previsto —cargar, menú, pulsar Jugar— ya provee esa interacción, pero conviene confirmarlo con el menú real.
- Medición con arte y audio definitivos, cuando existan: el spike genera sus texturas por código y no representa el peso real de los assets.
