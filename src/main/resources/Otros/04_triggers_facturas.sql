-- ============================================================
-- KUMOC - TRIGGERS PRESUPUESTO
-- Motor: SQLite
-- ============================================================


-- ============================================================
-- INSERTAR PRODUCTO EN PRESUPUESTO
-- ============================================================
-- Recalcula el total del presupuesto.

CREATE TRIGGER trg_pxp_insert_total
AFTER INSERT ON PRODUCTOSxPRESUPUESTOS
BEGIN

    UPDATE PRESUPUESTO

    SET total = (
        SELECT COALESCE(SUM(subtotal), 0)
        FROM PRODUCTOSxPRESUPUESTOS
        WHERE id_presupuesto = NEW.id_presupuesto
    )

    WHERE id_presupuesto = NEW.id_presupuesto;

END;


-- ============================================================
-- MODIFICAR PRODUCTO DEL PRESUPUESTO
-- ============================================================
-- Recalcula el total después de modificar un detalle.

CREATE TRIGGER trg_pxp_update_total
AFTER UPDATE ON PRODUCTOSxPRESUPUESTOS
BEGIN

    UPDATE PRESUPUESTO

    SET total = (
        SELECT COALESCE(SUM(subtotal), 0)
        FROM PRODUCTOSxPRESUPUESTOS
        WHERE id_presupuesto = NEW.id_presupuesto
    )

    WHERE id_presupuesto = NEW.id_presupuesto;

END;


-- ============================================================
-- ELIMINAR PRODUCTO DEL PRESUPUESTO
-- ============================================================
-- Recalcula el total después de eliminar un detalle.

CREATE TRIGGER trg_pxp_delete_total
AFTER DELETE ON PRODUCTOSxPRESUPUESTOS
BEGIN

    UPDATE PRESUPUESTO

    SET total = (
        SELECT COALESCE(SUM(subtotal), 0)
        FROM PRODUCTOSxPRESUPUESTOS
        WHERE id_presupuesto = OLD.id_presupuesto
    )

    WHERE id_presupuesto = OLD.id_presupuesto;

END;