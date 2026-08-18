# Especificación funcional del MVP

## Objetivo

Entregar un único nivel completamente jugable, pulido y publicable, capaz de mostrar el loop central, la presentación audiovisual y la dirección futura del producto.

El MVP no es la campaña completa. Es una porción vertical: todos los sistemas necesarios para jugar y terminar el nivel deben funcionar, aunque su contenido sea mínimo.

## Flujo completo

1. Inicio del juego.
2. Menú principal.
3. Pantalla de selección/construcción de nave.
4. Selección de la única nave básica disponible.
5. Nivel 1.
6. Victoria al derrotar al boss conservando al menos una vida, o derrota al perder todas las vidas.
7. Pantalla correspondiente con opciones simples.

## Menú y pantallas

### Menú principal

- Jugar.
- Opciones.
- Salir.

No se mostrarán modos futuros bloqueados ni botones “próximamente”.

### Selección/construcción de nave

- Una nave básica seleccionable.
- Presentación de sus características principales.
- La pantalla debe quedar conceptualmente preparada para sumar naves.
- Es opcional mostrar siluetas o espacios futuros; no habrá desbloqueo, compra ni personalización profunda en el MVP.

### Opciones

- Volumen general.
- Volumen de música.
- Volumen de efectos.
- Activar/desactivar control con mouse.
- Sin reasignación de teclas.

### Pausa

- Símbolo o botón simple.
- Congela gameplay, enemigos, proyectiles, animaciones relevantes y temporizadores.
- Sin menú de pausa completo.

### Derrota

- Reintentar el nivel.
- Volver al menú.

### Victoria

- Animación o pantalla breve.
- Volver al menú.
- Reintentar es opcional, no requisito.

## Nave básica

### Movimiento

- Movimiento libre en dos ejes dentro del área jugable.
- Velocidad normal rápida para reposicionarse.
- Tecla de movimiento lento/preciso.
- Sin dash ni maniobra de esquiva especial.

### Ataque

- Disparo principal automático/sostenido.
- El power-up de disparo aumenta la cantidad de proyectiles de la nave básica.
- El nivel de mejora debe reconocerse por la forma, cantidad o tamaño del disparo, sin exigir un indicador numérico.
- Debe existir un máximo configurable; el valor de arranque está en `10-valores-iniciales-mvp.md`.

### Hitbox e identidad de combate

- El juego es un shoot 'em up de densidad intermedia, no un bullet hell ni un arcade puramente tradicional.
- La hitbox del jugador es menor que su sprite, pero no puntual, y no se muestra en pantalla.
- La densidad de proyectiles es moderada: se esquiva leyendo trayectorias y posicionándose, con el movimiento lento como herramienta de precisión en los momentos exigentes.
- La legibilidad manda sobre la cantidad: el jugador siempre debe poder distinguir qué le puede pegar.

### Ataque especial

- Bomba que elimina la mayoría de amenazas/proyectiles en pantalla y causa mucho daño a enemigos resistentes.
- Funciona como recurso ofensivo y de emergencia.
- La cantidad inicial depende del diseño de la nave. Valores de arranque en `10-valores-iniciales-mvp.md`.

### Supervivencia

- Sistema arcade de vidas.
- Tres vidas iniciales, con un máximo alcanzable mediante el power-up de vida extra.
- Al perder una vida, la nave reaparece cerca de la zona donde fue destruida.
- Existe invulnerabilidad temporal después del respawn.
- Cualquier daño recibido otorga invulnerabilidad temporal, no solo la muerte. Perder el escudo o el acoplamiento también concede esos fotogramas de gracia, evitando encadenar varios impactos en un instante.
- Los fotogramas de gracia por daño absorbido son más breves que los del respawn; ambos valores son configurables.
- En dificultad normal, perder una vida **no elimina automáticamente los power-ups persistentes**.
- Cada power-up conserva su propia condición de consumo: por ejemplo, el escudo puede perderse al absorber daño y la invulnerabilidad termina por tiempo.
- En dificultades superiores puede aplicarse una regla más castigadora; todavía debe definirse cuando exista el sistema de dificultad.
- El orden defensivo confirmado es: **invulnerabilidad → escudo → acoplamiento → vida**.

### Colisión con enemigos

- Chocar contra un enemigo daña al jugador y consume la capa defensiva que corresponda según el orden confirmado.
- Los enemigos débiles —básicos, ligeros y rápidos— se destruyen en ese choque; tanques y transportadores pesados no.
- El impacto activa la invulnerabilidad temporal por daño.
- Esta regla deja preparado el arquetipo de enemigo embestidor previsto para etapas posteriores.

## Controles

### Teclado

- Flechas para movimiento.
- Una tecla para disparar.
- Una tecla para movimiento lento/preciso.
- Una tecla para bomba/habilidad especial.

### Mouse opcional

- Mover la nave.
- Disparar.
- Lanzar la habilidad especial.
- Activable o desactivable desde Opciones.
- Teclado y mouse funcionan simultáneamente y de forma aditiva cuando el mouse está activado: ambos aportan un vector de movimiento y esos vectores se suman, de modo que direcciones opuestas se cancelan.
- El mouse es relativo, no posicional: mueve la nave por desplazamiento del cursor, no la lleva al punto del puntero.

No se incluye gamepad ni control táctil móvil en el alcance confirmado.

## Power-ups del MVP

- Mejora de disparo.
- Escudo.
- Vida extra poco frecuente.
- Recuperación de munición/carga de bomba.
- Invulnerabilidad temporal.

Los obstáculos del escenario son decorativos en el MVP y no colisionan; solo las estructuras destruibles interactúan con el gameplay.

Los drops se controlan desde el diseño del nivel. Un tipo de enemigo no suelta siempre lo mismo: un ejemplar específico de una oleada puede estar marcado para entregar una mejora. También pueden existir estructuras destruibles que contengan recursos.

## Acoplamientos del MVP

- Un solo acoplamiento activo a la vez. El MVP no entrega un segundo, así que ese caso no se da.
- Añade una capacidad nueva, no un simple aumento numérico.
- Ejemplos contemplados: misiles, láser o contramedidas.
- Debe ser más raro que un power-up.
- En el nivel 1 lo entrega el encuentro fuerte previo al descanso, de modo que llega justo antes de la escalada final y el boss.
- En niveles futuros puede provenir de sub-bosses, unidades excepcionales, estructuras/bases o eventos diseñados.
- Absorbe un impacto y se destruye antes de que se pierda una vida.
- También se pierde al perder una vida.
- Orden de absorción confirmado: invulnerabilidad → escudo → acoplamiento → vida.

## Nivel 1

### Contexto narrativo

La Tierra sufre la primera gran oleada alienígena. Fuerzas actuales intentan defender una ciudad, base o lugar importante, pero su tecnología es insuficiente. Una nave experimental sale desde una base secreta para contener el ataque.

### Ritmo macro

1. Animación breve de salida/despegue.
2. Entre 5 y 10 segundos de recorrido tranquilo.
3. Eventos ambientales de fondo: ataques, meteoritos, aviones humanos, defensas y destrucción.
4. Aparición de enemigos básicos.
5. Introducción progresiva de formaciones y nuevos arquetipos.
6. Escalada de presión.
7. Amenaza o encuentro fuerte previo al final.
8. Descanso de 5–10 segundos.
9. Nueva escalada más rápida.
10. Boss.
11. Victoria o derrota.

La secuencia es provisional y se ajustará con una curva de intensidad, no como una lista rígida.

### Roster mínimo de enemigos

- Básico: débil, poca vida y disparo lento.
- Rápido ligero: movilidad alta y disparo simple/diferente.
- Básico evolucionado o tirador: similar al básico con mayor cadencia.
- Súper rápido: amenaza principal por movimiento; dispara poco.
- Tanque: lento y resistente.
- Transportador pesado: muy lento, mucha vida, no dispara y genera enemigos básicos cada cierto tiempo.
- Boss sencillo, legible y apropiado para un primer nivel; patrones y estética todavía abiertos.

Los enemigos del nivel 1 usan patrones relativamente legibles, pero sus trayectorias y formaciones deben poder reutilizarse o combinarse con otros tipos en niveles futuros.

## Puntuación

- Sistema arcade sencillo.
- Se obtienen puntos principalmente al destruir enemigos y completar el nivel.
- Se muestra la puntuación actual.
- No funciona como moneda en el MVP.
- Sin combos, multiplicadores ni economía persistente confirmados.

## HUD

- Vidas restantes.
- Cargas de bomba/habilidad especial.
- Puntuación actual.
- Estado de power-ups cuando corresponda.
- Acoplamiento equipado, si existe.
- Estado de invulnerabilidad comunicado visualmente.
- Vida del boss únicamente durante su combate.
- Feedback claro de impacto y pérdida de mejoras.

No se incluyen minimapa, contador de enemigos, estadísticas detalladas ni barra permanente de progreso del nivel.

## Presentación audiovisual

- Pixel-art con una dirección visual cercana a la definitiva.
- Animaciones de movimiento, disparo, aparición, impacto, explosión, bomba, pérdida de vida, pickups, victoria y derrota.
- Efectos de sonido para disparos, impactos, explosiones, power-ups, bomba y UI.
- Música principal del nivel.
- Cambio de música al comenzar el boss.
- Cambios dinámicos durante otros picos de dificultad son pulido opcional.

## Exclusiones explícitas del MVP

- Selector o sistema de dificultad.
- Checkpoints.
- Perfiles y slots de guardado.
- Guardar y salir / Continuar.
- Hangar funcional y economía.
- Tienda permanente.
- Desbloqueos reales.
- Más de una nave jugable.
- Campaña completa.
- Modos Supervivencia e Infinito.
- Reasignación de teclas.
- Dash.
- Gamepad y controles táctiles.

Los sistemas deben evitar quedar rígidamente acoplados al único contenido del MVP, pero no se implementarán funciones futuras sin uso real.

## Criterios funcionales de aceptación

- El jugador puede recorrer el flujo completo sin herramientas de desarrollo.
- El nivel puede ganarse y perderse.
- Pausa, opciones y reinicio funcionan correctamente.
- El boss marca un clímax diferenciado con música y HUD propios.
- Los controles son legibles y el modo preciso permite esquivar.
- El nivel introduce arquetipos y luego los combina.
- Los power-ups, puntuación y acoplamiento se comunican correctamente.
- El apartado audiovisual no depende exclusivamente de placeholders.
- El build elegido puede publicarse y ejecutarse de forma reproducible.
