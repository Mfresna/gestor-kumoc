-- ============================================================
-- KUMOC - TRIGGERS STOCK
-- Motor: SQLite
-- ============================================================


-- ============================================================
-- ENTRADA
-- ============================================================
-- Una ENTRADA incrementa el stock físico.

CREATE TRIGGER trg_movstock_entrada
AFTER INSERT ON MOVIMIENTOS_STOCK
WHEN NEW.tipo_movimiento = 'ENTRADA'
BEGIN

    UPDATE PRODUCTOS
    SET stock_fisico = stock_fisico + NEW.cantidad,
        ult_actualizacion = datetime('now')
    WHERE id_producto = NEW.id_producto;

END;


-- ============================================================
-- SALIDA - VALIDACION
-- ============================================================
-- Una SALIDA no puede dejar el stock físico por debajo de 0.

CREATE TRIGGER trg_movstock_salida_check
BEFORE INSERT ON MOVIMIENTOS_STOCK
WHEN NEW.tipo_movimiento = 'SALIDA'
BEGIN

    SELECT CASE

        WHEN (
            SELECT stock_fisico
            FROM PRODUCTOS
            WHERE id_producto = NEW.id_producto
        ) < NEW.cantidad

        THEN RAISE(
            ABORT,
            'Stock físico insuficiente para realizar la salida'
        )

    END;

END;


-- ============================================================
-- SALIDA
-- ============================================================
-- Una SALIDA disminuye el stock físico.

CREATE TRIGGER trg_movstock_salida
AFTER INSERT ON MOVIMIENTOS_STOCK
WHEN NEW.tipo_movimiento = 'SALIDA'
BEGIN

    UPDATE PRODUCTOS
    SET stock_fisico = stock_fisico - NEW.cantidad,
        ult_actualizacion = datetime('now')
    WHERE id_producto = NEW.id_producto;

END;


-- ============================================================
-- AJUSTE POSITIVO
-- ============================================================
-- Incrementa manualmente el stock físico.

CREATE TRIGGER trg_movstock_ajuste_pos
AFTER INSERT ON MOVIMIENTOS_STOCK
WHEN NEW.tipo_movimiento = 'AJUSTE_POS'
BEGIN

    UPDATE PRODUCTOS
    SET stock_fisico = stock_fisico + NEW.cantidad,
        ult_actualizacion = datetime('now')
    WHERE id_producto = NEW.id_producto;

END;


-- ============================================================
-- AJUSTE NEGATIVO - VALIDACION
-- ============================================================
-- El ajuste negativo no puede dejar stock físico negativo.

CREATE TRIGGER trg_movstock_ajuste_neg_check
BEFORE INSERT ON MOVIMIENTOS_STOCK
WHEN NEW.tipo_movimiento = 'AJUSTE_NEG'
BEGIN

    SELECT CASE

        WHEN (
            SELECT stock_fisico
            FROM PRODUCTOS
            WHERE id_producto = NEW.id_producto
        ) < NEW.cantidad

        THEN RAISE(
            ABORT,
            'El ajuste negativo dejaría el stock físico en negativo'
        )

    END;

END;


-- ============================================================
-- AJUSTE NEGATIVO
-- ============================================================
-- Disminuye manualmente el stock físico.

CREATE TRIGGER trg_movstock_ajuste_neg
AFTER INSERT ON MOVIMIENTOS_STOCK
WHEN NEW.tipo_movimiento = 'AJUSTE_NEG'
BEGIN

    UPDATE PRODUCTOS
    SET stock_fisico = stock_fisico - NEW.cantidad,
        ult_actualizacion = datetime('now')
    WHERE id_producto = NEW.id_producto;

END;


-- ============================================================
-- RESERVA
-- ============================================================
-- Una RESERVA incrementa el stock comprometido.
--
-- Ejemplo:
--
-- Stock físico:        5
-- Presupuesto:        10
-- Stock comprometido: 10
--
-- Disponible = 5 - 10 = -5
--
-- El -5 significa que faltan 5 unidades para cubrir
-- los compromisos actuales.

CREATE TRIGGER trg_movstock_reserva
AFTER INSERT ON MOVIMIENTOS_STOCK
WHEN NEW.tipo_movimiento = 'RESERVA'
BEGIN

    UPDATE PRODUCTOS
    SET stock_comprometido = stock_comprometido + NEW.cantidad,
        ult_actualizacion = datetime('now')
    WHERE id_producto = NEW.id_producto;

END;


-- ============================================================
-- LIBERACION DE RESERVA - VALIDACION
-- ============================================================
-- No se puede liberar una cantidad superior al stock comprometido.

CREATE TRIGGER trg_movstock_liberacion_reserva_check
BEFORE INSERT ON MOVIMIENTOS_STOCK
WHEN NEW.tipo_movimiento = 'LIBERACION_RESERVA'
BEGIN

    SELECT CASE

        WHEN (
            SELECT stock_comprometido
            FROM PRODUCTOS
            WHERE id_producto = NEW.id_producto
        ) < NEW.cantidad

        THEN RAISE(
            ABORT,
            'No existe suficiente stock comprometido para liberar'
        )

    END;

END;


-- ============================================================
-- LIBERACION DE RESERVA
-- ============================================================
-- Disminuye el stock comprometido.

CREATE TRIGGER trg_movstock_liberacion_reserva
AFTER INSERT ON MOVIMIENTOS_STOCK
WHEN NEW.tipo_movimiento = 'LIBERACION_RESERVA'
BEGIN

    UPDATE PRODUCTOS
    SET stock_comprometido = stock_comprometido - NEW.cantidad,
        ult_actualizacion = datetime('now')
    WHERE id_producto = NEW.id_producto;

END;