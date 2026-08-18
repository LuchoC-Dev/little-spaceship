# Plataforma y validación técnica

## Estado de la decisión

**Resuelto el 18/08/2026.** El prototipo técnico se ejecutó y la opción web quedó aprobada: `Java + libGDX + Gradle + gdx-teavm`, con desktop compartiendo el mismo core. Gradle sustituye definitivamente a Maven.

Los resultados, el semáforo final y lo que sigue pendiente están en `11-resultado-prototipo-tecnico.md`. El resto de este documento se conserva como registro del razonamiento previo a la medición.

## Requisitos técnicos derivados del producto

- Renderizado 2D con sprites y pixel-art.
- Muchos enemigos, proyectiles y partículas.
- Movimiento y colisiones precisas.
- Teclado y mouse.
- Música, efectos y cambios de pista en runtime.
- Menús, HUD y overlays.
- Carga de assets y pantalla de carga.
- Resolución lógica y escalado.
- Debugging razonable desde Java.
- Build reproducible y publicable.
- Posibilidad de añadir targets sin acoplar el core a uno solo.

## Objetivos demostrables de portfolio

El proyecto debe exhibir de forma conjunta:

- arquitectura modular y decisiones justificadas;
- testing automatizado relevante;
- integración continua;
- medición y cuidado del rendimiento;
- documentación técnica y de producto;
- acabado artístico y audiovisual;
- proceso reproducible de publicación y despliegue.

El repositorio será privado durante el desarrollo inicial. Su apertura se reconsiderará al alcanzar el MVP o al finalizar el producto.

## Lenguaje y build tool

### Java

Java es una decisión de identidad y objetivo del proyecto, no una restricción accidental. Se mantiene como lenguaje principal.

### Maven → Gradle provisional

Maven fue la preferencia inicial. La investigación posterior indicó que:

- TeaVM posee integración Maven;
- libGDX puede usarse con Maven, especialmente en desktop;
- el flujo multiplataforma de libGDX y gdx-teavm está orientado a Gradle;
- insistir en Maven para ese stack aumentaría integración manual y riesgo.

Por eso, para la candidata web, la recomendación provisional es **Gradle**. No es una elección final hasta validar el stack.

## Candidata principal

**Java + libGDX + Gradle + gdx-teavm → navegador**

Con un core compartido y target desktop posible.

### Razones

- libGDX cubre render 2D, input, audio, UI, assets, cámaras y viewports.
- gdx-teavm permite compilar el proyecto web a JavaScript o WebAssembly.
- Un juego local y puramente cliente puede alojarse como sitio estático.
- Para portfolio, “abrir un enlace y jugar” reduce mucho la fricción.
- El mismo core puede conservar una salida desktop.

## Hosting

La investigación previa consideró viables opciones gratuitas para un build estático:

- GitHub Pages.
- Cloudflare Pages.
- Vercel.

El hosting no se considera el principal riesgo. El riesgo está en compatibilidad, tooling, tamaño del build, debugging y dependencias.

## Riesgos y restricciones

### Compatibilidad TeaVM

TeaVM no ejecuta una JVM completa en el navegador. No debe asumirse que cualquier dependencia de Maven Central funcionará.

Revisar especialmente:

- APIs específicas de JVM/desktop;
- JNI o código nativo;
- filesystem directo;
- reflexión compleja;
- threading no compatible;
- bibliotecas que dependan de capacidades no disponibles en web.

Regla propuesta: cada dependencia adicional debe evaluarse tanto por licencia como por compatibilidad TeaVM.

### Backend adicional

gdx-teavm es un proyecto separado del core oficial de libGDX. Aunque fue evaluado como activo y capaz de generar JavaScript/Wasm, añade una dependencia tecnológica y un riesgo extra respecto de desktop puro.

### Rendimiento

El stack parece apropiado, pero el rendimiento con alta densidad de entidades y efectos debe medirse antes de cerrar la decisión.

### Compatibilidad de navegador

Verificar Chrome, Firefox, Edge y Safari en un alcance razonable. Revisar también formatos de audio y comportamiento de input/foco.

## Prototipo técnico de decisión

Antes de implementar el MVP, crear una prueba descartable que valide únicamente infraestructura:

- renderizado masivo de sprites/proyectiles;
- colisiones o actualización de muchas entidades;
- teclado y mouse, incluido su uso simultáneo y aditivo;
- captura del puntero (Pointer Lock) para el mouse relativo;
- audio, efectos y cambio de música;
- carga de texturas/atlas/fuentes/sonidos;
- UI/HUD básico;
- resolución lógica y escalado pixel-art;
- build web JavaScript;
- build web WebAssembly si está disponible;
- tiempos/tamaño de carga;
- source maps y stack traces útiles;
- ejecución desktop desde el mismo core;
- compatibilidad en navegadores objetivo.

## Criterio de elección

Elegir web si:

- la prueba alcanza rendimiento estable con margen;
- el flujo de build y debugging es razonable;
- los assets y el audio funcionan de forma consistente;
- las restricciones no dictan el diseño del juego;
- el costo de mantener web + desktop es aceptable.

Pasar a desktop si:

- las restricciones de TeaVM afectan el gameplay o la arquitectura;
- el debugging o build resultan frágiles;
- el rendimiento/compatibilidad no alcanza;
- mantener el target web consume esfuerzo desproporcionado.

## Principios arquitectónicos ya expresados

No son todavía un diseño de clases o módulos, pero deben guiar la futura arquitectura:

- composición por encima de herencia;
- inyección de dependencias;
- eventos cuando aporten desacoplamiento real;
- separación entre lógica del juego y adaptadores de plataforma;
- contenido configurable para balancear sin reescribir sistemas;
- patrones, trayectorias, formaciones y drops desacoplados del arquetipo enemigo;
- evitar implementar un framework de UI propio;
- evitar abstracciones futuras sin un caso real del MVP.

## Alternativas conservadas

### libGDX desktop

Camino técnicamente más directo, con JVM completa y menos restricciones. Desventaja: requiere descarga y añade fricción al portfolio.

### Web con JS/TS

Más natural para navegador, pero contradice el deseo de mantener Java como núcleo. Se dejó en segundo plano.

### Otros frameworks Java

FXGL se mencionó como alternativa con afinidad por Maven y 2D. No fue seleccionado ni validado al mismo nivel que libGDX + gdx-teavm.
