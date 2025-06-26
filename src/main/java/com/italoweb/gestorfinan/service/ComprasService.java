package com.italoweb.gestorfinan.service;

import java.time.LocalDateTime;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.Transaction;

import com.italoweb.gestorfinan.model.Inventario;
import com.italoweb.gestorfinan.model.Producto;
import com.italoweb.gestorfinan.model.Proveedor;
import com.italoweb.gestorfinan.model.TipoMovimiento;
import com.italoweb.gestorfinan.model.compra.CompraDetalle;
import com.italoweb.gestorfinan.model.compra.Compras;
import com.italoweb.gestorfinan.util.HibernateUtil;

public class ComprasService {

	@SuppressWarnings("deprecation")
	public void guardarCompraConProductosYMovInv(Compras compras) {
		Session session = HibernateUtil.getSessionFactory().openSession();
		Transaction tx = session.beginTransaction();
		try {
			// 1) Actualiza fecha de vencimiento en cada producto
			for (CompraDetalle det : compras.getDetalles()) {
				if (det.getFechaVencimiento() != null) {
					det.getProducto().setFechaVencimiento(det.getFechaVencimiento().atStartOfDay());
					session.update(det.getProducto());
				}
				det.setCompra(compras);
			}
			session.save(compras);

			registrarMovimientoInventario(session, compras);

			tx.commit();
		} catch (Exception ex) {
			tx.rollback();
			throw ex;
		} finally {
			session.close();
		}
	}

	@SuppressWarnings("deprecation")
	private void registrarMovimientoInventario(Session session, Compras compras) {
		Proveedor proveedor = compras.getProveedor();
		List<CompraDetalle> detalles = compras.getDetalles();

		for (CompraDetalle detalle : detalles) {
			Producto producto = detalle.getProducto();
			// Actualiza precios en el mismo objeto y sesión
			producto.setPrecioCompra(detalle.getPrecioCompra());
			producto.setPrecioVenta(detalle.getPrecioVenta());
			session.update(producto);

			// Crea y persiste el movimiento de inventario
			Inventario movimiento = new Inventario();
			movimiento.setProducto(producto);
			movimiento.setCompra(compras);
			movimiento.getCompra().setProveedor(proveedor);
			movimiento.setCantidadStock(detalle.getCantidadIngresar());
			movimiento.setTipoMovimiento(TipoMovimiento.INGRESOS);
			movimiento.setFecha(LocalDateTime.now());
			movimiento.setObservaciones("Ingreso por compra #" + compras.getId());

			session.save(movimiento);
		}
	}
}
