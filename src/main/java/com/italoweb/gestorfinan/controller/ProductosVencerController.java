package com.italoweb.gestorfinan.controller;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;

import com.italoweb.gestorfinan.model.Producto;
import com.italoweb.gestorfinan.repository.ProductoDAO;

public class ProductosVencerController extends SelectorComposer<Component> {
	private static final long serialVersionUID = 1L;

	@Wire
	private Grid gridSemanaActual;

	@Wire
	private Grid gridSemanaSiguiente;

	private final ProductoDAO productoDAO = new ProductoDAO();

	@Override
	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);
		cargarProductosPorVencer();
		boolean esMovil = Executions.getCurrent().getBrowser("mobile") != null;
		if (esMovil) {
			hideColumn(gridSemanaActual, 1);
			hideColumn(gridSemanaActual, 3);
			hideColumn(gridSemanaSiguiente, 1);
			hideColumn(gridSemanaSiguiente, 3);
		}
	}

	private void hideColumn(Grid grid, int colIndex) {
		// oculta encabezado
		grid.getColumns().getChildren().get(colIndex).setVisible(false);
		// oculta cada celda de esa columna
		for (Component c : grid.getRows().getChildren()) {
			((Row) c).getChildren().get(colIndex).setVisible(false);
		}
	}

	private void cargarProductosPorVencer() {
		// Limpiar tablas
		Rows rowsActual = gridSemanaActual.getRows();
		Rows rowsSiguiente = gridSemanaSiguiente.getRows();
		rowsActual.getChildren().clear();
		rowsSiguiente.getChildren().clear();

		// Consultar productos
		List<Producto> listaActual = productoDAO.obtenerPorVencerSemanaActual();
		List<Producto> listaSiguiente = productoDAO.obtenerPorVencerSemanaSiguiente();

		// Llenar semana actual
		for (Producto p : listaActual) {
			Row row = new Row();
			LocalDate venc = p.getFechaVencimiento().toLocalDate();
			long diasRestan = ChronoUnit.DAYS.between(LocalDate.now(), venc);
			row.appendChild(new Label(venc.toString()));
			row.appendChild(new Label(p.getCodigo()));
			row.appendChild(new Label(p.getNombre()));
			row.appendChild(new Label(String.valueOf(diasRestan)));
			rowsActual.appendChild(row);
		}

		// Llenar semana siguiente
		for (Producto p : listaSiguiente) {
			Row row = new Row();
			LocalDate venc = p.getFechaVencimiento().toLocalDate();
			long diasRestan = ChronoUnit.DAYS.between(LocalDate.now(), venc);
			row.appendChild(new Label(venc.toString()));
			row.appendChild(new Label(p.getCodigo()));
			row.appendChild(new Label(p.getNombre()));
			row.appendChild(new Label(String.valueOf(diasRestan)));
			rowsSiguiente.appendChild(row);
		}
	}
}
