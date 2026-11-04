# MODELO DE DATOS – KUMOC

Motor: SQLite. Cinco entidades principales. Los nombres de columnas parten de `src/main/resources/db/schema.sql` y los marcados con **(nuevo)** o **(cambia)** requieren modificar ese esquema (pendiente de aprobación, ver `DECISIONES_TECNICAS.md`).

## CLIENTES
Clientes a los que se presupuesta y vende, con información mínima.

| Campo | Tipo | Notas |
|---|---|---|
| id_cliente | INTEGER PK | autoincremental |
| razon_social | TEXT NOT NULL | nombre |
| cuit | TEXT NOT NULL UNIQUE | CUIT/CUIL |
| tipo_factura | TEXT NOT NULL | `A`, `B`, `C`, `X` **(nuevo)** |
| telefono | TEXT | contacto |
| email, direccion | TEXT | ya existen en el esquema actual; opcionales |

## PRODUCTOS
Productos vendibles (chalecos, camperas, buzos, etc.).

| Campo | Tipo | Notas |
|---|---|---|
| id_producto | INTEGER PK | |
| codigo | TEXT NOT NULL UNIQUE | |
| nombre | TEXT NOT NULL | |
| descripcion | TEXT | |
| talle, color | TEXT | |
| publico_objetivo | TEXT | |
| precio_unitario | REAL NOT NULL ≥ 0 | |
| stock_minimo | INTEGER NOT NULL ≥ 0 | umbral de alerta |
| activo | INTEGER 1/0 | baja lógica |
| imagen | TEXT | ruta en `%APPDATA%\Kumoc\imagenes` |
| ult_actualizacion | TEXT | |

**(cambia)** Se eliminan `stock_fisico` y `stock_comprometido`: el stock se deriva de `MOVIMIENTO_STOCK`.

## PRESUPUESTO
Cabecera del documento. Cubre todo el ciclo mediante `estado`.

| Campo | Tipo | Notas |
|---|---|---|
| id_presupuesto | INTEGER PK | |
| id_cliente | INTEGER FK → CLIENTES | |
| fecha_emision | TEXT | |
| total | REAL ≥ 0 | suma de subtotales |
| estado | TEXT | **(cambia)** `PRESUPUESTO`, `RECHAZADO`, `ORDEN_TRABAJO`, `VENTA` (hoy el esquema tiene `FACTURADO`) |
| fecha_cobro | TEXT | **(nuevo)** NULL = pendiente de pago; se completa al registrar el pago |
| facturar_arca | INTEGER 1/0 | **(nuevo)** se define al registrar el pago |
| fecha_facturacion_arca | TEXT | **(nuevo)** NULL = pendiente de facturar en ARCA |

El pago y la facturación son ejes independientes del `estado`.

## PRODUCTOSxPRESUPUESTOS
Detalle de ítems de cada presupuesto.

| Campo | Tipo | Notas |
|---|---|---|
| id_prodxpres | INTEGER PK | |
| id_presupuesto | INTEGER FK | |
| id_producto | INTEGER FK | |
| cantidad | INTEGER > 0 | |
| precio_unitario | REAL | precio vigente al presupuestar |
| subtotal | REAL | = cantidad × precio_unitario |

## MOVIMIENTO_STOCK **(nuevo en db/schema.sql)**
Histórico inmutable de eventos de inventario.

| Campo | Tipo | Notas |
|---|---|---|
| id_movimiento | INTEGER PK | |
| id_producto | INTEGER FK → PRODUCTOS | |
| tipo_movimiento | TEXT | `INGRESO`, `EGRESO`, `AJUSTE` |
| cantidad | INTEGER ≠ 0 | positiva en INGRESO/EGRESO; con signo (+/−) en AJUSTE |
| fecha | TEXT | |
| motivo | TEXT NOT NULL | p. ej. "Fabricación", "Venta" |
| id_presupuesto | INTEGER FK, nullable | orden/venta que originó el movimiento (fabricación, venta) |

## Relaciones
- CLIENTES 1 — N PRESUPUESTO
- PRESUPUESTO 1 — N PRODUCTOSxPRESUPUESTOS N — 1 PRODUCTOS
- PRODUCTOS 1 — N MOVIMIENTO_STOCK

## Cálculo de stock
- `físico = Σ INGRESO − Σ EGRESO + Σ AJUSTE` (con el signo del ajuste).
- `comprometido = Σ cantidad` de los ítems de presupuestos en estado `ORDEN_TRABAJO`.
- `disponible = físico − comprometido`.

Todo se calcula con consulta; nada se guarda. Alerta de stock bajo cuando el stock calculado `< stock_minimo`.

## Ciclo de vida
`PRESUPUESTO` → (aprobado) `ORDEN_TRABAJO` → (faltantes cubiertos, se generan los EGRESO) `VENTA`. `RECHAZADO` desde `PRESUPUESTO`. El cobro y la facturación ARCA se registran aparte.

## Pendiente
Regla exacta de la transición `ORDEN_TRABAJO → VENTA`. Ver `DECISIONES_TECNICAS.md`.
