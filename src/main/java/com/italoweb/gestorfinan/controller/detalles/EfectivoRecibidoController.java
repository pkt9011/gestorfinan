package com.italoweb.gestorfinan.controller.detalles;

import java.math.BigDecimal;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.util.GenericForwardComposer;
import org.zkoss.zul.Decimalbox;
import org.zkoss.zul.Window;

import com.italoweb.gestorfinan.util.DialogUtil;
import com.italoweb.gestorfinan.util.FormatoUtil;

public class EfectivoRecibidoController extends GenericForwardComposer<Component> {

	private static final long serialVersionUID = 1L;
	private Window win_valida_efectivo;
	private Decimalbox debx_efectivo;
	private Component padre;
	private BigDecimal totalVenta;

	@Override
	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);
		padre = (Component) Executions.getCurrent().getArg().get("padre");
		totalVenta = (BigDecimal) Executions.getCurrent().getArg().get("total_venta");
		this.debx_efectivo.setValue(new BigDecimal(0));
		debx_efectivo.setConstraint("no empty");
		debx_efectivo.setFormat("#,##0.00");

		System.out.println("TOTAL VENTA: " + totalVenta);
	}

	public void onClick$btnAceptar() {
		System.out.println("onClick$guardarWinClienteForm");
		debx_efectivo.setConstraint("no empty");
		BigDecimal efectivo = this.debx_efectivo.getValue();
		BigDecimal valorDevolver = efectivo.subtract(totalVenta);
		if (valorDevolver.compareTo(BigDecimal.ZERO) < 0) {
			DialogUtil.showInformation("El valor ingresado es inferior al de la venta.");
			return;
		}
		String mensaje = "El valor a devolver es: " + FormatoUtil.formatDecimal(valorDevolver);
		Events.sendEvent(new Event("onAceptar", padre, valorDevolver));
		DialogUtil.showInformation(mensaje);
		win_valida_efectivo.detach();
	}

	public void onClick$btnCancelar() {
		Events.sendEvent(new Event("onCancelar", padre, null));
		win_valida_efectivo.detach();
	}
}