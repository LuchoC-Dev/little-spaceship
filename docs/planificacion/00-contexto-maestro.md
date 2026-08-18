# Contexto maestro — juego espacial

## Propósito de este paquete

Este paquete consolida la etapa inicial de descubrimiento y planificación del proyecto. La fuente es la conversación completa **“Planificación juego espacial”** (`6a8328f6-a57c-83e9-80f8-c07edb4191dd`), desde el primer turno `88a1854a-0636-4737-bc11-9d995c2895bb` hasta el último turno `a2bce64f-3161-4d77-9376-65c2aef70132`.

No es todavía una arquitectura cerrada ni un plan de implementación. Distingue deliberadamente entre:

- **Confirmado:** decisión expresada y sostenida durante la conversación.
- **Provisional:** dirección aceptada para poder avanzar, pero sujeta a prueba o balance.
- **Abierto:** alternativa todavía no elegida o aspecto no definido.
- **Fuera del MVP:** parte de la visión, pero no de la primera entrega publicable.

## Resumen ejecutivo

El proyecto —repositorio **`little-spaceship`**— será un **shoot 'em up vertical por niveles**, single-player y local, desarrollado desde cero. Retoma el ADN emocional de un juego espacial antiguo del autor —nave, amenazas desde arriba, esquivar, disparar y puntuación—, pero no reutiliza su código, arquitectura ni assets.

El objetivo es crear un producto funcional, publicable y presentable en portfolio que refleje el nivel actual del autor mediante arquitectura, pruebas, CI, rendimiento, documentación, arte y despliegue. Java seguirá siendo el lenguaje principal. La plataforma quedó decidida tras el prototipo técnico: **Java 17 + libGDX + Gradle + gdx-teavm**, publicando en navegador con JavaScript y con un target desktop que comparte el mismo core. El repositorio permanecerá privado durante el desarrollo inicial; su apertura se evaluará al llegar al MVP o al producto final.

El MVP será una experiencia pequeña pero terminada: menú, opciones, selección de una nave básica, un nivel completo en la Tierra, enemigos y oleadas, power-ups, un acoplamiento, puntuación, HUD, boss, audio, animaciones y pantallas de victoria/derrota. No incluirá perfiles, guardado, checkpoints, tienda, dificultades ni modos alternativos.

La visión completa contempla una campaña de cinco etapas, 3–5 niveles por etapa, hangares, desbloqueos permanentes, tres slots de perfil, autosave, modos Supervivencia e Infinito, meta-progresión y una posible segunda campaña ofensiva.

## Orden de lectura recomendado

1. `01-vision-y-alcance.md`: identidad del producto y fronteras de alcance.
2. `02-especificacion-funcional-mvp.md`: qué debe contener la primera entrega.
3. `03-sistemas-de-juego.md`: reglas jugables y estados persistentes/temporales.
4. `04-campana-narrativa-y-niveles.md`: estructura narrativa y diseño de niveles.
5. `05-progresion-modos-y-guardado.md`: campaña extendida, perfiles y modos futuros.
6. `06-plataforma-y-validacion-tecnica.md`: razonamiento previo a la validación de plataforma.
7. `07-referencias-y-restricciones-de-assets.md`: relación con proyectos antiguos y licencias.
8. `08-registro-de-decisiones-y-pendientes.md`: dudas, contradicciones y verificaciones pendientes.
9. `09-mapa-de-fuentes.md`: trazabilidad hacia los turnos originales.
10. `10-valores-iniciales-mvp.md`: valores de arranque y decisiones operativas para construir el MVP.
11. `11-resultado-prototipo-tecnico.md`: resultado del spike y decisión de plataforma.
12. `12-arquitectura.md`: estructura del proyecto, ECS, contenido y pruebas.

## Idioma

Los documentos de esta etapa de planificación están en **español**, y así se quedan mientras dure.

Al pasar a implementación cambia la política: **todo el código y toda la documentación nueva se escriben en inglés** —ADR incluidos—, y estos documentos de planificación se traducen al inglés como parte del arranque de esa etapa.

La única excepción permanente es la conversación con agentes, que sigue en español.

## Principios rectores

- Primero especificar la experiencia; luego validar plataforma; después definir arquitectura; finalmente implementar.
- El juego nuevo no es un remake ni una restauración técnica.
- La variedad debe surgir de sistemas combinables —naves, patrones, trayectorias, formaciones, acoplamientos— y no solamente de aumentar estadísticas.
- El MVP debe sentirse publicable, no como una demo de cajas y placeholders.
- La campaña es el recorrido principal y debe enseñar/desbloquear el resto del juego.
- Web es deseable por la facilidad de abrir un enlace y jugar, pero no debe deformar el diseño ni imponer una complejidad desproporcionada.

## Fuente y trazabilidad

La conversación fuente incluye decisiones funcionales, propuestas del asistente y puntos que el usuario dejó deliberadamente abiertos. Cuando una propuesta del asistente no fue confirmada de manera explícita, este paquete la conserva como recomendación o alternativa, no como decisión definitiva.
