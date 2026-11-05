# MODELO DE DATOS – KUMOC

Motor: SQLite. Seis tablas (cinco entidades principales y `PAGO`). El esquema vigente está en `src/main/resources/db/schema.sql` (tablas) y `db/indexes.sql` (índices). Solo se crea si la base no existe; un cambio de esquema exige borrar `%APPDATA%\Kumoc\kumoc.db` (o migrar).

## CLIENTES
Clientes a los que se presupuesta y vende, con información mínima.

| Campo | Tipo | Notas |
|---|---|---|
| id_cliente | INTEGER PK | autoincremental |
| razon_social | TEXT NOT NULL | nombre |
| cuit | TEXT UNIQUE | CUIT/CUIL, opcional (NULL si no se informa; varios clientes pueden no tenerlo) |
| tipo_factura | TEXT NOT NULL | `A`, `B`, `C`, `X` |
| telefono | TEXT | contacto |
| email, direccion | TEXT | opcionales |
| localidad, codigo_postal, provincia | TEXT | opcionales (texto libre; el CP no se valida por los distintos formatos) |
| activo | INTEGER 1/0 | baja lógica |

## PRODUCTOS
Productos vendibles (chalecos, camperas, buzos, etc.).

| Campo | Tipo | Notas |
|---|---|---|
| id_producto | INTEGER PK | |
| codigo | TEXT UNIQUE | opcional (NULL si no se informa; si se informa es único) |
| nombre | TEXT NOT NULL | |
| descripcion | TEXT | |
| talle, color | TEXT | |
| publico_objetivo | TEXT | |
| precio_unitario | REAL NOT NULL ≥ 0 | |
| stock_minimo | INTEGER NOT NULL ≥ 0 | umbral de alerta |
| activo | INTEGER 1/0 | baja lógica |
| imagen | TEXT | ruta en `%APPDATA%\Kumoc\imagenes` |
| ult_actualizacion | TEXT | |

`PRODUCTOS` no guarda stock: se deriva de `MOVIMIENTO_STOCK` (ver "Cálculo de stock").

## PRESUPUESTO
Cabecera del documento. Cubre todo el ciclo mediante `estado`.

| Campo | Tipo | Notas |
|---|---|---|
| id_presupuesto | INTEGER PK | |
| id_cliente | INTEGER FK → CLIENTES | |
| fecha_emision | TEXT | |
| total | REAL ≥ 0 | suma de subtotales |
| estado | TEXT | `PRESUPUESTO`, `RECHAZADO`, `ORDEN_TRABAJO`, `VENTA` |
| facturar_arca | INTEGER 1/0 | se define al registrar el pago que salda la orden |
| fecha_facturacion_arca | TEXT | NULL = pendiente de facturar en ARCA |

El pago y la facturación son ejes independientes del `estado`. El cobro se deriva de `PAGO` (ver abajo).

## PAGO
Cobros de una orden; puede haber varios (pagos parciales).

| Campo | Tipo | Notas |
|---|---|---|
| id_pago | INTEGER PK | |
| id_presupuesto | INTEGER FK → PRESUPUESTO | |
| fecha | TEXT NOT NULL | fecha del cobro |
| monto | REAL NOT NULL > 0 | |

`pagado = Σ monto`; `saldo = total − pagado`. La orden está cobrada cuando `saldo = 0`; la fecha de cobro es la del último pago. No se guarda saldo ni estado de cobro: se calculan.

## PRODUCTOSxPRESUPUESTOS
Detalle de ítems de cada presupuesto.

| Campo | Tipo | Notas |
|---|---|---|
| id_prodxpres | INTEGER PK | |
| id_presupuesto | INTEGER FK | |
| id_producto | INTEGER FK | |
| cantidad | INTEGER > 0 | |
| precio_unitario | REAL > 0 | precio vigente al presupuestar |
| subtotal | REAL > 0 | = cantidad × precio_unitario; lo calcula el servicio (2 decimales) |

## MOVIMIENTO_STOCK
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
- PRESUPUESTO 1 — N PAGO
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
Confirmar la regla de la transición `ORDEN_TRABAJO → VENTA`. Ver `DECISIONES_TECNICAS.md`.
