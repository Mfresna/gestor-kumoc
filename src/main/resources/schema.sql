-- ============================================================
-- SCHEMA: Control de stock chalecos salvavidas
-- Motor: SQLite
-- ============================================================

PRAGMA foreign_keys = ON;

-- ---------------- TABLAS ----------------

CREATE TABLE PRODUCTOS (
    id_producto INTEGER PRIMARY KEY AUTOINCREMENT,
    nombre TEXT NOT NULL,
    talle TEXT,
    publico_objetivo TEXT,
    color TEXT,
    precio_unitario REAL NOT NULL CHECK (precio_unitario >= 0),
    activo INTEGER NOT NULL DEFAULT 1 CHECK (activo IN (1,0)),
    cantidad_actual INTEGER NOT NULL DEFAULT 0 CHECK (cantidad_actual >= 0),
    stock_minimo INTEGER NOT NULL DEFAULT 0,
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

-- cantidad: SIEMPRE positiva. El signo del impacto en stock lo define tipo_movimiento.
CREATE TABLE MOVIMIENTOS_STOCK (
    id_movimiento INTEGER PRIMARY KEY AUTOINCREMENT,
    id_producto INTEGER NOT NULL,
    tipo_movimiento TEXT NOT NULL CHECK (tipo_movimiento IN ('ENTRADA','SALIDA','AJUSTE_POS','AJUSTE_NEG')),
    cantidad INTEGER NOT NULL CHECK (cantidad > 0),
    fecha TEXT NOT NULL DEFAULT (datetime('now')),
    FOREIGN KEY (id_producto) REFERENCES PRODUCTOS(id_producto)
);

CREATE TABLE FACTURAS (
    id_factura INTEGER PRIMARY KEY AUTOINCREMENT,
    id_cliente INTEGER NOT NULL,
    fecha_emision TEXT NOT NULL DEFAULT (date('now')),
    estado TEXT NOT NULL DEFAULT 'PRESUPUESTO'
        CHECK (estado IN ('PRESUPUESTO','RECHAZADO','FACTURADO')),
    total REAL NOT NULL DEFAULT 0 CHECK (total >= 0),
    FOREIGN KEY (id_cliente) REFERENCES CLIENTES(id_cliente)
);

CREATE TABLE PRODUCTOSxFACTURAS (
    id_prodxfactur INTEGER PRIMARY KEY AUTOINCREMENT,
    id_factura INTEGER NOT NULL,
    id_producto INTEGER NOT NULL,
    cantidad INTEGER NOT NULL CHECK (cantidad > 0),
    precio_unitario REAL NOT NULL CHECK (precio_unitario > 0),
    subtotal REAL NOT NULL CHECK (subtotal = cantidad * precio_unitario),
    FOREIGN KEY (id_factura) REFERENCES FACTURAS(id_factura),
    FOREIGN KEY (id_producto) REFERENCES PRODUCTOS(id_producto)
);

-- ---------------- INDICES ----------------

CREATE INDEX idx_mov_producto ON MOVIMIENTOS_STOCK(id_producto);
CREATE INDEX idx_pxf_factura ON PRODUCTOSxFACTURAS(id_factura);
CREATE INDEX idx_pxf_producto ON PRODUCTOSxFACTURAS(id_producto);
CREATE INDEX idx_facturas_cliente ON FACTURAS(id_cliente);

-- ---------------- TRIGGERS: STOCK ----------------

-- ENTRADA: suma stock
CREATE TRIGGER trg_mov_entrada
AFTER INSERT ON MOVIMIENTOS_STOCK
WHEN NEW.tipo_movimiento = 'ENTRADA'
BEGIN
    UPDATE PRODUCTOS
    SET cantidad_actual = cantidad_actual + NEW.cantidad,
        ult_actualizacion = datetime('now')
    WHERE id_producto = NEW.id_producto;
END;

-- SALIDA: resta stock (bloquea si deja negativo)
CREATE TRIGGER trg_mov_salida_check
BEFORE INSERT ON MOVIMIENTOS_STOCK
WHEN NEW.tipo_movimiento = 'SALIDA'
BEGIN
    SELECT CASE
        WHEN (SELECT cantidad_actual FROM PRODUCTOS WHERE id_producto = NEW.id_producto) < NEW.cantidad
        THEN RAISE(ABORT, 'Stock insuficiente para SALIDA')
    END;
END;

CREATE TRIGGER trg_mov_salida
AFTER INSERT ON MOVIMIENTOS_STOCK
WHEN NEW.tipo_movimiento = 'SALIDA'
BEGIN
    UPDATE PRODUCTOS
    SET cantidad_actual = cantidad_actual - NEW.cantidad,
        ult_actualizacion = datetime('now')
    WHERE id_producto = NEW.id_producto;
END;

-- AJUSTE_POS / AJUSTE_NEG: correcciones manuales de inventario
CREATE TRIGGER trg_mov_ajuste_pos
AFTER INSERT ON MOVIMIENTOS_STOCK
WHEN NEW.tipo_movimiento = 'AJUSTE_POS'
BEGIN
    UPDATE PRODUCTOS
    SET cantidad_actual = cantidad_actual + NEW.cantidad,
        ult_actualizacion = datetime('now')
    WHERE id_producto = NEW.id_producto;
END;

CREATE TRIGGER trg_mov_ajuste_neg_check
BEFORE INSERT ON MOVIMIENTOS_STOCK
WHEN NEW.tipo_movimiento = 'AJUSTE_NEG'
BEGIN
    SELECT CASE
        WHEN (SELECT cantidad_actual FROM PRODUCTOS WHERE id_producto = NEW.id_producto) < NEW.cantidad
        THEN RAISE(ABORT, 'Ajuste negativo dejaría stock negativo')
    END;
END;

CREATE TRIGGER trg_mov_ajuste_neg
AFTER INSERT ON MOVIMIENTOS_STOCK
WHEN NEW.tipo_movimiento = 'AJUSTE_NEG'
BEGIN
    UPDATE PRODUCTOS
    SET cantidad_actual = cantidad_actual - NEW.cantidad,
        ult_actualizacion = datetime('now')
    WHERE id_producto = NEW.id_producto;
END;

-- ---------------- TRIGGERS: FACTURAS.total ----------------

CREATE TRIGGER trg_pxf_insert_total
AFTER INSERT ON PRODUCTOSxFACTURAS
BEGIN
    UPDATE FACTURAS
    SET total = (SELECT COALESCE(SUM(subtotal),0) FROM PRODUCTOSxFACTURAS WHERE id_factura = NEW.id_factura)
    WHERE id_factura = NEW.id_factura;
END;

CREATE TRIGGER trg_pxf_update_total
AFTER UPDATE ON PRODUCTOSxFACTURAS
BEGIN
    UPDATE FACTURAS
    SET total = (SELECT COALESCE(SUM(subtotal),0) FROM PRODUCTOSxFACTURAS WHERE id_factura = NEW.id_factura)
    WHERE id_factura = NEW.id_factura;
END;

CREATE TRIGGER trg_pxf_delete_total
AFTER DELETE ON PRODUCTOSxFACTURAS
BEGIN
    UPDATE FACTURAS
    SET total = (SELECT COALESCE(SUM(subtotal),0) FROM PRODUCTOSxFACTURAS WHERE id_factura = OLD.id_factura)
    WHERE id_factura = OLD.id_factura;
END;
