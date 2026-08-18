# Arquitectura

Decidida el 18/08/2026, sobre la plataforma validada en `11-resultado-prototipo-tecnico.md` y las reglas de `02` y `03`.

Este documento define la estructura del proyecto real, no del spike. La regla que lo gobierna todo: **no se construye ninguna abstracción que no tenga un caso concreto en el MVP**.

## Decisiones estructurales

| Decisión | Elección |
|---|---|
| Modelo de entidades | ECS propio, escrito a mano, sin librería |
| Contenido balanceable | JSON externo, leído sin reflexión |
| Pruebas | Sistemas puros más replays deterministas |
| Concurrencia | Ninguna. Bucle single-thread determinista |
| Inyección de dependencias | Manual por constructor, sin framework |

## Configuración del proyecto

| Elemento | Valor |
|---|---|
| Repositorio | `little-spaceship`, privado al principio |
| Paquete raíz | `dev.luchoc.littlespaceship` |
| Versión de Java | 17 |
| Build | Gradle con wrapper |
| Pruebas | JUnit 5 |

Sobre Java 17: elegido por estar verificado de punta a punta en el spike, incluida la ejecución en navegador real. Java 21 también compila en todo el pipeline y sus características se ejecutan en TeaVM, así que subir más adelante es un cambio de dos líneas. El detalle está en `11-resultado-prototipo-tecnico.md`.

## Módulos

```
little-spaceship/
  core/        Java puro. Simulación, reglas y ECS. Sin libGDX.
  game/        libGDX. Presentación, assets, entrada y pantallas.
  desktop/     Launcher LWJGL3.
  web/         Launcher TeaVM.
  assets/      Contenido: JSON, sprites, audio.
```

Dependencias, en un solo sentido:

```
desktop ─┐
         ├─→ game ──→ core
web ─────┘
```

### Por qué `core` es Java puro

`core` **no depende de libGDX**. Ni siquiera de sus utilidades matemáticas.

No es purismo: es lo que hace que la simulación sea testeable en milisegundos, que los replays sean fiables y que el determinismo no dependa del framework. El spike ya demostró que Java puro se comporta igual en la JVM y en el navegador — `collisionbench` corre en ambos sin un solo cambio.

El costo es reimplementar un `Vec2` y unas pocas funciones matemáticas. Son decenas de líneas y a cambio el módulo que contiene todas las reglas del juego no puede romperse por un cambio de framework.

La regla es mecánica y verificable: **si `core` necesita importar `com.badlogic.gdx`, el diseño está mal**.

### Qué vive en `game`

Todo lo que toca el framework:

- carga de assets y traducción de JSON a definiciones de contenido;
- render del mundo y del HUD;
- audio, reaccionando a los eventos que emite el core;
- lectura de input y construcción del `InputFrame` de cada tick;
- pantallas de menú, opciones, selección de nave, victoria y derrota, con `scene2d.ui`.

## Arquitectura hexagonal

El proyecto sigue **puertos y adaptadores**: el dominio en el centro, sin conocer a nadie, y todo lo externo —framework, render, audio, entrada, archivos— conectado desde fuera a través de puertos que el propio dominio declara.

La regla de dependencia es una sola y no admite excepciones: **todo apunta hacia adentro**. El dominio no importa infraestructura; la infraestructura importa el dominio.

```
                 ┌──────────────────────────────┐
                 │        adaptadores           │   game, desktop, web
                 │  render · audio · input      │
                 │  contenido · persistencia    │
                 └──────────────┬───────────────┘
                                │  implementan
                 ┌──────────────▼───────────────┐
                 │           puertos            │   core.port
                 └──────────────┬───────────────┘
                                │  declarados por
                 ┌──────────────▼───────────────┐
                 │          aplicación          │   core.application
                 ├──────────────────────────────┤
                 │           dominio            │   core.domain
                 │  reglas, entidades, sistemas │
                 └──────────────────────────────┘
```

### Qué se toma y qué no

Hexagonal se aplica **entero**: es exactamente el problema que tenemos, un núcleo de reglas que no debe acoplarse a libGDX ni a TeaVM ni al formato del contenido.

De Clean Architecture se toma la regla de dependencia y la separación entre dominio e infraestructura, pero **no se fuerzan los casos de uso donde no los hay**. Un bucle de juego no es una operación transaccional: `MotionSystem` no es un caso de uso, es una regla que corre sesenta veces por segundo. Modelarlo como tal añadiría ceremonia sin comprar nada, y contradice la regla de no construir abstracciones sin caso real.

Donde sí hay casos de uso genuinos es **fuera del bucle**, en las acciones que el jugador dispara una vez: iniciar una partida, seleccionar nave, aplicar opciones y, más adelante, guardar y continuar. Esos viven en `core.application`.

### Estructura de paquetes

```
core/
  domain/        reglas, entidades, componentes, sistemas, eventos
                 sin una sola dependencia externa
  application/   orquestación del bucle y casos de uso fuera de él
  port/          interfaces que el dominio necesita del exterior

game/
  adapter/content/     JSON  → ContentSource
  adapter/render/      WorldView → SpriteBatch
  adapter/audio/       GameEvent → Sound
  adapter/input/       teclado y mouse → InputFrame
  screens/             scene2d.ui
  Composition.java     raíz de composición
```

Los adaptadores son intercambiables por definición: sustituir libGDX, cambiar JSON por otro formato o añadir un target nuevo toca `game`, nunca `core`.

## Contratos en las fronteras

Sobre ese marco, una regla estricta: **ningún módulo expone clases concretas a otro**. Todo cruce de frontera ocurre a través de un contrato, y el contrato lo define **quien consume**, no quien implementa.

Esto vale también dentro de `core`: el dominio se modela con conceptos nombrados —nave, enemigo, proyectil, acoplamiento— y no con identificadores sueltos y componentes anónimos manipulados desde cualquier parte.

### Lo que implica

**`core` declara lo que necesita; `game` lo implementa.** El core no conoce ni una sola clase de `game`. Los puertos son suyos:

```java
public interface ContentSource {
    EnemyDefinition enemy(String id);
    WaveTimeline timeline(String levelId);
    BalanceValues balance();
}

public interface GameEventSink {
    void emit(GameEvent event);
}
```

`EnemyDefinition`, `WaveTimeline` y `BalanceValues` son contratos del dominio. `game` los construye leyendo JSON, pero el core nunca ve ni el JSON ni la clase que lo parsea.

**`game` no manipula el ECS.** Para renderizar no accede a componentes: lee a través de un contrato de solo lectura.

```java
public interface WorldView {
    void forEachSprite(SpriteVisitor visitor);
    PlayerStatus player();
    BossStatus boss();
}

public interface SpriteVisitor {
    void accept(SpriteId sprite, float x, float y, int frame, float rotation);
}
```

El visitante es deliberado: recorrer con él no asigna un objeto por entidad y por fotograma, que a 60 fps y cientos de entidades sería basura constante para el recolector. El contrato protege la frontera **y** el rendimiento.

**Lo que cruza es inmutable o de solo lectura.** `InputFrame` entra inmutable. `GameEvent` sale inmutable. Nadie recibe una referencia con la que pueda modificar el estado de otro módulo.

### Qué cuesta y qué compra

Cuesta más interfaces y la disciplina de no filtrar tipos de implementación, que es la forma habitual en que este tipo de regla se degrada: basta un `getter` que devuelva la clase concreta "por comodidad" para perder la frontera entera.

A cambio: el dominio queda explícito en vez de disuelto en el framework, los tests dobles se escriben sin esfuerzo porque todo es una interfaz, y cambiar libGDX o el formato del contenido no toca ni una regla del juego.

### Cómo se verifica

La frontera no se sostiene con buenas intenciones. Se comprueba de forma mecánica:

- `core` no declara la dependencia de libGDX, así que no puede importarlo aunque quiera;
- `core` no declara dependencia de `game`, así que la flecha nunca puede invertirse;
- un test de arquitectura verifica la regla de dependencia entre capas y que ningún tipo público de `core` exponga clases de implementación.

## El bucle

Paso fijo, acumulador, sin interpolación:

```
acumulador += min(deltaReal, 0.25)     // el tope evita la espiral de la muerte
mientras acumulador >= PASO:
    mundo.update(PASO, inputDelTick)
    acumulador -= PASO
render(mundo)
```

`PASO` es 1/60. La simulación **nunca** recibe un delta variable, porque un delta variable destruye el determinismo y con él los replays.

Sin interpolación al renderizar: en pixel-art con posiciones ajustadas a píxel entero, interpolar aporta poco y complica. Si alguna vez se nota, se añade en la capa de presentación sin tocar el core.

### Tres reglas de determinismo

El core no puede:

1. **leer el reloj** — recibe el paso fijo;
2. **leer el input directamente** — recibe un `InputFrame` inmutable por tick;
3. **usar `Math.random()`** — usa un `Rng` propio con semilla explícita.

Romper cualquiera de las tres invalida los replays en silencio, así que conviene tratarlas como invariantes, no como preferencias.

## El ECS

Escrito a mano. Tres piezas y nada más.

### Entidad

Un `int`. Un identificador y su generación, para detectar referencias a entidades ya destruidas.

### Componentes

Datos puros, sin lógica ni métodos de comportamiento. Los del MVP:

| Componente | Contenido |
|---|---|
| `Transform` | posición |
| `Motion` | velocidad y, opcionalmente, trayectoria |
| `Collider` | radio y capa de colisión |
| `Health` | puntos de vida (enemigos y boss) |
| `Player` | vidas, bombas, nivel de disparo |
| `Weapon` | cadencia, patrón, temporizador |
| `Lifetime` | duración restante, para proyectiles |
| `Sprite` | referencia al recurso y animación actual |
| `ScoreValue` | puntos al destruirse |
| `Drop` | qué suelta al morir, si suelta algo |
| `Spawner` | qué genera y cada cuánto, para el transportador |
| `Shield` | escudo activo |
| `Invulnerable` | tiempo restante de gracia |
| `Attachment` | acoplamiento equipado y su durabilidad |
| `Pickup` | tipo de power-up recogible |

### Sistemas

Funciones sobre el mundo, ejecutadas **en orden fijo**. El orden es parte de las reglas del juego, no un detalle: cambiarlo cambia el comportamiento.

```
1  InputSystem        traduce el InputFrame en intención del jugador
2  MotionSystem       aplica velocidades y trayectorias
3  WeaponSystem       resuelve cadencias y crea proyectiles
4  SpawnSystem        avanza la línea de tiempo del nivel
5  LifetimeSystem     caduca proyectiles y efectos
6  CollisionSystem    detecta impactos y emite eventos de colisión
7  DamageSystem       aplica la prioridad defensiva
8  PickupSystem       resuelve power-ups y acoplamientos
9  ScoreSystem        acumula puntuación
10 CleanupSystem      destruye lo marcado y libera identificadores
```

`DamageSystem` es el único lugar donde vive la prioridad defensiva confirmada —invulnerabilidad → escudo → acoplamiento → vida— y donde se concede la invulnerabilidad tras cualquier daño. Concentrarla en un solo sistema es lo que la hace testeable y lo que evita que se disperse en condicionales por todo el código.

### Colisión

Por pares de capas, no todos contra todos:

```
proyectil del jugador  ×  enemigo
proyectil enemigo      ×  jugador
enemigo                ×  jugador
pickup                 ×  jugador
```

Comparación ingenua al principio. El benchmark de `collisionbench` midió que el escenario del MVP cuesta 0,028 ms, así que optimizar ahora sería trabajo sin causa. La rejilla uniforme ya está escrita y medida en el spike; si algún nivel avanzado la necesita, se introduce detrás de la misma interfaz sin tocar los sistemas.

## Eventos

Existen, pero con una frontera estricta:

- **Dentro de la simulación**, los sistemas se llaman directamente. Nada de eventos: un flujo de reglas que salta por un bus es imposible de seguir y de testear.
- **Hacia la presentación**, el core emite eventos y la capa `game` los consume. Audio, HUD, partículas y sacudidas de cámara se enganchan ahí.

```
core emite:  EnemyDestroyed, PlayerHit, PowerUpTaken, BombFired,
             AttachmentLost, BossPhaseStarted, LevelCleared
```

Así el core no sabe que existe el sonido, y añadir un efecto nuevo no toca ninguna regla del juego. Es el desacoplamiento que la especificación pedía, aplicado donde de verdad paga.

Los eventos se acumulan en una cola por tick y se drenan después del update. No hay callbacks reentrantes.

## Contenido en JSON

Los archivos viven en `assets/data/`. Los lee `game` con `JsonReader`/`JsonValue`, **nunca** con la clase `Json` de serialización automática: esa usa reflexión y en TeaVM obligaría a declarar cada clase a mano.

```
assets/data/
  enemies.json      arquetipos y sus componentes
  patterns.json     patrones de disparo
  trajectories.json trayectorias
  formations.json   formaciones
  level-01.json     línea de tiempo del nivel
  balance.json      valores de 10-valores-iniciales-mvp.md
```

Un enemigo es una lista de componentes, no una clase:

```json
{
  "id": "tank",
  "components": {
    "health":     { "points": 40 },
    "motion":     { "speed": 18, "trajectory": "slow-descent" },
    "weapon":     { "rate": 2.2, "pattern": "straight-single" },
    "collider":   { "radius": 7, "layer": "enemy" },
    "scoreValue": { "points": 500 }
  }
}
```

El cargador no sabe qué es un tanque: consulta un registro `nombre → fábrica de componente`. Al añadir un componente nuevo se registra una vez y **queda disponible desde JSON** sin tocar el cargador.

`core` no parsea nada. Define las interfaces de contenido y `game` se las entrega ya construidas. Por eso los tests pueden armar definiciones a mano sin leer un solo archivo.

### La línea de tiempo del nivel

Un nivel es una secuencia de eventos con marca de tiempo, que es la forma ejecutable de la curva de intensidad:

```json
{
  "events": [
    { "at": 8.0,  "spawn": "basic",  "formation": "line-3",   "atX": 0.5 },
    { "at": 12.0, "spawn": "light",  "formation": "diagonal", "atX": 0.2 },
    { "at": 45.0, "spawn": "tank",   "formation": "single",   "drop": "shield" },
    { "at": 95.0, "spawn": "heavy-encounter",                 "drop": "attachment" }
  ]
}
```

Los drops garantizados de `10-valores-iniciales-mvp.md` se expresan aquí, marcando la instancia concreta. Es lo que permite que un enemigo suelte algo sin que todos los de su tipo lo hagan.

## Inyección de dependencias

Manual, por constructor, con una única raíz de composición.

Sin Guice, Dagger ni Spring: los tres dependen de reflexión o de procesadores de anotaciones y añadirían riesgo con TeaVM a cambio de nada. En un proyecto de este tamaño, una clase que arma el grafo de objetos es más clara que un framework.

```java
// composition root, en game
var rng = new Rng(seed);
var content = contentLoader.load();
var world = new World(content, rng);
var systems = List.of(
    new InputSystem(), new MotionSystem(), /* ... */);
var loop = new GameLoop(world, systems);
```

Los sistemas reciben lo que necesitan y no consultan singletons. Eso es lo que permite instanciar cualquiera de ellos en un test con dependencias falsas.

## Pruebas

`core/src/test/java`, con JUnit 5. No necesitan libGDX, así que corren en segundos.

**Unitarias de sistemas.** Cada sistema con su mundo mínimo. Los casos que importan salen de las reglas ya decididas: la prioridad defensiva completa, la invulnerabilidad tras daño absorbido, el tope de vidas, el máximo de mejora de disparo, el power-up recogido al máximo que otorga puntos, el acoplamiento que absorbe un impacto y desaparece.

**Replays deterministas.** Un replay es una semilla más la secuencia de `InputFrame` por tick. El test la reproduce entera y compara el estado final contra el esperado.

```
core/src/test/resources/replays/
  nivel-01-victoria.replay
  nivel-01-derrota.replay
  acoplamiento-absorbe.replay
```

Detectan lo que las pruebas unitarias no ven: que dos sistemas correctos por separado interactúen mal. Y sirven de red al refactorizar, que es exactamente cuando más falta hace.

Si un replay falla tras un cambio deliberado de balance, se regenera. Un replay obsoleto no es un fallo, es un dato que caducó.

## Integración continua

GitHub Actions: compilar, pasar los tests de `core`, y construir los targets web y desktop.

Con una limitación que el spike dejó clara: **el runtime web no se puede validar en CI**, porque Chrome headless con SwiftShader falla aunque el navegador real funcione. CI verifica que el build web compila y produce artefactos; que ejecuta se comprueba a mano.

## Convenciones

- **Todo el código en inglés**: identificadores, comentarios, mensajes de log, nombres de archivos de contenido y claves JSON.
- Paquetes según las capas hexagonales: `core.domain.*`, `core.application`, `core.port`, `game.adapter.*`, `game.screens`.
- Los componentes no tienen lógica; los sistemas no tienen estado propio salvo el estrictamente necesario.
- Nada de singletons estáticos en `core`.

## Lo que NO se construye ahora

Está en la visión, no en el MVP, y construirlo antes de tiempo sería adivinar:

- perfiles, guardado y serialización del estado de la run;
- sistema de dificultad;
- hangar, tienda y economía;
- modos Supervivencia e Infinito;
- checkpoints;
- rejilla espacial para colisiones;
- pooling de objetos, hasta que un perfilado lo justifique;
- cualquier abstracción de plataforma más allá de los launchers.

El diseño debe permitir añadirlos sin reescribir los sistemas. Eso es distinto de dejarlos preparados.

## Orden de implementación sugerido

1. `core`: ECS, bucle, `Rng`, `InputFrame` y los tests que los cubren.
2. Movimiento, colisión y daño, con la prioridad defensiva completa.
3. `game`: launcher desktop, render mínimo y entrada. Primer momento jugable.
4. Carga de contenido JSON y arquetipos de enemigos.
5. Línea de tiempo del nivel y oleadas.
6. Power-ups, acoplamiento, bomba y puntuación.
7. HUD y pantallas.
8. Boss.
9. Audio y acabado audiovisual.
10. Target web, CI y despliegue.

Desktop primero no contradice la decisión de plataforma: es el camino más corto a tener algo jugable, y el core es el mismo. El target web se activa cuando hay juego que mostrar.
