# little-spaceship

Shoot 'em up vertical por niveles, en pixel-art, hecho desde cero en Java. Single-player y local. Es una pieza de portfolio: tiene que demostrar arquitectura, pruebas, CI, rendimiento, documentación, arte y despliegue.

No es un remake. No se reutiliza código ni assets de proyectos anteriores.

## Stack

| | |
|---|---|
| Java | 17 (compilado con JDK 25 instalado) |
| Framework | libGDX 1.14.2 |
| Web | gdx-teavm 1.6.1 + TeaVM 0.15.0 → JavaScript |
| Build | Gradle con wrapper |
| Pruebas | JUnit 5 |
| Paquete raíz | `dev.luchoc.littlespaceship` |

Todo esto está **medido, no supuesto**: ver `docs/planificacion/11-resultado-prototipo-tecnico.md`.

## Módulos

```
core/     Java puro. Simulación, reglas, ECS. Sin libGDX.
game/     libGDX. Render, HUD, pantallas, audio, entrada, assets.
desktop/  Launcher LWJGL3.
web/      Launcher TeaVM.
assets/   Contenido: JSON, sprites, audio.

desktop ─┐
         ├─→ game ──→ core
web ─────┘
```

## Invariantes

No son preferencias: están decididas y medidas. Romperlas invalida trabajo previo.

1. **`core` no depende de libGDX.** Ni de sus utilidades matemáticas. Si hace falta importar `com.badlogic.gdx` en `core`, el diseño está mal.
2. **Determinismo.** El core no lee el reloj, no lee la entrada directamente y no usa `Math.random()`. Recibe paso fijo (1/60), un `InputFrame` inmutable y un `Rng` con semilla. Los replays dependen de esto y fallan en silencio si se rompe.
3. **Single-thread.** El target web no da paralelismo real y `ExecutorService`, `CompletableFuture` y `ReentrantLock` **no existen en TeaVM**: rompen la compilación. Además no hace falta — la lógica cuesta fracciones de milisegundo contra 10 ms de dibujado.
4. **Contratos en las fronteras.** Ningún módulo expone clases concretas a otro. Lo que cruza es inmutable o de solo lectura. `game` no manipula el ECS: lee por `WorldView`.
5. **Orden fijo de sistemas.** Es parte de las reglas del juego, no un detalle de implementación.
6. **Nada de abstracciones sin un caso real en el MVP.**

## Trampas del target web

Documentadas tras costar horas en el spike:

- **`assets/startup-logo.png` es obligatorio.** Sin él, la app revienta al terminar la precarga con un error que no lo menciona.
- **El canvas necesita tamaño explícito.** Con `config.width = 0` hereda un contenedor de 0×0 y el preloader queda sin stage.
- **Chrome headless no sirve** para validar el runtime web: falla con SwiftShader aunque el navegador real funcione. CI solo puede verificar que el build compila.
- **JSON con `JsonReader`/`JsonValue`**, nunca con la clase `Json`: usa reflexión y en TeaVM habría que declarar cada clase.
- Cada dependencia nueva se evalúa por compatibilidad con TeaVM antes de añadirla.

## Convenciones

- **Todo el código en inglés**: identificadores, comentarios, logs, claves JSON e ids de contenido.
- La documentación de `docs/planificacion/` está en español porque es de la etapa de planificación. La documentación nueva de implementación —ADR incluidos— va en inglés, y esos documentos se traducirán.
- La conversación con el usuario es siempre en español.
- Composición sobre herencia. Los componentes son datos puros, sin lógica.
- Resolución lógica 480×270, campo de juego 208 px, escalado entero, nearest-neighbor.

## Rendimiento: dónde está el coste

Medido en el spike, con 4000 entidades: el dibujado cuesta ~10 ms y la lógica ~0,05 ms. Con colisiones n×m reales, medio millón de pares por tick cuesta 2,1 ms sin optimizar.

Orden de optimización: **batching y atlas primero**, estructuras espaciales para colisión si alguna vez hace falta, concurrencia nunca.

## Agentes

Definidos en `.claude/agents/`, cada uno con memoria persistente en `.claude/agent-memory/`.

| Agente | Dueño de |
|---|---|
| `core-domain` | `core/` — ECS, sistemas, reglas |
| `game-presentation` | `game/`, `desktop/`, `web/` — render, HUD, audio, entrada |
| `visual-designer` | dirección visual; produce documentos, no código |
| `test-engineer` | pruebas unitarias y replays |
| `reviewer` | solo lee y reporta |

Las fronteras salen de la arquitectura: un agente no escribe fuera de su módulo.

## Documentación

`docs/planificacion/` — 13 documentos. Los que más se consultan:

- `02-especificacion-funcional-mvp.md` — qué entra en el MVP
- `03-sistemas-de-juego.md` — reglas de juego
- `08-registro-de-decisiones-y-pendientes.md` — qué está decidido y qué sigue abierto
- `10-valores-iniciales-mvp.md` — valores de balance
- `11-resultado-prototipo-tecnico.md` — mediciones y decisión de plataforma
- `12-arquitectura.md` — estructura, ECS, contratos, pruebas

**Antes de inventar una regla del juego, búscala ahí: casi todo está decidido.** `08` distingue lo confirmado de lo provisional y lo abierto.

`spikes/web-viability/` es el prototipo descartable que validó la plataforma. No es la base del juego y puede borrarse cuando ya no aporte.
