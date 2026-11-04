# REQUISITOS – KUMOC

Kumoc fabrica chalecos salvavidas de distintos modelos, además de otras prendas (camperas, buzos, etc.).
El software es una aplicación de escritorio monousuario para **control de stock de chalecos**, **presupuestos** y **órdenes de trabajo**, con trazabilidad completa de los movimientos de inventario.

## Requisitos funcionales

### Productos
- **RF-01** ABM de productos vendibles: nombre, descripción, talle, color, precio y stock mínimo. La baja es lógica (`activo`).

### Clientes
- **RF-02** ABM de clientes: nombre/razón social, CUIT/CUIL, tipo de factura (A, B, C, X) y teléfono de contacto. Información mínima.

### Stock
- **RF-03** Cada ingreso, egreso o ajuste de inventario se registra como un movimiento individual (tipo, cantidad, fecha y motivo).
- **RF-04** El stock actual de un producto se calcula a partir de su historial de movimientos; no existe un contador editable.
- **RF-05** Alerta automática cuando el stock calculado de un producto cae por debajo de su stock mínimo.
- **RF-14** El sistema muestra por producto el stock físico, el comprometido por órdenes de trabajo y el disponible.

### Presupuestos
- **RF-06** ABM de presupuestos: selección de múltiples productos y cantidades, con cálculo automático del total.
- **RF-07** Exportación del presupuesto a PDF.

### Órdenes de trabajo y ventas
- **RF-08** Un presupuesto aprobado pasa a **orden de trabajo**. Corresponde a ventas en las que hay que fabricar total o parcialmente los chalecos.
- **RF-09** Mientras la orden tiene faltantes, el stock necesario queda **congelado** hasta que se fabrique lo faltante.
- **RF-10** Una vez cubiertos los faltantes, la orden pasa a ser una **venta registrada**.
- **RF-11** La orden queda **pendiente de pago**. El pago se realiza fuera de la aplicación; el sistema solo registra que se cobró.
- **RF-12** Al registrar el pago, el sistema pregunta si la orden queda con el flag **"Facturar en ARCA"** (campo en la BD).
- **RF-13** A fin de mes se listan las órdenes cobradas con el flag "Facturar en ARCA" pendiente de facturar.
- **RF-15** Se puede marcar una orden como facturada en ARCA (fecha de facturación), lo que la saca del listado de RF-13.
- **RF-16** Al pasar una orden a venta registrada se generan los egresos de stock de cada ítem.
- **RF-17** La fabricación de faltantes se registra como ingreso de stock con motivo "Fabricación", vinculado a la orden.

## Requisitos no funcionales
- **RNF-01** Aplicación de escritorio monousuario, sin red ni servidor.
- **RNF-02** Persistencia local en SQLite con transacciones ACID en las operaciones críticas (movimientos de stock).
- **RNF-03** Arquitectura MVC.

## Pendientes de definir
- Regla exacta de la transición orden de trabajo → venta (ver `DECISIONES_TECNICAS.md`).
- Formato y contenido del PDF del presupuesto.
- Forma de la alerta de stock bajo (aviso al abrir, indicador en pantalla, etc.).
