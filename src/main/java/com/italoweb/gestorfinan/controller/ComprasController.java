package com.italoweb.gestorfinan.controller;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Session;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.event.InputEvent;
import org.zkoss.zk.ui.util.GenericForwardComposer;
import org.zkoss.zul.Button;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Datebox;
import org.zkoss.zul.Decimalbox;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Window;

import com.italoweb.gestorfinan.model.Estado;
import com.italoweb.gestorfinan.model.MedioPago;
import com.italoweb.gestorfinan.model.Producto;
import com.italoweb.gestorfinan.model.Proveedor;
import com.italoweb.gestorfinan.model.Usuario;
import com.italoweb.gestorfinan.model.compra.CompraDetalle;
import com.italoweb.gestorfinan.model.compra.Compras;
import com.italoweb.gestorfinan.repository.MedioPagoDAO;
import com.italoweb.gestorfinan.repository.ProveedorDAO;
import com.italoweb.gestorfinan.service.ComprasService;
import com.italoweb.gestorfinan.util.ComponentsUtil;
import com.italoweb.gestorfinan.util.DialogUtil;
import com.italoweb.gestorfinan.util.FormatoUtil;

public class ComprasController extends GenericForwardComposer<Component> {

	private static final long serialVersionUID = 1L;

	@SuppressWarnings("unused")
	private Grid gridDetalle;
	private Rows rowsDetalle;
	private Label lblTotal;

	// Campos para datos cabecera Compras
	private Textbox txtNumeroFactura;
	private Textbox text_usuario_vendedor;
	private Combobox comb_Proveedor;
	private Combobox comb_medio_pago;
	private Datebox date_fecha_ingreso;
	private Datebox date_fecha_pago;
	private List<CompraDetalle> listaDetalle = new ArrayList<>();
	private Usuario usuarioSession = new Usuario();
	private MedioPagoDAO medioPagoDAO = new MedioPagoDAO();
	private ProveedorDAO proveedorDAO = new ProveedorDAO();

	private Compras Compras;

	@Override
	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);
		Compras = new Compras();
		Compras.setDetalles(new ArrayList<>());

		actualizarTotales();

		self.addEventListener("onProductoSeleccionado", new EventListener<Event>() {
			@Override
			public void onEvent(Event event) throws Exception {
				Producto productoSeleccionado = (Producto) event.getData();
				System.out.println("Producto recibido: " + productoSeleccionado.getNombre());
				agregarProductoADetalle(productoSeleccionado);
			}
		});
		this.usuarioSession = this.session();
		cargarComponentes();
	}

	public void cargarComponentes() {
		text_usuario_vendedor.setValue(usuarioSession.getUsername().toUpperCase());
		text_usuario_vendedor.setDisabled(true);
		date_fecha_ingreso.setValue(new Date());
		date_fecha_pago.setValue(new Date());
		cargarMedioPago();
		cargarProveedor();
	}

	private Usuario session() {
		Session session = Sessions.getCurrent();
		Object usuario = session.getAttribute("usuario");
		return (Usuario) usuario;
	}

	public void cargarMedioPago() {
		this.comb_medio_pago.setAutocomplete(false);
		List<MedioPago> listaMedioPago = this.medioPagoDAO.findAll();
		for (MedioPago medioPago : listaMedioPago) {
			this.comb_medio_pago.appendChild(ComponentsUtil.getComboitem(medioPago.getNombre(), null, medioPago));
		}
	}

	public void cargarProveedor() {
		this.comb_Proveedor.setAutocomplete(false);
		List<Proveedor> listaProveedores = this.proveedorDAO.filtroEstadoActivo(Estado.ACTIVO);
		for (Proveedor Proveedor : listaProveedores) {
			this.comb_Proveedor.appendChild(ComponentsUtil
					.getComboitem("[" + Proveedor.getNit() + "]" + " " + Proveedor.getNombre(), null, Proveedor));
		}
	}

	public void onChanging$comb_medio_pago(InputEvent event) {
		String filtro = event.getValue();
		filtrarCombo(comb_medio_pago, filtro);
	}

	public void onChanging$comb_Proveedor(InputEvent event) {
		String filtro = event.getValue();
		filtrarCombo(comb_Proveedor, filtro);
	}

	public void filtrarCombo(Combobox combo, String filter) {
		filter = filter.trim().toUpperCase();
		for (Comboitem comboItem : combo.getItems()) {
			String label = comboItem.getLabel();
			comboItem.setVisible(filter.isEmpty() || label.toUpperCase().contains(filter));
		}
	}

	/** Acción del botón "Agregar Producto" */
	public void onClick$btnAgregarProducto() {
		Map<String, Object> params = new HashMap<>();
		params.put("padre", self);
		params.put("tipo", "C");

		Window modal = (Window) Executions.createComponents("/views/detalles/agregarProductoDet.zul", self, params);
		modal.doModal();
	}

	private void agregarProductoADetalle(Producto producto) {
		// Verificar si el producto ya está en la lista
		boolean productoYaExiste = listaDetalle.stream()
				.anyMatch(detalle -> detalle.getProducto().getId().equals(producto.getId()));
		if (productoYaExiste) {
			Messagebox.show("El producto '" + producto.getNombre() + "' ya fue agregado.", "Producto duplicado",
					Messagebox.OK, Messagebox.EXCLAMATION);
			return;
		}
		CompraDetalle detalle = new CompraDetalle();
		detalle.setProducto(producto);
		detalle.setCantidadIngresar(1);
		detalle.setPrecioCompra(producto.getPrecioCompra());
		detalle.setPrecioVenta(calcularPrecio(producto));
		detalle.setFechaVencimiento(null);
		detalle.setTotalCompra(detalle.getPrecioVenta().multiply(BigDecimal.valueOf(detalle.getCantidadIngresar())));

		detalle.setCompra(Compras);

		Compras.getDetalles().add(detalle);
		renderFilaDetalle(detalle);
		actualizarTotales();
	}

	private void renderFilaDetalle(CompraDetalle detalle) {
		Row row = new Row();

		row.appendChild(new Label(detalle.getProducto().getCodigo()));
		row.appendChild(new Label(detalle.getProducto().getNombre()));

		Decimalbox decPrecioCompras = new Decimalbox(detalle.getPrecioCompra());
		decPrecioCompras.setWidth("120px");
		decPrecioCompras.setConstraint("no empty");
		decPrecioCompras.setFormat("#,##0.00"); // Formato con separadores y 2 decimales
		decPrecioCompras.setInplace(false);
		row.appendChild(decPrecioCompras);

		Decimalbox decPrecioVenta = new Decimalbox(detalle.getPrecioVenta());
		decPrecioVenta.setWidth("120px");
		decPrecioVenta.setConstraint("no empty");
		decPrecioVenta.setFormat("#,##0.00");
		decPrecioVenta.setInplace(false);
		row.appendChild(decPrecioVenta);

		Intbox intCantidad = new Intbox(detalle.getCantidadIngresar());
		intCantidad.setWidth("60px");
		intCantidad.setInplace(false);
		row.appendChild(intCantidad);

		Datebox dtFechaVencimiento = new Datebox();
		dtFechaVencimiento.setWidth("180px");
		dtFechaVencimiento.setValue(FormatoUtil.convertirALocalDateDate(detalle.getFechaVencimiento()));
		row.appendChild(dtFechaVencimiento);

		Label lblTotalFila = new Label("$ " + FormatoUtil.formatDecimal(detalle.getTotalCompra()));
		row.appendChild(lblTotalFila);

		dtFechaVencimiento.addEventListener(Events.ON_CHANGE, e -> {
			Date fechaSeleccionada = dtFechaVencimiento.getValue();
			if (fechaSeleccionada != null) {
				detalle.setFechaVencimiento(fechaSeleccionada.toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
			} else {
				detalle.setFechaVencimiento(null);
			}
		});
		// Listener para cambios en precio Compras
		decPrecioCompras.addEventListener("onChange", e -> {
			BigDecimal nuevoPrecioCompras = decPrecioCompras.getValue();
			if (nuevoPrecioCompras != null && nuevoPrecioCompras.compareTo(BigDecimal.ZERO) >= 0) {
				detalle.setPrecioCompra(nuevoPrecioCompras);
				recalcularTotal(detalle, intCantidad.getValue(), nuevoPrecioCompras, lblTotalFila);
				actualizarTotales();
			} else {
				DialogUtil.showError("El precio de Compras debe ser igual o mayor a cero.");
				decPrecioCompras.setValue(detalle.getPrecioCompra());
			}
		});

		// Listener para cambios en precio venta
		decPrecioVenta.addEventListener("onChange", e -> {
			BigDecimal nuevoPrecioVenta = decPrecioVenta.getValue();
			if (nuevoPrecioVenta != null && nuevoPrecioVenta.compareTo(BigDecimal.ZERO) >= 0) {
				detalle.setPrecioVenta(nuevoPrecioVenta);
				recalcularTotal(detalle, intCantidad.getValue(), decPrecioCompras.getValue(), lblTotalFila);
				actualizarTotales();
			} else {
				DialogUtil.showError("El precio de venta debe ser igual o mayor a cero.");
				decPrecioVenta.setValue(detalle.getPrecioVenta());
			}
		});

		// Listener para cambios en cantidad
		intCantidad.addEventListener("onChange", e -> {
			Integer nuevaCantidad = intCantidad.getValue();
			if (nuevaCantidad != null && nuevaCantidad > 0) {
				detalle.setCantidadIngresar(nuevaCantidad);
				recalcularTotal(detalle, nuevaCantidad, decPrecioCompras.getValue(), lblTotalFila);
				actualizarTotales();
			} else {
				DialogUtil.showError("La cantidad debe ser mayor a cero.");
				intCantidad.setValue(detalle.getCantidadIngresar());
			}
		});

		// Botón eliminar fila
		Button btnEliminar = new Button("Eliminar");
		btnEliminar.addEventListener("onClick", e -> {
			Compras.getDetalles().remove(detalle);
			rowsDetalle.removeChild(row);
			actualizarTotales();
		});
		row.appendChild(btnEliminar);

		rowsDetalle.appendChild(row);
	}

	private void recalcularTotal(CompraDetalle detalle, Integer cantidad, BigDecimal precioCompra, Label lblTotalFila) {
		if (cantidad == null || precioCompra == null) {
			lblTotalFila.setValue("$ 0.00");
			return;
		}
		BigDecimal total = precioCompra.multiply(BigDecimal.valueOf(cantidad));
		detalle.setCantidadIngresar(cantidad);
		detalle.setTotalCompra(total);
		lblTotalFila.setValue("$ " + FormatoUtil.formatDecimal(total));
	}

	private BigDecimal calcularPrecio(Producto producto) {
		LocalDateTime ahora = LocalDateTime.now();

		if (producto.getFechaIniciaDescuento() != null && producto.getFechaFinalDescuento() != null) {
			if (!ahora.isBefore(producto.getFechaIniciaDescuento()) && !ahora.isAfter(producto.getFechaFinalDescuento())
					&& producto.getDescuento() != null) {
				return producto.getDescuento();
			}
		}

		return producto.getPrecioVenta() != null ? producto.getPrecioVenta() : BigDecimal.ZERO;
	}

	private void actualizarTotales() {
		BigDecimal total = Compras.getDetalles().stream().map(detalle -> {
			BigDecimal precio = detalle.getPrecioCompra() != null ? detalle.getPrecioCompra() : BigDecimal.ZERO;
			BigDecimal cantidad = BigDecimal.valueOf(detalle.getCantidadIngresar());
			return precio.multiply(cantidad);
		}).reduce(BigDecimal.ZERO, BigDecimal::add);

		lblTotal.setValue("$ " + FormatoUtil.formatDecimal(total));
	}

	public void onClick$btnGuardar() {
		// 1) Validaciones previas
		if (!validarCabecera()) {
			return;
		}
		if (Compras.getDetalles().isEmpty()) {
			DialogUtil.showError("Debe agregar al menos un producto a la compra.");
			return;
		}

		// 2) Rellenar datos de la cabecera
		Compras.setNumeroFactura(txtNumeroFactura.getValue());
		Compras.setFechaIngreso(FormatoUtil.convertirADateLocal(date_fecha_ingreso.getValue()));
		Compras.setFechaPago(FormatoUtil.convertirADateLocal(date_fecha_pago.getValue()));

		Comboitem itemProveedor = comb_Proveedor.getSelectedItem();
		Comboitem itemMedioPago = comb_medio_pago.getSelectedItem();
		if (itemProveedor != null) {
			Compras.setProveedor((Proveedor) itemProveedor.getValue());
		}
		if (itemMedioPago != null) {
			Compras.setMedioPago((MedioPago) itemMedioPago.getValue());
		}

		// 3) Asociar cada detalle a la compra (y fechas de vencimiento quedan en el
		// objeto)
		for (CompraDetalle detalle : Compras.getDetalles()) {
			detalle.setCompra(Compras);
			// opcional: ajustar aquí la fecha de vencimiento en detalle si la necesitas
			// antes
		}

		// 4) Llamar al servicio que agrupa toda la lógica de Hibernate
		try {
			ComprasService service = new ComprasService();
			service.guardarCompraConProductosYMovInv(Compras);

			Messagebox.show("Compra guardada exitosamente.", "Éxito", Messagebox.OK, Messagebox.INFORMATION,
					evt -> limpiarPantallaCompra());
		} catch (Exception e) {
			e.printStackTrace();
			Messagebox.show("Error al guardar la compra: " + e.getMessage(), "Error", Messagebox.OK, Messagebox.ERROR);
		}
	}

	private boolean validarCabecera() {
		if (comb_Proveedor.getSelectedItem() == null) {
			DialogUtil.showError("Debe seleccionar un proveedor.");
			return false;
		}
		if (txtNumeroFactura.getValue() == null || txtNumeroFactura.getValue().trim().isEmpty()) {
			DialogUtil.showError("Debe ingresar el número de factura.");
			return false;
		}
		if (date_fecha_pago.getValue() == null) {
			DialogUtil.showError("Debe ingresar la fecha de pago.");
			return false;
		}
		if (comb_medio_pago.getSelectedItem() == null) {
			DialogUtil.showError("Debe seleccionar un medio de pago.");
			return false;
		}
		return true;
	}

	private void limpiarPantallaCompra() {
		Compras.setProveedor(null);
		Compras.setNumeroFactura(null);
		Compras.setFechaIngreso(null);
		Compras.setFechaPago(null);
		Compras.setMedioPago(null);

		if (Compras.getDetalles() != null) {
			Compras.getDetalles().clear();
		}
		rowsDetalle.getChildren().clear();
		txtNumeroFactura.setValue("");
		date_fecha_ingreso.setValue(new Date());
		date_fecha_pago.setValue(new Date());
		comb_Proveedor.setSelectedItem(null);
		comb_medio_pago.setSelectedItem(null);

		lblTotal.setValue("$ 0.00");
	}

}
