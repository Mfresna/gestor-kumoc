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
  - Pago externo a la aplicación: `fecha_cobro` (NULL = pendiente de pago).
  - `facturar_arca` (1/0), definido al registrar el pago, y `fecha_facturacion_arca` (NULL = pendiente).
- Listado de fin de mes: `fecha_cobro` no nula, `facturar_arca = 1` y `fecha_facturacion_arca` nula.

## Imagen de producto
- Cada producto puede tener una imagen. Se guarda comprimida en `%APPDATA%\Kumoc\imagenes` (junto a la base) como WebP con pérdida (calidad 0,8), redimensionada a un máximo de 800 px de lado; la transparencia se aplana sobre fondo blanco.
- En `PRODUCTOS.imagen` solo se guarda el nombre de archivo generado (`{uuid}.webp`). `ImagenService` solo lee/borra nombres con ese formato (evita rutas manipuladas), rechaza archivos que no sean imagen, de más de 20 MB o de más de 50 millones de píxeles.
- Al reemplazar o quitar la imagen se borra el archivo anterior recién cuando el producto se guardó. Al dar de baja un producto la imagen se conserva (la baja es reversible).
- **Dependencia agregada:** `com.github.usefulness:webp-imageio` 0.11.0 (trae Kotlin stdlib como dependencia transitiva). Es necesaria porque Java no escribe WebP de forma nativa; incluye los binarios nativos para Windows. Verificado: ida y vuelta de lectura/escritura en Windows.

## Interfaz gráfica (JavaFX + FXML)
- Ventana principal con barra lateral (logo, 5 secciones y contador de alertas de stock bajo) y área de contenido; cada sección es un FXML con su controlador en `org.kumoc.controller` y usa solo los servicios de `AppContext`.
- Secciones: Productos (con imagen), Clientes, Presupuestos, Órdenes de trabajo y Stock. Diálogos: presupuesto (ítems y total), movimiento de stock (ingreso, ajuste, fabricación) y cobro.
- Estilo en `resources/css/kumoc.css` con la paleta de Kumoc: negro `#000000`, rojo `#E21C21`, naranja `#EA5823`, grises `#1F1F1F`/`#242323`/`#E8E6E6`. Logo en `resources/img/logo.png`.
- Las operaciones corren en el hilo de JavaFX (base local, volumen chico). Los errores de negocio se muestran con su mensaje; los inesperados, con un mensaje genérico.
- En la pantalla de Stock el comprometido se rotula "Congelado". Al registrar un cobro, "Facturar en ARCA" aparece destildado por defecto.

## Estructura del código (paquetes de `org.kumoc`)
- `model`: entidades inmutables (`record`) y enums. `dao`: JDBC con consultas parametrizadas, sin lógica de negocio. `service`: reglas de negocio y transacciones. `controller` y `resources/view` (FXML): capa gráfica. `database`, `config`, `exception`: infraestructura.
- `AppContext` arma conexión, DAOs y servicios una sola vez; los controladores usan solo los servicios.
- `Transaccion.ejecutar` agrupa operaciones críticas (movimientos de stock, pasar a venta) y hace rollback ante cualquier error.
- Errores: `NegocioException` (mensaje apto para el usuario) y `AccesoDatosException` (mensaje genérico; el detalle queda en la causa).

## Implementación inicial (pendiente de tu revisión)
- Dinero: `BigDecimal` en Java, `REAL` en SQLite, redondeo a 2 decimales en los servicios. Por eso el `CHECK` de `subtotal` pasó de `= cantidad × precio` a `> 0`.
- CUIT/CUIL: se acepta con o sin guiones y se guarda con 11 dígitos; no se verifica el dígito verificador.
- Un presupuesto solo se modifica en estado `PRESUPUESTO`; solo se elimina en `PRESUPUESTO` o `RECHAZADO`. El cobro se registra en `ORDEN_TRABAJO` o `VENTA`.
- Alerta de stock bajo: producto activo con stock **físico** menor a `stock_minimo`.
- Fuera de alcance por ahora: exportación a PDF (requiere una dependencia a justificar). No hay pruebas automatizadas en el repositorio.

## Pendientes (requieren aprobación)
- Confirmar la regla de `ORDEN_TRABAJO → VENTA`, hoy implementada como acción manual que exige cero faltantes (stock físico ≥ cantidad en cada ítem) y genera los `EGRESO`.
- Librería para el PDF del presupuesto.
