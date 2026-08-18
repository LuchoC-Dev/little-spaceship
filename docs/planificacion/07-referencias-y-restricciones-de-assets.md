# Referencias históricas y restricciones de assets

## Relación con los proyectos antiguos

Los proyectos anteriores sirven como referencia emocional e histórica. No son base técnica ni repositorio de assets.

El nuevo proyecto:

- comienza desde cero;
- no copia arquitectura ni clases;
- no intenta reconstruir versiones perdidas;
- no está obligado a preservar reglas antiguas;
- puede mantener únicamente el ADN de shooter espacial arcade.

## Repo informado como V3

Repositorio: `https://github.com/LuchoC00/ProyectitoNavecita`

Lo inspeccionado durante la planificación:

- Java 17 con Maven.
- Ventana de 800×600.
- Título “Lost Galaxian - Grupo 3 - v1”.
- Dependencia externa `entorno.jar`.
- Imágenes de fondo.
- Sin lógica de juego ni entidades implementadas en lo publicado.
- README denominado “Proyecto jueguito 2.0”.
- La versión pública no coincide claramente con la numeración recordada.
- Sin historial visible suficiente para reconstruir las versiones 2–4.

Conclusión: no ofrece una base funcional aprovechable.

## Repo V1 jugable

Repositorio: `https://github.com/LuchoC00/Tp-Progra1`

Características observadas:

- Pantalla fija 800×600.
- Nave cerca del borde inferior y centrada.
- Movimiento horizontal con flechas.
- Inclinación visual al moverse.
- Asteroides desde arriba o laterales con trayectorias diagonales.
- Disparo vertical, pensado para un misil activo a la vez.
- Puntos al destruir asteroides.
- Meta de puntuación como condición de victoria.
- Barra de vida sobre la nave.
- Aura/escudo al mantener Shift.
- Clases o recursos previstos para enemigos y disparos enemigos que no estaban integrados.
- Varias llamadas importantes comentadas en el `tick` público.

Conclusión: sirve para entender el origen y el tono arcade, no para inferir un diseño final ni reutilizar código.

## Assets antiguos

No deben reutilizarse. Incluyen material asociado a Star Wars y un fondo atribuido a Lucasfilm, sin una base de licencia adecuada para una publicación nueva.

## Política de assets para el proyecto nuevo

- Priorizar pixel-art propio.
- Preferir assets CC0 cuando se usen recursos externos.
- Aceptar CC-BY u otras licencias compatibles solamente con atribución documentada.
- Registrar autor, fuente, licencia, versión y modificaciones de cada asset externo.
- Revisar licencias de fuentes, música, efectos, sprites, iconos y librerías por separado.
- No asumir que “gratis” significa permitido para redistribución o uso en portfolio.
- Mantener créditos incluso si todavía no existe una pantalla de créditos en el MVP.

## Referencia, no dependencia

El diseño futuro puede homenajear elementos generales —posición de la nave, amenaza descendente, escudo, score—, pero debe reconstruirlos como sistemas propios y coherentes con la nueva visión.

