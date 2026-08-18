# Registro de decisiones, contradicciones y pendientes

## Decisiones confirmadas

### Producto

- Plataforma: Java + libGDX + Gradle + gdx-teavm, publicando en navegador, con desktop compartiendo el core. Validado por el prototipo el 18/08/2026.
- Build tool: Gradle. Sustituye a Maven por ser el camino soportado por el stack.
- Target web de publicación: JavaScript, manteniendo Wasm disponible sin costo.
- El core es **single-thread** con bucle determinista. El multihilo se evaluó y se descartó: el target web no ofrece paralelismo real, y las mediciones muestran que no hay nada que ganar. Decisión cerrada, no reabrir sin un caso nuevo.
- Orden de optimización acordado: batching y atlas primero, estructuras espaciales para colisión después, concurrencia nunca.
- Arquitectura: ECS propio escrito a mano, sin librería. Contenido balanceable en JSON externo leído sin reflexión. Pruebas de sistemas puros más replays deterministas. Inyección de dependencias manual por constructor. Detalle en `12-arquitectura.md`.
- `core` es Java puro y no depende de libGDX; la presentación vive en `game`.
- Regla estricta de contratos: ningún módulo expone clases concretas a otro. Todo cruce de frontera pasa por una interfaz definida por el consumidor, y lo que cruza es inmutable o de solo lectura. Se verifica con un test de arquitectura.
- Todo el código se escribe en inglés, incluidos comentarios, logs, claves JSON e identificadores de contenido.
- Repositorio `little-spaceship`, paquete raíz `dev.luchoc.littlespaceship`, Java 17, Gradle con wrapper y JUnit 5.
- Arquitectura hexagonal, puertos y adaptadores, con la regla de dependencia hacia el dominio. De Clean se toma esa regla y la separación dominio/infraestructura, sin forzar casos de uso dentro del bucle de juego.
- Idioma: la planificación sigue en español; en implementación todo pasa a inglés y estos documentos se traducen.
- Juego nuevo, no remake.
- Shoot 'em up vertical completo por niveles.
- Single-player y local inicialmente.
- Java como lenguaje principal.
- Retro/pixel-art.
- Un nivel completo y publicable como MVP.
- Campaña como modo principal.
- 3–5 niveles por etapa y cinco etapas como visión de la primera campaña.
- Boss normalmente al final de cada nivel, con excepciones válidas.

### MVP

- Una nave básica.
- Disparo sostenido automático.
- Movimiento rápido y movimiento lento/preciso.
- Bomba como habilidad especial.
- Sistema de vidas; tres iniciales como valor provisional.
- Puntuación arcade sin moneda.
- Power-ups simples.
- Un único acoplamiento activo.
- Menú Jugar/Opciones/Salir.
- Teclado y mouse opcional.
- Pausa simple.
- Música de nivel y cambio al boss.
- Sin dificultad ni checkpoints.
- Densidad intermedia: ni bullet hell ni arcade puramente tradicional. Hitbox menor que el sprite, no puntual y no visible.
- Chocar contra un enemigo daña al jugador; los enemigos débiles se destruyen en el choque, los pesados no.
- Todo daño recibido otorga invulnerabilidad temporal, no solo la muerte; la del respawn dura más que la del daño absorbido.
- Escenario decorativo sin colisión, salvo unas pocas estructuras destruibles que sueltan recursos.
- El acoplamiento del nivel 1 lo entrega el encuentro fuerte previo al descanso.
- Teclado y mouse funcionan a la vez de forma aditiva: sus vectores de movimiento se suman y las direcciones opuestas se cancelan.
- El mouse es relativo, no posicional.
- Recoger un power-up ya al máximo otorga puntos en lugar de desperdiciarse.
- El MVP persiste las preferencias de audio y mouse, aunque no guarde progreso.
- El MVP incluye una pantalla mínima de créditos y licencias en Opciones.
- Escalado entero con nearest-neighbor y letterbox sobre una resolución lógica fija.
- Los valores numéricos de arranque viven en `10-valores-iniciales-mvp.md` y en configuración, no en el código.

### Campaña y progresión

- Desbloqueos de naves/acoplamientos permanentes.
- Gestión libre de lo desbloqueado en hangares.
- Moneda del hangar temporal a la run.
- Supervivencia desbloqueada tras la etapa 1.
- Infinito desbloqueado tras completar la campaña.
- Tres slots de perfil.
- Configuración de audio/controles global a los slots.
- Autosave al finalizar niveles/etapas y en puntos seguros.
- Botón Continuar.
- En dificultad normal, perder una vida no elimina automáticamente power-ups persistentes; cada uno se consume por su propia regla.
- Prioridad defensiva: invulnerabilidad → escudo → acoplamiento → vida.
- El acoplamiento se pierde al recibir daño y al perder una vida; absorbe ese impacto para evitar la pérdida de vida.
- La durabilidad del acoplamiento es un dato configurable por acoplamiento, no una constante en código.
- Los acoplamientos operan de forma automática o semi-automática.
- Power-ups y acoplamiento se conservan al pasar de nivel dentro de la misma run.
- Guardar y salir reanuda desde el último checkpoint seguro, no desde la posición exacta.
- Continuar recupera el estado guardado de la run; iniciar desde el checkpoint crea una run nueva con loadout predeterminado.
- El portfolio debe demostrar arquitectura, pruebas, CI, rendimiento, documentación, arte y despliegue.
- El repositorio permanece privado inicialmente; se evaluará hacerlo público al llegar al MVP o al producto final.

## Decisiones provisionales

- Tres vidas iniciales.
- Roster y orden aproximado de aparición del nivel 1.
- La nave básica mejora su disparo aumentando proyectiles.
- Power-ups controlados por el diseño del nivel.
- Hangar al finalizar cada etapa y reabastecimiento ocasional.
- Loadout mínimo al empezar desde checkpoints avanzados.
- HP para Supervivencia e híbrido HP + vidas para Infinito.

## Contradicciones resueltas

### Power-ups al perder una vida

La definición inicial de pérdida total fue reemplazada para la dificultad normal: los power-ups persistentes no desaparecen automáticamente al perder una vida. Cada power-up se elimina por su condición propia; por ejemplo, el escudo al absorber daño. Las dificultades superiores podrán imponer una pérdida mayor cuando se diseñen.

### Acoplamiento y daño

Resuelto: el acoplamiento desaparece al recibir daño y al perder una vida. Absorbe el impacto que lo destruye, evitando esa pérdida de vida, y se sitúa después del escudo en la prioridad defensiva. Se conserva al pasar de nivel dentro de la misma run.

Durabilidad: por defecto igual para todos los acoplamientos, pero modelada como dato configurable por acoplamiento y no como constante en código, para admitir más adelante un acoplamiento de protección más resistente.

### Build tool

Maven fue una decisión inicial y preferencia del usuario. Gradle pasó a ser la recomendación para libGDX + gdx-teavm.

Resuelto por el prototipo, como estaba previsto: **Gradle**. El plugin de gdx-teavm es un plugin de Gradle que resuelve backend, assets, `index.html` y servidor local, y genera las tareas de JS y Wasm. Reproducirlo con Maven sería integración manual sin ganancia.

## Pendientes de gameplay

- Regla de pérdida de power-ups en dificultades superiores y al terminar la run.
- Comportamiento exacto con varios acoplamientos simultáneos post-MVP: no debería generar conflicto por ser automáticos, pero hay que verlo con gameplay real.
- Si el boss del nivel 1 tendrá fases y qué patrones/estética usará.
- Herramienta/formato de curva de intensidad.

## Pendientes de campaña y narrativa

- Nombre del juego, mundo, facciones y personajes.
- Lugar exacto que se defiende en el nivel 1.
- Formato de presentación entre etapas.
- Detalle de bosses y sub-bosses.
- Frecuencia y reglas de niveles multi-boss.
- Cantidad final de niveles por etapa.
- Narrativa exacta de la supernave y la entidad de la transición 3→4.
- Alcance real de una posible segunda campaña.

## Pendientes de progresión y guardado

- Campos técnicos exactos y formato de serialización del snapshot de la run.
- Reglas de restauración ante guardado corrupto o versión incompatible.
- Nombres y cantidades de la moneda temporal.
- Moneda/sistema de la tienda permanente.
- Qué estadísticas y récords se guardan por perfil.
- Si se puede copiar, renombrar o borrar un slot.
- Tratamiento del save-scumming.
- Hangares en modo Infinito.
- Diseño final de HP/vidas por modo.
- Reglas y formato de cheat codes.

## Pendientes técnicos a verificar

- Compatibilidad real en Firefox, Edge y Safari; Chrome ya verificado.
- Captura del puntero para el mouse relativo.
- Medición con arte y audio definitivos; el spike genera sus texturas por código.
- Compatibilidad de dependencias Java.
- Hosting final.
- Estrategia de testing, CI y despliegue.
- Política de redimensionado del canvas web: el backend necesita tamaño explícito.

## Preguntas originales todavía sin respuesta explícita

- ¿Qué diferencia concreta debe verse respecto del juego viejo: código, acabado, profundidad, publicación o todo?
- ¿Existe una fecha objetivo para el MVP?
- ¿Gamepad será requisito post-MVP?
- ¿Se espera soporte móvil/táctil alguna vez?

## Tareas del arranque de implementación

- Traducir al inglés los documentos de `docs/planificacion/`, con cuidado de preservar las distinciones finas: confirmado contra provisional, la prioridad defensiva, y las reglas de run y checkpoint.
- A partir de ese punto, toda documentación nueva —ADR incluidos— se escribe directamente en inglés.

## Orden recomendado para resolver pendientes

1. Confirmar/corregir este paquete funcional.
2. Resolver las reglas que afectan directamente al MVP.
3. Ejecutar el prototipo técnico web/desktop.
4. Decidir stack y build tool.
5. Definir arquitectura y estrategia de datos/configuración.
6. Crear backlog y comenzar implementación.
