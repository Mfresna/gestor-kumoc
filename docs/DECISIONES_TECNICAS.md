# DECISIONES TÉCNICAS – KUMOC

Solo se registran decisiones aprobadas. Lo no resuelto figura en "Pendientes".

## Stack
- Java 25, JavaFX, SQLite local (`sqlite-jdbc`), Maven.
- Arquitectura MVC. La lógica de negocio queda fuera de los controladores.
- **Sin ORM**, deliberadamente: se busca control explícito sobre las transacciones críticas (registro de movimientos y descuento de stock). Acceso por JDBC con consultas parametrizadas.
- **Por qué SQLite:** aplicación monousuario sin red ni servidor; ofrece transacciones ACID reales sin la complejidad operativa de un motor cliente-servidor.
- La base y las imágenes se guardan en `%APPDATA%\Kumoc` (`AppPaths`), fuera del repositorio. La base se crea al primer arranque si no existe.

## Stock
- **Stock derivado de movimientos.** No se almacena un contador editable en `PRODUCTOS` (se eliminan `stock_fisico` y `stock_comprometido` del esquema). El stock se calcula sumando el historial de `MOVIMIENTO_STOCK`.
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

## Pendientes (requieren aprobación antes de implementar)
- Regla exacta de la transición `ORDEN_TRABAJO → VENTA` (propuesta: disponible ≥ cantidad en cada ítem) y si es automática o manual.
- Alineación del `pom.xml` con Java 25 (hoy declara 26) y agregado de JavaFX.
- Unificación de los `schema.sql` duplicados en `src/main/resources` (`db/`, `Otros/` y raíz).
