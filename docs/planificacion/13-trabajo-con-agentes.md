# Trabajo con agentes

Definido el 19/08/2026, antes de escribir la primera línea de código del proyecto real.

## El modelo

Un **jefe** que no ejecuta el trabajo fino, sino que decide, planifica y reparte. Debajo, agentes especializados que hacen el trabajo dentro de una frontera clara.

El jefe es la sesión principal de Claude, no un subagente. Tiene el contexto de toda la planificación —las mediciones, las trampas encontradas, por qué se descartó cada alternativa— y ese contexto se perdería al delegarlo a un agente que arranca en frío.

## Memoria: cómo se conserva el contexto

Distinguimos dos cosas:

**La memoria viva.** Un agente lanzado conserva su contexto mientras la sesión sigue abierta y se le puede seguir mandando trabajo sin repetirle nada.

**La memoria persistente.** Cada agente tiene un directorio propio en `.claude/agent-memory/<agente>/`, declarado con `memory: project` en su definición. Al arrancar cualquier instancia nueva, el harness le carga automáticamente su `MEMORY.md`. Así una instancia lanzada mañana sabe lo que aprendió la de hoy.

Esa memoria **no se carga en la conversación principal**: es exclusiva del agente, y por eso no infla el contexto del jefe.

Como está bajo `.claude/agent-memory/` y no en `agent-memory-local/`, va a git: el conocimiento acumulado es parte del repositorio.

### Qué se guarda y qué no

Regla que evita el problema más común: **la memoria de un agente no repite lo que ya está en `docs/`**.

Guarda solo lo que ese agente descubrió y no está escrito en otro lado: dónde vive cierto código, qué trampa le costó una hora, qué decisión propia tomó y por qué. Si duplicamos la especificación en cinco memorias, en dos semanas hay seis fuentes de verdad que se contradicen.

Se escribe **al terminar una tarea**, no continuamente.

## Roster

| Agente | Rol | Escribe en |
|---|---|---|
| jefe (sesión principal) | decide, planifica, delega, revisa | todo |
| `core-domain` | reglas, ECS, sistemas | `core/` |
| `game-presentation` | render, HUD, pantallas, audio, entrada | `game/`, `desktop/`, `web/` |
| `visual-designer` | dirección visual y specs | documentos |
| `test-engineer` | pruebas unitarias y replays | pruebas |
| `reviewer` | auditoría | nada |

### Por qué estas fronteras

No son arbitrarias: **salen de la arquitectura**. Como los módulos ya tienen dependencias en un solo sentido, la propiedad de archivos se reparte sola y dos agentes no pueden pisarse.

`core-domain` y `game-presentation` son la separación back/front que ya impone la arquitectura hexagonal, aplicada a quién trabaja en cada lado.

Las fronteras no dependen solo de la buena voluntad: `reviewer` no tiene herramientas de escritura, así que no puede modificar nada aunque quisiera.

### Por qué no hay agente de contenido

Se consideró y se descartó. El contenido —JSON de balance, algún sprite— se toca poco y con cambios pequeños. Un agente dedicado a eso sería burocracia: lo hace quien esté trabajando en ese momento.

## Cómo se reparte una tarea

1. El jefe decide qué hay que hacer y contra qué documentos se valida.
2. Escribe un plan concreto para el agente que corresponda, con el objetivo, la frontera y las invariantes en juego.
3. El agente consulta su memoria, ejecuta y guarda lo aprendido.
4. El jefe revisa el resultado y, si el trabajo lo justifica, lo pasa por `reviewer`.

El jefe **no lanza agentes por defecto**. Delegar cuesta: cada agente arranca en frío y vuelve a leer contexto que el jefe ya tiene. Se delega cuando la tarea es grande y aislada, o cuando conviene que su salida no ocupe el contexto principal.

## Trabajo en paralelo

Cuando dos agentes tengan que trabajar a la vez, se usa `isolation: worktree`: cada uno recibe su propio worktree de git y no pisan los archivos del otro.

Aun así, la primera defensa sigue siendo la frontera de módulos.

## Idioma

Las definiciones de los agentes están **en inglés**, como todo lo que vive en el repositorio, y lo mismo vale para lo que producen: código, comentarios y logs.

El español queda para dos cosas: la conversación con el usuario, y los documentos de `docs/planificacion/` mientras dure esta etapa. Los agentes leen esos documentos en español y trabajan en inglés; sus definiciones lo advierten.
