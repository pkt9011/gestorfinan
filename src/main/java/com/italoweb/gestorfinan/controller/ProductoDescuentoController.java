package com.italoweb.gestorfinan.controller;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.util.GenericForwardComposer;
import org.zkoss.zul.Button;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Datebox;
import org.zkoss.zul.Decimalbox;
import org.zkoss.zul.Div;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Label;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Window;

import com.italoweb.gestorfinan.model.ParametrosGenerales;
import com.italoweb.gestorfinan.model.Producto;
import com.italoweb.gestorfinan.model.ProductoDescuento;
import com.italoweb.gestorfinan.repository.ParametrosGeneralesDAO;
import com.italoweb.gestorfinan.repository.ProductoDAO;
import com.italoweb.gestorfinan.repository.ProductoDescuentoDAO;
import com.italoweb.gestorfinan.util.ComponentsUtil;
import com.italoweb.gestorfinan.util.DialogUtil;
import com.italoweb.gestorfinan.util.FormatoUtil;

public class ProductoDescuentoController extends GenericForwardComposer<Component> {

	private static final long serialVersionUID = 1L;
	private Combobox productoCmb;
	private Decimalbox descuentoBox;
	private Datebox fechaIniciaBox;
	private Datebox fechaFinalizaBox;
	private Grid descuentosGrid;
	private LocalDate fechaActualSistema = LocalDate.now();

	private ProductoDAO productoDAO = new ProductoDAO();
	private ProductoDescuentoDAO productoDescuentoDAO = new ProductoDescuentoDAO();
	private ParametrosGeneralesDAO parametrosGeneralesDAO = new ParametrosGeneralesDAO();
	private List<ProductoDescuento> descuentos;
	private ParametrosGenerales parametro;

	@Override
	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);
		productoCmb.setDisabled(true);
		self.addEventListener("onProductoSeleccionado", new EventListener<Event>() {
			@Override
			public void onEvent(Event event) {
				Producto productoSeleccionado = (Producto) event.getData();
				System.out.println("Producto recibido: " + productoSeleccionado.getNombre());
				cargarProducto(productoSeleccionado);
			}
		});
		cargarDescuentos();
		descuentoBox.setFormat("##.00");
		descuentoBox.setConstraint(ComponentsUtil.porcentajeConstraint());
		ComponentsUtil.agregarValidadorPorcentajeEnVivo(descuentoBox);
	}

	public void onClick$btnAgregarProducto() {
		Map<String, Object> params = new HashMap<>();
		params.put("padre", self);
		params.put("tipo", "D");

		Window modal = (Window) Executions.createComponents("/views/detalles/agregarProductoDet.zul", self, params);
		modal.doModal();
	}

	public void cargarProducto(Producto producto) {
		System.out.println("ejecutando: cargarProducto");

		boolean productoYaExiste = productoCmb.getItems().stream().map(item -> (Producto) item.getValue())
				.anyMatch(p -> p != null && p.getId().equals(producto.getId()));

		if (productoYaExiste) {
			Messagebox.show("El producto '" + producto.getNombre() + "' ya fue agregado a la lista.",
					"Producto duplicado", Messagebox.OK, Messagebox.EXCLAMATION);
			return;
		}
		if (productoDescuentoDAO.obtenerDescuentoPorIdProducto(producto.getId()).isPresent()) {
			DialogUtil.showError("El producto '" + producto.getNombre()
					+ "' ya tiene un descuento asignado. Por favor edítelo o elija otro producto.");
			return;
		}
		Comboitem nuevoItem = new Comboitem("[" + producto.getCodigo() + "] " + producto.getNombre());
		nuevoItem.setValue(producto);
		productoCmb.appendChild(nuevoItem);
		productoCmb.setSelectedItem(nuevoItem);
		productoCmb.setDisabled(false);
	}

	private void cargarDescuentos() {
		descuentos = productoDescuentoDAO.findAll();
		renderizarDescuentos();
	}

	private void renderizarDescuentos() {
		Rows rows = descuentosGrid.getRows();
		rows.getChildren().clear();

		for (ProductoDescuento pd : descuentos) {
			Row row = new Row();

			// Columna: Nombre producto
			row.appendChild(new Label(pd.getProducto().getNombre()));

			// Columna: Descuento %
			row.appendChild(new Label(pd.getDescuento().toString() + " %"));

			// Columna: Fecha inicio
			row.appendChild(new Label(pd.getFechaInicia().toLocalDate().toString()));

			// Columna: Fecha finaliza
			row.appendChild(new Label(pd.getFechaFinaliza().toLocalDate().toString()));

			// Acciones
			Div divBtns = new Div();
			divBtns.setSclass("btn-group");

			Button btnEdit = new Button();
			btnEdit.setSclass("btn btn-primary");
			btnEdit.setIconSclass("z-icon-pencil");
			btnEdit.addEventListener(Events.ON_CLICK, e -> habilitarEdicionEnFila(row, pd));

			Button btnDelete = new Button();
			btnDelete.setSclass("btn btn-danger");
			btnDelete.setIconSclass("z-icon-trash-o");
			btnDelete.addEventListener(Events.ON_CLICK, e -> eliminarDescuento(pd));

			divBtns.appendChild(btnEdit);
			divBtns.appendChild(btnDelete);

			row.appendChild(divBtns);

			rows.appendChild(row);
		}
	}

	private void habilitarEdicionEnFila(Row row, ProductoDescuento pd) {
		row.getChildren().clear();

		// Deshabilita botones de otras filas
		setBotonesHabilitadosEnTodasLasFilas(false);

		// Columna: Nombre producto (solo lectura)
		row.appendChild(new Label(pd.getProducto().getNombre()));

		// Columna: Descuento %
		Decimalbox dbDescuento = new Decimalbox(pd.getDescuento());
		dbDescuento.setWidth("120px");
		dbDescuento.setFormat("##0.00");
		dbDescuento.setConstraint("no empty");
		dbDescuento.setConstraint(ComponentsUtil.porcentajeConstraint());
		ComponentsUtil.agregarValidadorPorcentajeEnVivo(dbDescuento);
		row.appendChild(dbDescuento);

		// Fecha actual
		Date fechaActual = new Date();

		// Columna: Fecha inicio
		Datebox dbFechaInicio = new Datebox(fechaActual);
		dbFechaInicio.setWidth("150px");
		row.appendChild(dbFechaInicio);

		// Columna: Fecha finaliza
		Datebox dbFechaFinal = new Datebox(fechaActual);
		dbFechaFinal.setWidth("150px");
		row.appendChild(dbFechaFinal);

		// Acciones: Confirmar y cancelar
		Div divBtns = new Div();
		divBtns.setSclass("btn-group");

		Button btnConfirmar = new Button();
		btnConfirmar.setSclass("btn btn-success");
		btnConfirmar.setIconSclass("z-icon-check");
		btnConfirmar.addEventListener(Events.ON_CLICK, e -> {
			pd.setDescuento(dbDescuento.getValue());
			pd.setFechaInicia(dbFechaInicio.getValue().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
			pd.setFechaFinaliza(dbFechaFinal.getValue().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
			if (!obtenerPrecioVentaConDescuento(pd, pd.getProducto())) {

				LocalDate fechaInicia = pd.getFechaInicia().toLocalDate();
				if (fechaInicia.isBefore(fechaActualSistema)) {
					DialogUtil.showError("La fecha inicial no puede ser anterior a la fecha actual.");
					return;
				} else {
					productoDescuentoDAO.update(pd);
					Messagebox.show("Descuento actualizado correctamente.");
					setBotonesHabilitadosEnTodasLasFilas(true);
					renderizarDescuentos();
				}
			}
		});

		Button btnCancelar = new Button();
		btnCancelar.setSclass("btn btn-secondary");
		btnCancelar.setIconSclass("z-icon-times");
		btnCancelar.addEventListener(Events.ON_CLICK, e -> {
			setBotonesHabilitadosEnTodasLasFilas(true);
			renderizarDescuentos();
		});

		divBtns.appendChild(btnConfirmar);
		divBtns.appendChild(btnCancelar);

		row.appendChild(divBtns);
	}

	private void setBotonesHabilitadosEnTodasLasFilas(boolean habilitado) {
		for (Component comp : descuentosGrid.getRows().getChildren()) {
			if (comp instanceof Row) {
				Row r = (Row) comp;
				for (Component c : r.getChildren()) {
					if (c instanceof Div) {
						Div divBtns = (Div) c;
						for (Component btnComp : divBtns.getChildren()) {
							if (btnComp instanceof Button) {
								((Button) btnComp).setDisabled(!habilitado);
							}
						}
					}
				}
			}
		}
	}

	public void onClick$guardarDescuento() {
		if (productoCmb.getSelectedItem() == null) {
			DialogUtil.showError("Debe seleccionar un producto.");
			return;
		}
		if (fechaIniciaBox.getValue() == null || fechaFinalizaBox.getValue() == null) {
			DialogUtil.showError("Debe ingresar ambas fechas.");
			return;
		}
		if (fechaFinalizaBox.getValue().before(fechaIniciaBox.getValue())) {
			DialogUtil.showError("La fecha finaliza debe ser mayor o igual a la fecha de inicio.");
			return;
		}

		LocalDate fechaInicia = fechaIniciaBox.getValue().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();

		if (fechaInicia.isBefore(fechaActualSistema)) {
			DialogUtil.showError("La fecha inicial no puede ser anterior a la fecha actual.");
			return;
		}

		Producto producto = productoCmb.getSelectedItem().getValue();

		ProductoDescuento pd = new ProductoDescuento();
		pd.setProducto(producto);
		pd.setDescuento(descuentoBox.getValue());
		pd.setFechaInicia(
				fechaIniciaBox.getValue().toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime());
		pd.setFechaFinaliza(
				fechaFinalizaBox.getValue().toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime());
		if (!validaCodigoProducto(pd)) {
			if (!obtenerPrecioVentaConDescuento(pd, producto)) {
				try {
					productoDescuentoDAO.save(pd);
					Messagebox.show("Descuento guardado correctamente.");
					limpiarCampos();
					cargarDescuentos();
					Executions.sendRedirect(null);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	public boolean validaCodigoProducto(ProductoDescuento descuento) {
		try {
			if (productoDescuentoDAO.obtenerDescuentoPorIdProducto(descuento.getProducto().getId()).isPresent()) {
				DialogUtil.showError("El producto '" + descuento.getProducto().getNombre()
						+ "' ya tiene un descuento asignado. Por favor edítelo o elija otro producto.");
				return true;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	public boolean obtenerPrecioVentaConDescuento(ProductoDescuento descuento, Producto producto) {
		BigDecimal factorPorcentaje = descuento.getDescuento().divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
		BigDecimal descuentoVenta = producto.getPrecioVenta().multiply(factorPorcentaje).setScale(2,
				RoundingMode.HALF_UP);
		BigDecimal precioVentaFinal = producto.getPrecioVenta().subtract(descuentoVenta).setScale(2,
				RoundingMode.HALF_UP);
		if (precioVentaFinal.compareTo(producto.getPrecioCompra()) < 0) {
			parametro = parametrosGeneralesDAO.obtenerUnicoRegistro();
			if (parametro != null) {
				if (parametro.getHabilitaDescuentoTotal().equalsIgnoreCase("N")) {
					DialogUtil.showError("El descuento no es válido: el precio final ("
							+ FormatoUtil.formatDecimal(precioVentaFinal) + ") es menor al precio de compra ("
							+ FormatoUtil.formatDecimal(producto.getPrecioCompra()) + ").");
					return true;
				}
			}
		}
		return false;
	}

	public void onClick$limpiarCampos() {
		limpiarCampos();
	}

	public void actualizaProducto(Producto producto) {
		productoDAO.update(producto);
	}

	private void limpiarCampos() {
		productoCmb.setSelectedItem(null);
		descuentoBox.setValue(new BigDecimal(0));
		fechaIniciaBox.setValue(null);
		fechaFinalizaBox.setValue(null);
	}

	private void eliminarDescuento(ProductoDescuento pd) {
		DialogUtil.showConfirmDialog("¿Está seguro de que desea eliminar este registro?", "Confirmación")
				.thenAccept(confirmed -> {
					if (confirmed) {
						productoDescuentoDAO.delete(pd);
						cargarDescuentos();
						DialogUtil.showShortMessage("success", "Descuento Eliminado Exitosamente");
					}
				});
	}
}
