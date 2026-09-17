-- PRESUPUESTO: listar/filtrar presupuestos por cliente
CREATE INDEX idx_presupuesto_cliente ON PRESUPUESTO (id_cliente);
CREATE INDEX idx_presupuesto_estado ON PRESUPUESTO (estado);

-- PRODUCTOSxPRESUPUESTOS: detalle de líneas por presupuesto
CREATE INDEX idx_pxp_presupuesto ON PRODUCTOSxPRESUPUESTOS (id_presupuesto);

-- PRODUCTOSxPRESUPUESTOS: historial de ventas/cotizaciones por producto
CREATE INDEX idx_pxp_producto ON PRODUCTOSxPRESUPUESTOS (id_producto);

-- PRODUCTOS: filtrar catálogo por activos/inactivos
CREATE INDEX idx_productos_activo ON PRODUCTOS (activo);
CREATE INDEX idx_productos_stock_fisico ON PRODUCTOS (stock_fisico);
CREATE INDEX idx_productos_stock_comprometido ON PRODUCTOS (stock_comprometido);