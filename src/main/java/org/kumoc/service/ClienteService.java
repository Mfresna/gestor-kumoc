package org.kumoc.service;

import org.kumoc.dao.ClienteDao;
import org.kumoc.exception.NegocioException;
import org.kumoc.model.Cliente;

import java.util.List;
import java.util.Optional;

public class ClienteService {

    private final ClienteDao clienteDao;

    public ClienteService(ClienteDao clienteDao) {
        this.clienteDao = clienteDao;
    }

    public List<Cliente> listar() {
        return clienteDao.listar();
    }

    public Optional<Cliente> buscarPorId(int idCliente) {
        return clienteDao.buscarPorId(idCliente);
    }

    /** Crea el cliente si idCliente es null; si no, lo actualiza. Devuelve el cliente guardado. */
    public Cliente guardar(Cliente cliente) {

        String razonSocial = Validaciones.textoObligatorio(cliente.razonSocial(), "La razón social es obligatoria.");
        String cuit = normalizarCuit(cliente.cuit());

        if (cliente.tipoFactura() == null) {
            throw new NegocioException("El tipo de factura es obligatorio.");
        }

        clienteDao.buscarPorCuit(cuit).ifPresent(existente -> {
            if (!existente.idCliente().equals(cliente.idCliente())) {
                throw new NegocioException("Ya existe un cliente con ese CUIT/CUIL.");
            }
        });

        Cliente limpio = new Cliente(cliente.idCliente(), razonSocial, cuit, cliente.tipoFactura(),
                Validaciones.vacioANull(cliente.email()), Validaciones.vacioANull(cliente.telefono()),
                Validaciones.vacioANull(cliente.direccion()));

        if (limpio.idCliente() == null) {
            int id = clienteDao.insertar(limpio);
            return new Cliente(id, limpio.razonSocial(), limpio.cuit(), limpio.tipoFactura(),
                    limpio.email(), limpio.telefono(), limpio.direccion());
        }

        if (clienteDao.buscarPorId(limpio.idCliente()).isEmpty()) {
            throw new NegocioException("El cliente a modificar no existe.");
        }
        clienteDao.actualizar(limpio);
        return limpio;
    }

    public void eliminar(int idCliente) {

        if (clienteDao.tienePresupuestos(idCliente)) {
            throw new NegocioException("No se puede eliminar un cliente que tiene presupuestos.");
        }
        clienteDao.eliminar(idCliente);
    }

    /** Acepta CUIT/CUIL con o sin guiones; se guarda solo con los 11 digitos. */
    private String normalizarCuit(String cuit) {

        String digitos = cuit == null ? "" : cuit.replaceAll("[\\s-]", "");
        if (!digitos.matches("\\d{11}")) {
            throw new NegocioException("El CUIT/CUIL debe tener 11 dígitos.");
        }
        return digitos;
    }
}
