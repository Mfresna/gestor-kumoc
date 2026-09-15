# StockControl

Aplicación de escritorio en Java para control de stock de productos terminados, pensada para pequeñas empresas manufactureras que necesitan gestionar su inventario sin depender de infraestructura en la nube ni de conexión a internet.

Desarrollada como caso de estudio real: control de stock de chalecos salvavidas, con presupuestos, clientes y trazabilidad completa de movimientos de inventario.

## Características

- **ABM de productos**: alta, baja y modificación de productos vendibles (nombre, detalle, precio, stock mínimo).
- **ABM de clientes**: datos de contacto y CUIT/CUIL para la emisión de presupuestos.
- **Presupuestos**: selección de múltiples productos y cantidades, cálculo automático de totales, exportación a PDF.
- **Control de stock por histórico de movimientos**: cada ingreso, egreso o ajuste queda registrado individualmente. El stock actual de un producto se calcula sumando su historial de movimientos, no se almacena como un contador editable — esto garantiza trazabilidad completa y evita inconsistencias.
- **Alertas de stock bajo**: notificación automática cuando el stock calculado de un producto cae por debajo de su umbral mínimo configurado.

## Stack tecnológico

| Capa | Tecnología |
|---|---|
| Lenguaje | Java |
| Interfaz gráfica | JavaFX |
| Persistencia | SQLite (JDBC puro, sin ORM) |
| Generación de PDF | OpenPDF |
| Arquitectura | Capas: DAO → Service → UI |

**Por qué este stack:** aplicación monousuario, sin necesidad de red ni servidor. SQLite ofrece transacciones ACID reales sin la complejidad operativa de un motor cliente-servidor. Se evita un ORM deliberadamente para mantener control explícito sobre las transacciones críticas (descuento de stock, registro de movimientos).

## Modelo de datos

El modelo está compuesto por 5 entidades principales:

- **Cliente**: datos de contacto de quienes reciben presupuestos.
- **Producto**: productos vendibles, con su precio y stock mínimo configurado.
- **Presupuesto**: cabecera de un presupuesto asociado a un cliente.
- **ProductosXPresupuesto**: detalle de ítems (producto, cantidad, precio unitario) de cada presupuesto.
- **MovimientoStock**: histórico de eventos de inventario (`INGRESO`, `EGRESO`, `AJUSTE`) por producto, con fecha y motivo.

El stock actual de un producto **no es un campo persistido**: se obtiene sumando sus movimientos (`INGRESO` suma, `EGRESO` resta, `AJUSTE` según signo). Esto asegura que el número mostrado sea siempre consistente con la historia real de eventos, sin riesgo de desincronización por errores de aplicación o transacciones interrumpidas.

## Arquitectura

```
UI (JavaFX)
   ↓
Service (lógica de negocio: registrarMovimiento, calcularStock, detectarAlertas)
   ↓
DAO (JDBC puro: ProductoDAO, ClienteDAO, PresupuestoDAO, MovimientoStockDAO)
   ↓
SQLite (archivo local .db)
```

Todas las operaciones que modifican stock pasan exclusivamente por la capa de Service, que las envuelve en transacciones (`Connection.setAutoCommit(false)`) para garantizar atomicidad entre el registro del movimiento y cualquier efecto derivado.

## Roadmap

- [x] ABM de productos y clientes
- [x] Registro de movimientos de stock con histórico
- [x] Alertas de stock bajo
- [x] Presupuestos con múltiples ítems y exportación a PDF
- [ ] Facturación interna (sin integración AFIP) a partir de un presupuesto aprobado
- [ ] Reportes de movimientos por rango de fechas
- [ ] Exportación de historial a Excel/CSV

## Cómo ejecutar

```bash
git clone https://github.com/<usuario>/stockcontrol.git
cd stockcontrol
mvn clean javafx:run
```

Requiere JDK 17+ y Maven. La base de datos SQLite se crea automáticamente en el primer arranque.

## Motivación del proyecto

Este proyecto nace de un caso real de control de inventario para una empresa que fabrica y vende chalecos salvavidas. El foco está puesto en decisiones de diseño de datos sólidas (trazabilidad vía histórico de movimientos, separación de responsabilidades en capas) antes que en la cantidad de funcionalidades, priorizando un sistema simple pero correcto y auditable.

## Licencia

MIT
