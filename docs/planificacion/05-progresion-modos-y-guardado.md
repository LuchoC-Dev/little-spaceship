# Progresión, modos y guardado

## Campaña como recorrido principal

La campaña debe enseñar sistemas, presentar naves/acoplamientos y desbloquear modos. El jugador obtiene variedad básica simplemente progresando; los desafíos especiales se reservan para contenido raro o prestigioso.

## Tres perfiles

El juego completo tendrá **tres slots de perfil independientes**.

Cada slot guarda:

- progreso de campaña;
- niveles y etapas completados;
- naves desbloqueadas;
- acoplamientos desbloqueados;
- modos desbloqueados;
- contenido permanente de tienda;
- puntuaciones y récords;
- estado válido de continuación;
- posibles secretos/desafíos.

## Configuración global

Las preferencias deben compartirse entre los tres slots:

- volumen general;
- música;
- efectos;
- activación del mouse;
- keybindings cuando existan;
- fullscreen, resolución y accesibilidad futuras.

La motivación es que cambiar de perfil no debería modificar los controles o el volumen elegidos por la misma persona.

## Guardado

### Eventos confirmados

- Autosave al terminar un nivel o una etapa.
- Autosave al llegar a un punto seguro/hangar cuando corresponda.
- Botón **Guardar y salir**.
- Botón **Continuar** desde el último estado válido del slot.

### Guardar y salir durante un nivel

La continuación vuelve al **último checkpoint seguro**. No se conserva la posición exacta dentro de la acción, pero sí se recupera el estado que tenía el jugador al guardar la run: vidas, power-ups no consumidos, bombas, acoplamiento, moneda temporal y demás recursos pertinentes.

Si no existe un checkpoint intermedio, se vuelve al checkpoint de inicio del nivel o tramo manteniendo el snapshot del jugador guardado, salvo que el balance futuro determine excepciones concretas.

### Checkpoints

- Se usarán en niveles largos o difíciles.
- No se implementan en el MVP.
- El checkpoint define un punto seguro y un loadout inicial razonable **solamente para una nueva run iniciada desde allí**.
- Empezar desde una etapa avanzada puede entregar power-ups, bombas o moneda base según etapa/dificultad.

### Dos formas distintas de usar un checkpoint

**Continuar una run guardada:** recupera el snapshot del jugador en esa run y reanuda desde el checkpoint seguro correspondiente.

**Iniciar una run desde el checkpoint:** crea una run nueva con la configuración predeterminada del checkpoint, ajustada al tramo y la dificultad.

## Hangar

Normalmente aparece al terminar cada etapa; puede haber puntos intermedios limitados.

Permite:

- cambiar de nave;
- equipar acoplamientos desbloqueados;
- sincronizar contenido recién descubierto;
- usar moneda temporal de la run;
- guardar y preparar el siguiente tramo.

El hangar convierte los desbloqueos en decisiones de configuración sin permitir cambios arbitrarios en mitad del combate.

## Desbloqueo de modos

- **Supervivencia:** al completar la primera etapa, como recompensa temprana.
- **Infinito:** al completar toda la campaña, como recompensa final.
- Códigos/cheat codes: posibles para testing, easter eggs o desbloqueo especial, pero no sustituyen la progresión normal.

## Modo Supervivencia

### Identidad

Campaña infinita y cíclica, con niveles, transiciones y hangares.

### Ciclo

- Primer ciclo: escenarios/etapas 1–5.
- Segundo ciclo: vuelve visualmente a la etapa 1, pero con dificultad equivalente a etapas 6–10.
- Tercer ciclo: dificultad equivalente a 11–15.
- Continúa hasta perder.

No se reinician progreso ni dificultad al volver al escenario de la etapa 1.

### Variación

- Nuevas formaciones.
- Enemigos más avanzados.
- Patrones diferentes.
- Bosses cambiados o modificados.
- Mayor densidad, simultaneidad y presión.

### Métricas

- Ciclo/etapa alcanzada.
- Puntuación.
- Duración, si resulta útil.

### Modelo de daño

Se sugirió HP/desgaste acumulado, pero no está cerrado.

## Modo Infinito

### Identidad

Una única sesión continua, semejante a un nivel sin final. Enemigos, formaciones y bosses aparecen indefinidamente hasta que el jugador pierde todas sus vidas.

### Progresión

- Power-ups y módulos obtenidos durante la misma run.
- Dificultad creciente.
- Score y duración como métricas principales.

### Hangar

La inclusión de hangares quedó abierta. Podrían romper el ritmo continuo, pero quizá sean necesarios para la progresión de módulos.

### Modelo de daño

Se sugirió un sistema híbrido HP + vidas, todavía no confirmado.

## Meta-progresión y tienda

La tienda permanente no debe dominar el proyecto. Contenido considerado:

- skins;
- alguna nave especial;
- algún acoplamiento especial;
- contenido cosmético o de prestigio.

La moneda final está abierta. Usar score acumulado es simple, pero gastar un récord histórico sería confuso. Alternativas:

- score histórico no gastable + créditos derivados;
- moneda meta separada;
- recompensas por hitos/desafíos.

## Códigos

Los códigos pueden servir para:

- testing;
- desbloqueos internos;
- easter eggs;
- contenido promocional/especial;
- facilitar acceso en otra plataforma.

No deben reemplazar la progresión del jugador.
