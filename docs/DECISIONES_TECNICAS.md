# DECISIONES TÉCNICAS – KUMOC

Solo se registran decisiones aprobadas. Lo no resuelto figura en "Pendientes".

## Stack
- Java 25, JavaFX, SQLite local (`sqlite-jdbc`), Maven.
- Arquitectura MVC. La lógica de negocio queda fuera de los controladores.
- **Sin ORM**, deliberadamente: se busca control explícito sobre las transacciones críticas (registro de movimientos y descuento de stock). Acceso por JDBC con consultas parametrizadas.
- **Por qué SQLite:** aplicación monousuario sin red ni servidor; ofrece transacciones ACID reales sin la complejidad operativa de un motor cliente-servidor.
- La base y las imágenes se guardan en `%APPDATA%\Kumoc` (`AppPaths`), fuera del repositorio. La base se crea al primer arranque si no existe.

## Stock
- **Stock derivado de movimientos.** No se almacena un contador editable en `PRODUCTOS`. El stock se calcula sumando el historial de `MOVIMIENTO_STOCK`.
- Tipos de movimiento: `INGRESO`, `EGRESO`, `AJUSTE`. Cada movimiento lleva fecha y motivo obligatorio.
- `INGRESO` y `EGRESO` llevan cantidad positiva; `AJUSTE` lleva cantidad con signo (+/−).
- Los movimientos son inmutables: las correcciones se hacen con un nuevo movimiento de `AJUSTE`.
- El stock calculado no puede quedar negativo.
- **Stock físico** = `Σ INGRESO − Σ EGRESO + Σ AJUSTE`.
- **Stock comprometido (congelado)** = suma de las cantidades de los ítems de presupuestos en estado `ORDEN_TRABAJO`. Se calcula; no hay contador ni movimientos de reserva.
- **Stock disponible** = físico − comprometido.
- **Egreso:** el `EGRESO` por ítem se genera al pasar la orden a `VENTA`. Mientras es orden, el stock solo está congelado.
- **Fabricación de faltantes:** se registra como `INGRESO` con motivo "Fabricación".
- `MOVIMIENTO_STOCK.id_presupuesto` (nullable) vincula el movimiento con la orden/venta que lo originó.

## Presupuestos, órdenes de trabajo y ventas
- **Una sola tabla** (`PRESUPUESTO`) con campo `estado` para todo el ciclo: presupuesto → orden de trabajo → venta registrada. El detalle es `PRODUCTOSxPRESUPUESTOS`.
- Estados (`estado`): `PRESUPUESTO`, `RECHAZADO`, `ORDEN_TRABAJO`, `VENTA`.
- Pago y facturación son **ejes independientes** del estado:
  - Pago externo a la aplicación, con **pagos parciales**: cada cobro es una fila de `PAGO` (fecha y monto). Saldo = total − Σ pagos; cobrada cuando el saldo es 0. El monto de un pago no puede superar el saldo.
  - `facturar_arca` (1/0), definido al registrar el pago que salda la orden, y `fecha_facturacion_arca` (NULL = pendiente).
- Listado de fin de mes: `facturar_arca = 1` (solo se define al saldar) y `fecha_facturacion_arca` nula.

## Imagen de producto
- Cada producto puede tener una imagen. Se guarda comprimida en `%APPDATA%\Kumoc\imagenes` (junto a la base) como WebP con pérdida (calidad 0,8), redimensionada a un máximo de 800 px de lado; la transparencia se aplana sobre fondo blanco.
- En `PRODUCTOS.imagen` solo se guarda el nombre de archivo generado (`{uuid}.webp`). `ImagenService` solo lee/borra nombres con ese formato (evita rutas manipuladas), rechaza archivos que no sean imagen, de más de 20 MB o de más de 50 millones de píxeles.
- Al reemplazar o quitar la imagen se borra el archivo anterior recién cuando el producto se guardó. Al dar de baja un producto la imagen se conserva (la baja es reversible).
- **Dependencia agregada:** `com.github.usefulness:webp-imageio` 0.11.0 (trae Kotlin stdlib como dependencia transitiva). Es necesaria porque Java no escribe WebP de forma nativa; incluye los binarios nativos para Windows. Verificado: ida y vuelta de lectura/escritura en Windows.

## Distribución (Windows)
- Versión portable: `packaging\empaquetar.ps1` compila con Maven, reúne las dependencias y usa `jpackage` (del JDK 25) para generar una carpeta `KUMOC` con `KUMOC.exe` y un runtime de Java propio, y la comprime en `dist\KUMOC-<versión>-windows.zip` (`dist/` está en `.gitignore`). Quien la descarga no necesita instalar Java. Se ejecuta con `powershell -ExecutionPolicy Bypass -File packaging\empaquetar.ps1 [-Version x.y.z]`.
- Se ejecuta en modo classpath (el proyecto no tiene `module-info.java`); el runtime incluye los módulos `java.base`, `java.desktop`, `java.scripting` (lo exige FXML), `java.sql`, `java.logging`, `java.naming`, `java.xml`, `java.prefs`, `jdk.jfr` y `jdk.unsupported`. Si se agrega una dependencia nueva, revisar con `jdeps --print-module-deps` si hace falta otro módulo.
- Ícono: `packaging/kumoc.ico`, generado a partir de `resources/img/logo-cuadrado.png`.
- Sin firma digital: Windows SmartScreen avisa al ejecutar por primera vez. Un instalador `.exe` requeriría instalar WiX Toolset (pendiente, no aprobado).
- Los datos viven en `%APPDATA%\Kumoc`, fuera de la carpeta de la app: reemplazar la carpeta por una versión nueva no los borra.
- **Pendiente antes de distribuir con datos reales:** no hay migración de base de datos (el esquema solo se crea si el archivo no existe). Una versión con cambios de esquema dejaría inutilizable la base de un usuario existente.

## Interfaz gráfica (JavaFX + FXML)
- Ventana principal con barra lateral (logo, 5 secciones y contador de alertas de stock bajo) y área de contenido; cada sección es un FXML con su controlador en `org.kumoc.controller` y usa solo los servicios de `AppContext`.
- Secciones: Productos (con imagen), Clientes, Presupuestos, Órdenes de trabajo y Stock. Diálogos: presupuesto (ítems y total), movimiento de stock (ingreso, ajuste, fabricación) y cobro.
- Estilo en `resources/css/kumoc.css` con la paleta de Kumoc: negro `#000000`, rojo `#E21C21`, naranja `#EA5823`, grises `#1F1F1F`/`#242323`/`#E8E6E6`. Logo en `resources/img/logo.png`.
- Las operaciones corren en el hilo de JavaFX (base local, volumen chico). Los errores de negocio se muestran con su mensaje; los inesperados, con un mensaje genérico.
- En la pantalla de Stock el comprometido se rotula "Congelado". Al registrar un cobro, "Facturar en ARCA" aparece destildado por defecto y solo se muestra cuando el pago salda la orden.
- Cobro: el monto se sugiere con el saldo completo y es editable ("Me pagó…") para un pago parcial; se avisa el saldo que quedará.
- Formularios de Productos, Clientes y Presupuestos con tres modos: registro nuevo → `GUARDAR NUEVO`; registro seleccionado → `EDITAR` (campos de solo lectura); en edición → `GUARDAR`. `DAR DE BAJA` siempre disponible junto a ellos (deshabilitado en registro nuevo). En Productos y Clientes es baja lógica (`activo`, reactivable); en Presupuestos elimina el presupuesto (solo en `PRESUPUESTO` o `RECHAZADO`).
- Alta rápida de cliente desde "Nuevo presupuesto" con el botón "+ Nuevo cliente" (diálogo `ClienteDialog`).

## Estructura del código (paquetes de `org.kumoc`)
- `model`: entidades inmutables (`record`) y enums. `dao`: JDBC con consultas parametrizadas, sin lógica de negocio. `service`: reglas de negocio y transacciones. `controller` y `resources/view` (FXML): capa gráfica. `database`, `config`, `exception`: infraestructura.
- `AppContext` arma conexión, DAOs y servicios una sola vez; los controladores usan solo los servicios.
- `Transaccion.ejecutar` agrupa operaciones críticas (movimientos de stock, pasar a venta) y hace rollback ante cualquier error.
- Errores: `NegocioException` (mensaje apto para el usuario) y `AccesoDatosException` (mensaje genérico; el detalle queda en la causa).

## Implementación inicial (pendiente de tu revisión)
- Dinero: `BigDecimal` en Java, `REAL` en SQLite, redondeo a 2 decimales en los servicios. Por eso el `CHECK` de `subtotal` pasó de `= cantidad × precio` a `> 0`.
- CUIT/CUIL: opcional. Si se informa, se acepta con o sin guiones y se guarda con 11 dígitos (único); no se verifica el dígito verificador. Vacío se guarda como NULL.
- Un presupuesto solo se modifica en estado `PRESUPUESTO`; solo se elimina en `PRESUPUESTO` o `RECHAZADO`. El cobro se registra en `ORDEN_TRABAJO` o `VENTA`.
- Eliminar una orden de trabajo o venta: permitido solo sin cobros registrados y sin facturación ARCA. Borra los movimientos de stock ligados a la orden (fabricación y venta), por lo que el stock vuelve al valor previo; se cancela todo si algún producto quedara con stock físico negativo. Se pierde el rastro histórico de esos movimientos (decisión del usuario).
- Alerta de stock bajo: producto activo con stock **físico** menor a `stock_minimo`.
- Código de producto opcional: vacío se guarda como NULL y la unicidad se valida solo si se informa. Sin código, listas y PDF muestran solo el nombre; en el listado de faltantes van al final. La tabla de Stock muestra Talle y Color en columnas propias.
- No hay pruebas automatizadas en el repositorio.

## PDF
- **Dependencia agregada:** `org.apache.pdfbox:pdfbox` 3.0.5 (Apache 2.0; trae `fontbox`, `pdfbox-io` y `commons-logging`). Java no genera PDF de forma nativa; PDFBox permite replicar el diseño del modelo con logo sin depender de JavaFX. Se usa la fuente estándar Helvetica (cubre tildes y ñ), sin incrustar fuentes.
- Paquete `org.kumoc.pdf`: `PdfBase` (A4, franja negra superior, recuadro negro con el logo `resources/img/logo.png`, helpers y paginación), `PresupuestoPdf` y `FaltantesPdf`. Los controladores solo piden el destino (`ArchivosPdf`, diálogo de guardado) y llaman al generador.
- **Presupuesto en PDF:** cabecera (título, N° de 8 dígitos, fecha en español, logo), datos del cliente (CUIT solo si existe), tabla Descripción / Cant. / Precio unit. / Subtotal, recuadro TOTAL (suma de ítems, sin impuestos) y pie gris con "Información para el pago" y "Datos de contacto". No incluye tiempo estimado ni forma de pago (no existen en el modelo).
- **Datos del negocio del pie:** `%APPDATA%\Kumoc\negocio.properties` (UTF-8; claves `pago.banco`, `pago.titular`, `pago.cuenta`, `contacto.email`, `contacto.telefono`, `contacto.direccion`). Si no existe se crea una plantilla vacía al imprimir el primer presupuesto; los campos vacíos se omiten y, si todo está vacío, no se dibuja el pie.
- **Faltantes a fabricar:** por producto, `aFabricar = max(0, pedido total de las órdenes en producción − stock físico)`, con las órdenes que lo piden; consolida todas las órdenes (no cada una por separado) y ordena por código de forma natural (CH-2 antes que CH-10). Si no falta nada, el PDF lo informa.
