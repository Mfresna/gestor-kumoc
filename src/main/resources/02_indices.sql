-- ---------------- MOVIMIENTOS_STOCK ----------------

-- Consultar movimientos de un producto
CREATE INDEX idx_movstock_producto
    ON MOVIMIENTOS_STOCK(id_producto);

-- Consultar movimientos asociados a un presupuesto
CREATE INDEX idx_movstock_presupuesto
    ON MOVIMIENTOS_STOCK(id_presupuesto);

-- Consultar/ordenar movimientos por fecha
CREATE INDEX idx_movstock_fecha
    ON MOVIMIENTOS_STOCK(fecha);


-- ---------------- PRESUPUESTO ----------------

-- Consultar presupuestos de un cliente
CREATE INDEX idx_presupuesto_cliente
    ON PRESUPUESTO(id_cliente);

-- Consultar presupuestos por estado
CREATE INDEX idx_presupuesto_estado
    ON PRESUPUESTO(estado);

-- Consultar/ordenar presupuestos por fecha
CREATE INDEX idx_presupuesto_fecha
    ON PRESUPUESTO(fecha_emision);


-- ---------------- PRODUCTOSxPRESUPUESTOS ----------------

-- Obtener los productos de un presupuesto
CREATE INDEX idx_pxp_presupuesto
    ON PRODUCTOSxPRESUPUESTOS(id_presupuesto);

-- Obtener los presupuestos de un producto
CREATE INDEX idx_pxp_producto
    ON PRODUCTOSxPRESUPUESTOS(id_producto);