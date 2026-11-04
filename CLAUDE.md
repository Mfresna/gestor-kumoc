# KUMOC

## 1. Stack y configuración
- Stack: Java 25, SQLite local, JavaFX
- Respeta las versiones, dependencias, configuración y convenciones existentes.
- No agregues dependencias sin justificar su necesidad.
- No asumas que una funcionalidad o estructura existe: inspecciona el código relevante.
- Sigamos el modelo MVC - Modelo, Vista Controlador
- Aplicacion de escritorio monousuario

## 2. Arquitectura y desarrollo
- Mantén la lógica de negocio fuera de los controladores.
- Usa DTOs, validaciones y manejo de excepciones según las convenciones existentes.
- Respeta las responsabilidades de cada capa.
- Revisa transacciones, consultas, rendimiento y carga de relaciones.
- Evita consultas innecesarias, problemas N+1 y exposición accidental de entidades.
- Maneja excepciones sin exponer detalles internos sensibles.
- Aplica prácticas seguras de autenticación y autorización.

## 3. Base de datos y seguridad
- No modifiques el esquema de la base de datos ni ejecutes migraciones sin aprobación explícita.
- No ejecutes operaciones destructivas ni alteres datos reales sin autorización.
- Nunca expongas contraseñas, tokens, credenciales ni otros datos sensibles.
- Usa consultas parametrizadas y evita la inyección SQL.
- Verifica autorización, validación de entradas y protección de datos en las operaciones sensibles.

## 4. Implementación y pruebas
- Antes de implementar, identifica brevemente los archivos afectados, el enfoque y las pruebas necesarias.
- Para tareas pequeñas y claras, evita análisis y explicaciones innecesariamente extensos.
- Mantén los cambios acotados al requerimiento y evita refactorizaciones no solicitadas.
- No sobrescribas cambios existentes del usuario.
- Ejecuta la compilación y las pruebas pertinentes cuando sea posible.
- Informa qué verificaste, qué falló y qué no pudiste comprobar.

## 5. Repositorio de referencia: Arquitour
- Repositorio: https://github.com/Mfresna/ArquiTour
- Consúltalo cuando una tarea dependa de una implementación, patrón o decisión previa de Arquitour.
- Inspecciona únicamente los archivos relevantes; no explores todo el repositorio.
- Si la solución está clara y no depende de ese antecedente, no lo consultes.
- Adapta los patrones a KUMOC, no copies código automáticamente.
- No modifiques Arquitour.
- Si no puedes acceder al repositorio, indícalo sin suponer su contenido.

## 6. Contexto compartido del proyecto
- Antes de trabajar, consulta las instrucciones aplicables y la documentación relevante de `docs/`.
- `docs/MODELO_DATOS.md` es la referencia del modelo de datos aprobado.
- `docs/DECISIONES_TECNICAS.md` contiene las decisiones técnicas y funcionales aprobadas.
- `docs/REQUISITOS.md` contiene los requisitos funcionales numerados (RF-xx).
- Si alguno de estos archivos aún no existe, indícalo y no supongas su contenido.
- Lee solo las secciones de `docs/` relacionadas con la tarea, no los archivos completos.
- No asumas que conoces otras conversaciones.
- No inventes ni contradigas entidades, relaciones, reglas de negocio o decisiones documentadas.
- Si falta información relevante o hay contradicciones, pregunta antes de tomar decisiones que afecten la arquitectura o el comportamiento del sistema.
- No cambies decisiones aprobadas por iniciativa propia.
- Cuando se apruebe un cambio relevante, actualiza la documentación correspondiente para mantenerla alineada con el código.
- No actualices documentación con decisiones hipotéticas o propuestas que todavía no fueron aprobadas.
- Lee únicamente los archivos necesarios para la tarea actual y evita repetir información que ya esté documentada.