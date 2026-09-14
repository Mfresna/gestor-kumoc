CREATE TABLE PRODUCTOS (
    id_producto INTEGER PRIMARY KEY AUTOINCREMENT,
    codigo TEXT NOT NULL UNIQUE,
    nombre TEXT NOT NULL,
    talle TEXT,
    publico_objetivo TEXT,
    color TEXT,
    descripcion TEXT,
    precio_unitario REAL NOT NULL CHECK (precio_unitario >= 0),
    activo INTEGER NOT NULL DEFAULT 1 CHECK (activo IN (1,0)),
    stock_fisico INTEGER NOT NULL DEFAULT 0 CHECK (stock_fisico >= 0),
    stock_comprometido INTEGER NOT NULL DEFAULT 0 CHECK (stock_comprometido >= 0),
    stock_minimo INTEGER NOT NULL DEFAULT 0 CHECK (stock_minimo >= 0),
    imagen TEXT,
    ult_actualizacion TEXT NOT NULL DEFAULT (datetime('now'))
);

CREATE TABLE CLIENTES (
    id_cliente INTEGER PRIMARY KEY AUTOINCREMENT,
    razon_social TEXT NOT NULL,
    cuit TEXT NOT NULL UNIQUE,
    email TEXT,
    telefono TEXT,
    direccion TEXT
);

CREATE TABLE MOVIMIENTOS_STOCK (
    id_movimiento INTEGER PRIMARY KEY AUTOINCREMENT,
    id_producto INTEGER NOT NULL,
    id_presupuesto INTEGER,
    tipo_movimiento TEXT NOT NULL CHECK (tipo_movimiento IN ('ENTRADA','SALIDA','AJUSTE_POS','AJUSTE_NEG','RESERVA','LIBERACION_RESERVA')),
    cantidad INTEGER NOT NULL CHECK (cantidad > 0),
    fecha TEXT NOT NULL DEFAULT (datetime('now')),
    FOREIGN KEY (id_producto) REFERENCES PRODUCTOS(id_producto),
    FOREIGN KEY (id_presupuesto) REFERENCES PRESUPUESTO(id_presupuesto)
);

CREATE TABLE PRESUPUESTO (
    id_presupuesto INTEGER PRIMARY KEY AUTOINCREMENT,
    id_cliente INTEGER NOT NULL,
    fecha_emision TEXT NOT NULL DEFAULT (date('now')),
    estado TEXT NOT NULL DEFAULT 'PRESUPUESTO'
        CHECK (estado IN ('PRESUPUESTO','RECHAZADO','FACTURADO')),
    total REAL NOT NULL DEFAULT 0 CHECK (total >= 0),
    FOREIGN KEY (id_cliente) REFERENCES CLIENTES(id_cliente)
);

CREATE TABLE PRODUCTOSxPRESUPUESTOS (
    id_prodxpres INTEGER PRIMARY KEY AUTOINCREMENT,
    id_presupuesto INTEGER NOT NULL,
    id_producto INTEGER NOT NULL,
    cantidad INTEGER NOT NULL CHECK (cantidad > 0),
    precio_unitario REAL NOT NULL CHECK (precio_unitario > 0),
    subtotal REAL NOT NULL CHECK (subtotal = cantidad * precio_unitario),
    FOREIGN KEY (id_presupuesto) REFERENCES PRESUPUESTO(id_presupuesto),
    FOREIGN KEY (id_producto) REFERENCES PRODUCTOS(id_producto)
);
