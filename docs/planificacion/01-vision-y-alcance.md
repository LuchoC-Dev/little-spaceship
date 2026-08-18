# Visión y alcance del producto

## Identidad

El juego será un **shoot 'em up vertical completo, estructurado por niveles**, con movimiento libre de la nave dentro del área jugable, escenario en desplazamiento, enemigos organizados mediante oleadas y patrones, y normalmente un boss al final de cada nivel.

La inspiración nace de un juego espacial realizado por el autor cuando comenzaba a programar. El homenaje consiste en volver a Java y a la fantasía del shooter espacial; no en conservar mecánicas, estética, contenido o arquitectura de aquel proyecto.

El ADN que sí se conserva es:

- una nave controlada por el jugador;
- amenazas que llegan principalmente desde arriba;
- esquivar y posicionarse;
- disparar y destruir enemigos;
- una sensación arcade acompañada por puntuación.

## Objetivo del proyecto

El producto debe ser:

- funcional y jugable;
- publicable y fácil de mostrar;
- adecuado para portfolio;
- representativo de varios años de crecimiento técnico;
- suficientemente extensible para agregar campaña, modos, naves y contenido sin reconstruir los sistemas centrales.

El portfolio debe demostrar el conjunto completo del trabajo: arquitectura modular, diseño de sistemas, testing, integración continua, rendimiento, documentación, acabado artístico, publicación y despliegue.

El propósito no es “practicar POO”. Se usará Java y habrá objetos donde corresponda, pero el énfasis estará en diseño de sistemas, arquitectura modular, acabado, pruebas y capacidad de terminar y publicar el producto.

## Alcance general confirmado

- Proyecto creado 100 % desde cero.
- Single-player y local como alcance inicial.
- Java como lenguaje principal.
- Estética retro/pixel-art.
- Dependencias permitidas solamente con licencias compatibles.
- No se desarrollará un framework de UI propio.
- No se reutilizará código ni assets antiguos.
- El modo principal será una campaña por niveles.
- Los modos Infinito y Supervivencia pertenecerán a etapas posteriores.

## Fantasía jugable

El jugador pilota inicialmente una nave experimental humana activada durante la primera oleada de una invasión alienígena. La campaña comienza defendiendo la Tierra, escala hacia la órbita y la Luna, revela una fuerza alienígena más orgánica y peligrosa, y culmina con una última defensa terrestre.

El combate debe combinar:

- lectura de patrones y trayectorias;
- movimiento rápido para reposicionarse;
- movimiento lento para esquivar con precisión;
- destrucción priorizada de amenazas;
- conservación de vidas, power-ups y acoplamientos;
- uso estratégico de bombas/habilidades especiales;
- búsqueda de puntuación.

No se definió el juego como bullet hell puro. Puede incorporar densidad de proyectiles y precisión, pero también importan matar enemigos a tiempo, reconocer prioridades, manejar recursos y adaptarse a formaciones.

## Escalas de entrega

### MVP

Un nivel completo y publicable con los sistemas indispensables, una única nave y un boss sencillo. Debe demostrar el loop, presentación audiovisual y capacidad técnica del producto.

### Post-MVP cercano

Completar la primera etapa de 3–5 niveles, añadir hangares, más naves y acoplamientos, desbloqueos, perfiles y guardado, y preparar el modo Supervivencia.

### Visión completa

Campaña de cinco etapas y aproximadamente 15–25 niveles, múltiples naves con estilos propios, acoplamientos, meta-progresión, tienda limitada, bosses variados, tres perfiles, Supervivencia, Infinito y posible continuación narrativa fuera de la Tierra.

## Principios de diseño

### Identidad por comportamiento

Las naves y enemigos deben diferenciarse por cómo se juegan, no solo por HP, velocidad o daño. Una nave puede usar disparo sostenido, otra disparo manual, otra carga; algunas pueden privilegiar movilidad, cadencia, daño o habilidades especiales.

### Composición de contenido

Tipo de enemigo, trayectoria, patrón de disparo, formación y momento de aparición son conceptos separables. Un enemigo básico no queda atado para siempre a una trayectoria fija.

### Progresión contextual

Los acoplamientos deben aparecer cuando el nivel permite comprender su utilidad. Por ejemplo, contramedidas en una etapa con muchos proyectiles o misiles frente a blancos para los que resulten especialmente útiles.

### Dificultad mediante presión

La dificultad no debe depender solo de subir vida y daño. Puede aumentar mediante densidad, velocidad, combinaciones, entradas, patrones, obstáculos, espacio disponible y presión simultánea.

### Contenido abierto a iteración

No se fijarán ahora todas las naves, bosses, patrones o estadísticas. El diseño debe permitir cambiar y balancear esos elementos conforme exista gameplay real.

## Restricciones de proceso

La secuencia acordada es:

1. consolidar y revisar la especificación funcional;
2. validar de forma práctica la opción web con Java;
3. decidir plataforma y stack;
4. diseñar arquitectura;
5. dividir tareas;
6. implementar el MVP.

Todavía no se debe considerar cerrada la arquitectura ni la plataforma.

## Visibilidad del proyecto

El repositorio será privado durante la etapa inicial. Al alcanzar el MVP se evaluará si conviene hacerlo público en ese momento o esperar al cierre del producto. No existe todavía una fecha objetivo confirmada.
