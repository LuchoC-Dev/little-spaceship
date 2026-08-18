# Sistemas de juego

## Modelo de partida y run

Una **run** comienza cuando el jugador inicia desde un punto de guardado, hangar o checkpoint de campaña y continúa mientras conserve al menos una vida. Puede atravesar varios niveles.

Cuando pierde todas las vidas:

- termina la run;
- se pierde el estado temporal correspondiente;
- reiniciar desde el último punto seguro inicia una nueva run.

Una run no significa necesariamente jugar toda la campaña desde la etapa 1. Los puntos seguros permiten dividirla en tramos balanceables.

## Estados de progresión

### Permanentes en el perfil

- Naves desbloqueadas.
- Tipos de acoplamiento desbloqueados.
- Niveles/etapas alcanzados o completados.
- Modos desbloqueados.
- Contenido permanente de tienda.
- Récords y puntuaciones relevantes.
- Desafíos o secretos, si se implementan.

### Persistentes dentro de una run

- Vidas restantes.
- Power-ups conservados entre niveles según la regla final de dificultad.
- Bombas/cargas temporales.
- Acoplamiento equipado o encontrado.
- Moneda temporal del hangar.

### Configuración inicial de una nueva run

- Etapas tempranas: estado base.
- Etapas avanzadas: loadout inicial mínimo definido por checkpoint para no entrar subpotenciado.
- Dificultades altas/hardcore: pueden eliminar o reducir esas ayudas.
- El contenido permanente desbloqueado sigue disponible para seleccionar en el hangar.

Esta configuración predeterminada se usa solamente cuando el jugador elige **iniciar una nueva run desde ese checkpoint**. No reemplaza el estado de una run suspendida.

### Continuación de una run existente

Cuando el jugador usa Guardar y salir y luego Continuar:

- la ubicación o tramo reanudado corresponde al último checkpoint seguro;
- se restaura el estado que el jugador tenía al guardar: vidas, power-ups no consumidos, bombas/cargas, acoplamiento equipado, moneda temporal y demás datos de la run;
- no se aplica el loadout predeterminado del checkpoint, porque no es una run nueva.

La separación conceptual es: **Continuar = recuperar una run existente**; **Iniciar desde checkpoint = crear una run nueva balanceada para ese tramo**.

## Supervivencia y daño según el modo

### Campaña principal

- Sistema de vidas.
- El foco está en esquivar y evitar impactos.
- Power-ups defensivos y acoplamientos pueden absorber daño antes de consumir una vida.
- Respawn con invulnerabilidad temporal.
- Todo daño recibido concede invulnerabilidad temporal, incluida la pérdida de escudo o acoplamiento, con una duración menor que la del respawn.
- Chocar contra un enemigo daña al jugador y destruye a los enemigos débiles; los pesados resisten el impacto.

### Supervivencia

- Se propuso un modelo de HP/desgaste acumulado.
- La regla no se diseñó en detalle y debe validarse cuando se implemente el modo.

### Infinito

- Se propuso un modelo híbrido de HP + vidas.
- También quedó como dirección futura, no especificación cerrada.

## Power-ups

Los power-ups mejoran capacidades existentes o entregan recursos/protección temporal. Su efecto puede variar según la nave.

### Tipos contemplados

- Potencia/cantidad de disparo.
- Escudo.
- Vida extra.
- Munición/carga de habilidad especial.
- Invulnerabilidad temporal.

### Persistencia

- La mejora de disparo puede acumular niveles hasta un máximo configurable.
- Escudo y vida extra se consumen por sus propias reglas.
- Munición permanece hasta usar la habilidad.
- Invulnerabilidad dura un tiempo determinado.
- Pueden conservarse al cambiar de nivel dentro de la misma run.
- En dificultades normales, perder una vida no elimina los power-ups que todavía no hayan sido consumidos por su propia regla.
- El escudo sí puede desaparecer al absorber el impacto, y la invulnerabilidad termina al agotarse su duración.
- Dificultades superiores podrán modificar esta persistencia; la regla exacta queda para el diseño del sistema de dificultad.

### Aparición

- Preferentemente diseñada, no gobernada por probabilidades opacas.
- Un enemigo específico de una oleada puede soltar un power-up sin convertirlo en propiedad universal de ese arquetipo.
- Estructuras destruibles pueden contener recursos.
- El nivel puede garantizar mejoras después de una sección difícil o antes de un pico.

## Acoplamientos

Los acoplamientos añaden sistemas o armas nuevas a la nave. Se distinguen de los power-ups porque cambian de manera más profunda la forma de jugar.

### Ejemplos

- Misiles.
- Láser.
- Contramedidas que destruyen disparos enemigos.
- Futuras posibilidades: drones, torretas, armas laterales o tecnología alienígena/híbrida.

### Reglas actuales

- Un único slot activo en el MVP.
- Varios slots son posibles para naves o modos futuros. Los acoplamientos actúan de forma automática o semi-automática, así que acumular varios no genera conflicto de controles.
- Disponibilidad y compatibilidad pueden depender de la nave.
- Son más raros que los power-ups.
- Pueden encontrarse al derrotar sub-bosses, bosses, unidades excepcionales o al interactuar con una instalación/base.
- Encontrar uno puede permitir usarlo de inmediato y desbloquear permanentemente su tipo.
- Los desbloqueos se gestionan libremente al llegar a un hangar.
- El acoplamiento se pierde al recibir daño y al perder una vida. Absorbe el impacto que lo destruye, evitando esa pérdida de vida.
- Se conserva al pasar de nivel dentro de la misma run, igual que los power-ups.
- Por defecto todos los acoplamientos comparten la misma durabilidad, pero ese valor es un dato por acoplamiento y no una regla fija en código: debe poder subirse para casos como un acoplamiento de protección.

### Prioridad defensiva

Cuando coexisten varias capas, el orden confirmado es:

1. Invulnerabilidad.
2. Escudo.
3. Acoplamiento.
4. Vida.

El acoplamiento actúa como capa defensiva: absorbe el impacto y se destruye antes de que se consuma una vida. La durabilidad es configurable por acoplamiento; el catálogo concreto de categorías sigue pendiente de diseño.

### Filosofía de introducción

Se introducen cuando el nivel hace evidente su utilidad, sin transformar el módulo en una llave obligatoria salvo casos especiales.

## Naves

Cada nave debe tener identidad jugable propia. Las diferencias posibles incluyen:

- velocidad y precisión de movimiento;
- fragilidad o resistencia según el modo;
- cadencia, cantidad y daño de disparos;
- disparo sostenido, manual, lento o cargado;
- habilidad especial;
- interacción con power-ups;
- compatibilidad y aprovechamiento de acoplamientos.

Las características concretas de cada nave se iterarán durante el desarrollo. La narrativa no debe depender de estadísticas inmutables.

## Desbloqueos

- Naves principales: progreso natural, bosses o final de etapa.
- Acoplamientos comunes: descubrimiento en niveles, unidades especiales o sub-bosses.
- Naves/acoplamientos especiales: desafíos, rutas ocultas, score, no morir u objetivos opcionales.
- Evitar requisitos basados únicamente en grind.
- Lo recién desbloqueado queda guardado de inmediato, pero se cambia/equipa libremente en el siguiente hangar.

## Hangar y reabastecimiento

### Hangar principal

- Aparece normalmente al terminar una etapa.
- Selección/cambio de nave.
- Gestión de acoplamientos desbloqueados.
- Posibles compras con moneda temporal de la run.
- Punto seguro de guardado/continuación.

### Punto de reabastecimiento

- Puede aparecer entre niveles o dentro de uno largo.
- Ofrece un subconjunto de opciones.
- Puede representarse como portaaviones, base o estación aliada.

## Puntuación y economías

### Score

- Métrica arcade de rendimiento.
- Presente desde el MVP.
- Puede alimentar récords y recompensas futuras.

### Moneda temporal de run

- Se obtiene durante la run.
- Solo se usa en hangares o puntos equivalentes.
- Se pierde cuando termina la run.
- Si se comienza desde una etapa avanzada, puede darse una cantidad base.
- El nombre todavía no está definido.

### Meta-economía

- Tienda permanente limitada a skins y algún contenido especial.
- Se consideró usar puntuación acumulada.
- No conviene que gastar reduzca un récord histórico.
- Alternativa sugerida: score histórico separado de créditos derivados del score.
- La decisión final quedó abierta.

## Diseño de enemigos y oleadas

Los siguientes conceptos deben ser independientes:

- arquetipo/enemigo;
- estadísticas base;
- trayectoria;
- patrón de disparo;
- formación;
- evento de aparición;
- recompensa/drop.

En niveles iniciales los patrones serán legibles y progresivos. En niveles avanzados pueden combinarse, reutilizar enemigos conocidos en nuevas trayectorias, añadir reactividad o aumentar la presión.

## Curva de intensidad

Cada nivel debería diseñarse con una curva relativa de presión a lo largo del tiempo:

- introducción;
- escalada;
- picos;
- descansos;
- recombinación de amenazas;
- clímax/boss.

La presión puede modificarse mediante cantidad, densidad de proyectiles, velocidad, resistencia, formación, obstáculos, espacio y simultaneidad. Se propuso crear más adelante una herramienta o representación gráfica para diseñar esa curva.
