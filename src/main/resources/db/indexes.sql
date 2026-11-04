-- PRESUPUESTO: listar/filtrar presupuestos por cliente y estado
CREATE INDEX idx_presupuesto_cliente ON PRESUPUESTO (id_cliente);
CREATE INDEX idx_presupuesto_estado ON PRESUPUESTO (estado);

-- PRESUPUESTO: listado de fin de mes (cobradas pendientes de facturar en ARCA)
CREATE INDEX idx_presupuesto_arca ON PRESUPUESTO (facturar_arca, fecha_facturacion_arca);

-- PRODUCTOSxPRESUPUESTOS: detalle de líneas por presupuesto
CREATE INDEX idx_pxp_presupuesto ON PRODUCTOSxPRESUPUESTOS (id_presupuesto);

-- PRODUCTOSxPRESUPUESTOS: historial de ventas/cotizaciones por producto
CREATE INDEX idx_pxp_producto ON PRODUCTOSxPRESUPUESTOS (id_producto);

-- PRODUCTOS: filtrar catálogo por activos/inactivos
CREATE INDEX idx_productos_activo ON PRODUCTOS (activo);

-- MOVIMIENTO_STOCK: cálculo de stock por producto y trazabilidad por presupuesto
CREATE INDEX idx_mov_producto ON MOVIMIENTO_STOCK (id_producto);
CREATE INDEX idx_mov_presupuesto ON MOVIMIENTO_STOCK (id_presupuesto);
