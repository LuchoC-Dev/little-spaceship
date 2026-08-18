# Valores iniciales y decisiones operativas del MVP

Este documento reúne los valores de arranque necesarios para construir el MVP. **Ninguno es definitivo**: son puntos de partida elegidos para poder implementar y empezar a jugar. Todos deben vivir en configuración, no incrustados en el código, porque su propósito es cambiar durante el balance.

Cuando un valor cambie tras playtesting, se actualiza aquí.

## Nave y recursos

| Concepto | Valor inicial | Nota |
|---|---|---|
| Vidas iniciales | 3 | Ya confirmado en la especificación. |
| Máximo de vidas | 5 | Evita acumular vidas hasta volver trivial la tensión. |
| Bombas iniciales | 2 | Suficientes para usarlas sin atesorarlas. |
| Máximo de bombas | 3 | Valor de la nave básica; cada nave define el suyo. |
| Niveles de disparo | 4 | Base + 3 mejoras, distinguibles por forma y cantidad. |

### Invulnerabilidad

| Situación | Duración inicial |
|---|---|
| Tras respawn | 2,0 s |
| Tras daño absorbido por escudo o acoplamiento | 1,0 s |

La invulnerabilidad debe comunicarse visualmente en ambos casos, aunque el parpadeo del respawn puede ser más marcado.

### Recoger un power-up ya al máximo

El pickup **no se desperdicia**: se convierte en puntos. Evita el drop muerto y mantiene el incentivo de recoger todo. El bonus inicial propuesto es de 500 puntos.

## Controles

Cuando el mouse está activado en Opciones, **teclado y mouse funcionan simultáneamente y de forma aditiva**. No hay dispositivo prioritario ni conmutación entre uno y otro.

Ambos producen un **vector de movimiento** por fotograma y esos vectores **se suman**. Si el mouse empuja a la derecha y el teclado a la izquierda con la misma intensidad, la resultante es cero y la nave no se mueve: se cancelan. La resultante se limita a la velocidad máxima de la nave, para que combinar los dos dispositivos nunca permita ir más rápido que usar uno solo.

Esto obliga a una decisión concreta: el mouse es **relativo**, no posicional. Aporta el desplazamiento del cursor entre fotogramas, en lugar de teletransportar la nave a la posición del puntero. Es la única forma de que sumar y cancelar tenga sentido.

Disparo y bomba no tienen conflicto: cualquiera de los dos dispositivos los activa.

### Consecuencia técnica a validar

Un mouse relativo necesita capturar el puntero —Pointer Lock en navegador— porque si no, el cursor llega al borde de la ventana y deja de generar desplazamiento aunque el jugador siga moviéndolo. Pointer Lock exige un click previo del usuario y oculta el cursor del sistema.

Esto entra en el prototipo técnico, que ya tenía prevista la validación de input.

## Presentación

### Resolución y escalado

La política, más importante que el número concreto:

- Resolución lógica fija, independiente del tamaño de la ventana.
- Escalado **entero** (×2, ×3, ×4) para que el pixel-art nunca se deforme.
- Filtrado **nearest-neighbor**, sin suavizado.
- El espacio sobrante se resuelve con letterbox, no estirando la imagen.

Punto de partida propuesto: **480×270 lógicos** (escala entera exacta a 1920×1080), con el campo de juego vertical centrado —alrededor de 200-220 px de ancho— y el HUD ocupando los márgenes laterales, como es habitual en un shoot 'em up vertical mostrado en una pantalla apaisada.

El valor definitivo se fija durante el prototipo técnico, que ya incluye esta validación, y en coordinación con el tamaño real de los sprites.

### Créditos

El MVP incluye una pantalla mínima de créditos y licencias, accesible desde Opciones. Es barata de construir y necesaria en cuanto se use cualquier asset externo con atribución requerida.

## Persistencia en el MVP

El MVP **no** guarda progreso: no hay perfiles, checkpoints ni continuación.

Sí guarda las **preferencias**: volumen general, música, efectos y activación del mouse. Perder el volumen elegido en cada arranque se siente como un defecto, y el costo es una única entrada de configuración. Esto es coherente con la decisión de que la configuración sea global y no pertenezca a ningún perfil.

## Ritmo del nivel 1

| Tramo | Duración objetivo |
|---|---|
| Introducción y calma inicial | 5-10 s |
| Cuerpo del nivel hasta el encuentro fuerte | 3-4 min |
| Descanso | 5-10 s |
| Escalada final | 45-60 s |
| Boss | 60-90 s |
| **Total** | **5-6 min** |

## Drops garantizados

Para que el MVP se sienta diseñado y no aleatorio, el nivel 1 asegura:

- una mejora de disparo en el primer tercio, para que el jugador entienda el sistema temprano;
- un escudo antes del encuentro fuerte;
- el acoplamiento al derrotar el encuentro fuerte;
- una recarga de bomba antes del boss.

El resto de los drops se coloca en el diseño de oleadas según convenga al ritmo.

## Puntuación

Valores base de partida:

| Fuente | Puntos |
|---|---|
| Enemigo básico | 100 |
| Ligero rápido | 150 |
| Básico evolucionado | 200 |
| Súper rápido | 250 |
| Tanque | 500 |
| Transportador pesado | 1000 |
| Estructura destruible | 300 |
| Boss | 5000 |
| Power-up recogido al máximo | 500 |

Al completar el nivel se suma un bonus por vidas y bombas restantes —1000 y 300 respectivamente— para premiar terminar en buenas condiciones sin introducir combos ni multiplicadores, que quedan fuera del MVP.
