CREATE TABLE PRODUCTOS (
    id_producto INTEGER PRIMARY KEY AUTOINCREMENT,
    codigo TEXT UNIQUE,
    nombre TEXT NOT NULL,
    talle TEXT,
    publico_objetivo TEXT,
    color TEXT,
    descripcion TEXT,
    precio_unitario REAL NOT NULL CHECK (precio_unitario >= 0),
    activo INTEGER NOT NULL DEFAULT 1 CHECK (activo IN (1,0)),
    stock_minimo INTEGER NOT NULL DEFAULT 0 CHECK (stock_minimo >= 0),
    imagen TEXT,
    ult_actualizacion TEXT NOT NULL DEFAULT (datetime('now'))
);

CREATE TABLE CLIENTES (
    id_cliente INTEGER PRIMARY KEY AUTOINCREMENT,
    razon_social TEXT NOT NULL,
    cuit TEXT UNIQUE,
    tipo_factura TEXT NOT NULL CHECK (tipo_factura IN ('A','B','C','X')),
    email TEXT,
    telefono TEXT,
    direccion TEXT,
    localidad TEXT,
    codigo_postal TEXT,
    provincia TEXT,
    activo INTEGER NOT NULL DEFAULT 1 CHECK (activo IN (1,0))
);

CREATE TABLE PRESUPUESTO (
    id_presupuesto INTEGER PRIMARY KEY AUTOINCREMENT,
    id_cliente INTEGER NOT NULL,
    fecha_emision TEXT NOT NULL DEFAULT (date('now')),
    estado TEXT NOT NULL DEFAULT 'PRESUPUESTO' CHECK (estado IN ('PRESUPUESTO','RECHAZADO','ORDEN_TRABAJO','VENTA')),
    total REAL NOT NULL DEFAULT 0 CHECK (total >= 0),
    facturar_arca INTEGER NOT NULL DEFAULT 0 CHECK (facturar_arca IN (1,0)),
    fecha_facturacion_arca TEXT,
    FOREIGN KEY (id_cliente) REFERENCES CLIENTES(id_cliente)
);

-- Cobros de una orden (puede haber varios: pagos parciales). Saldo = total del presupuesto - suma de pagos.
CREATE TABLE PAGO (
    id_pago INTEGER PRIMARY KEY AUTOINCREMENT,
    id_presupuesto INTEGER NOT NULL,
    fecha TEXT NOT NULL,
    monto REAL NOT NULL CHECK (monto > 0),
    FOREIGN KEY (id_presupuesto) REFERENCES PRESUPUESTO(id_presupuesto)
);

CREATE TABLE PRODUCTOSxPRESUPUESTOS (
    id_prodxpres INTEGER PRIMARY KEY AUTOINCREMENT,
    id_presupuesto INTEGER NOT NULL,
    id_producto INTEGER NOT NULL,
    cantidad INTEGER NOT NULL CHECK (cantidad > 0),
    precio_unitario REAL NOT NULL CHECK (precio_unitario > 0),
    subtotal REAL NOT NULL CHECK (subtotal > 0),
    FOREIGN KEY (id_presupuesto) REFERENCES PRESUPUESTO(id_presupuesto),
    FOREIGN KEY (id_producto) REFERENCES PRODUCTOS(id_producto)
);

-- Historico inmutable. INGRESO/EGRESO: cantidad positiva. AJUSTE: cantidad con signo (distinta de 0).
CREATE TABLE MOVIMIENTO_STOCK (
    id_movimiento INTEGER PRIMARY KEY AUTOINCREMENT,
    id_producto INTEGER NOT NULL,
    tipo_movimiento TEXT NOT NULL CHECK (tipo_movimiento IN ('INGRESO','EGRESO','AJUSTE')),
    cantidad INTEGER NOT NULL,
    fecha TEXT NOT NULL DEFAULT (datetime('now')),
    motivo TEXT NOT NULL CHECK (length(trim(motivo)) > 0),
    id_presupuesto INTEGER,
    CHECK ((tipo_movimiento = 'AJUSTE' AND cantidad <> 0) OR (tipo_movimiento <> 'AJUSTE' AND cantidad > 0)),
    FOREIGN KEY (id_producto) REFERENCES PRODUCTOS(id_producto),
    FOREIGN KEY (id_presupuesto) REFERENCES PRESUPUESTO(id_presupuesto)
);
