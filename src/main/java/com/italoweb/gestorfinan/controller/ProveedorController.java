package com.italoweb.gestorfinan.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.event.SerializableEventListener;
import org.zkoss.zk.ui.ext.AfterCompose;
import org.zkoss.zul.Button;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Div;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listcell;
import org.zkoss.zul.Listhead;
import org.zkoss.zul.Listheader;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Window;

import com.italoweb.gestorfinan.components.Switch;
import com.italoweb.gestorfinan.model.Estado;
import com.italoweb.gestorfinan.model.Proveedor;
import com.italoweb.gestorfinan.model.TipoProveedor;
import com.italoweb.gestorfinan.model.poblacion.Ciudad;
import com.italoweb.gestorfinan.model.poblacion.Departamento;
import com.italoweb.gestorfinan.model.poblacion.Pais;
import com.italoweb.gestorfinan.repository.PoblacionDAO;
import com.italoweb.gestorfinan.repository.ProveedorDAO;
import com.italoweb.gestorfinan.util.ComponentsUtil;
import com.italoweb.gestorfinan.util.DialogUtil;

public class ProveedorController extends Window implements AfterCompose {
	private static final long serialVersionUID = 7513499911306546485L;
	private Textbox text_filtro_proveedor;
	private Listbox listbox_proveedor;
	private Map<String, Listitem> listitems = new HashMap<String, Listitem>();

	private Window win_proveedor_form;
	private Textbox text_nit;
	private Textbox text_nombre;
	private Textbox text_email;
	private Textbox text_telefono;
	private Combobox comb_ciudad;
	private Combobox comb_tipo_proveedor;
	private Textbox text_direccion;
	private Textbox text_persContacto;
	private Combobox comb_departamento;
	private Combobox comb_pais;

	private ProveedorDAO proveedorDAO = new ProveedorDAO();
	private PoblacionDAO poblacionDAO = new PoblacionDAO();

	@Override
	public void afterCompose() {
		ComponentsUtil.connectVariablesController(this);
		this.cargarComponentes();
		this.cargarListaProveedor();
		this.isMobile();
	}

	public void cargarComponentes() {
		this.text_telefono.setMaxlength(14);
		this.text_nit.setMaxlength(12);
		this.text_nombre.setMaxlength(20);
		this.text_direccion.setMaxlength(25);
		this.text_telefono.setZclass("none");
		this.text_telefono.setWidgetListener("onBind", "jq(this.getInputNode()).mask('(999) 999-9999');");
		cargarComboboxPais();
		inicializarComboboxTipoProveedor();
	}

	public void cargarListaProveedor() {
		this.text_filtro_proveedor.setValue("");
		this.listbox_proveedor.getItems().clear();
		proveedorDAO.findAll().forEach(this::createListitem);
	}

	private void inicializarComboboxTipoProveedor() {
		comb_tipo_proveedor.getItems().clear();
		comb_tipo_proveedor.setReadonly(true);
		for (TipoProveedor tp : TipoProveedor.values()) {
			Comboitem item = new Comboitem(tp.getLabel());
			item.setValue(tp);
			comb_tipo_proveedor.appendChild(item);
		}
	}

	private void cargarComboboxPais() {
		System.out.println("método cargarComboboxPais");
		comb_pais.getItems().clear();
		comb_departamento.getItems().clear();
		comb_ciudad.getItems().clear();

		List<Pais> paises = poblacionDAO.listarPaises();
		for (Pais pais : paises) {
			Comboitem item = new Comboitem(pais.getNombre());
			item.setValue(pais);
			comb_pais.appendChild(item);
		}

		comb_pais.setSelectedItem(null);
		deshabilitaSelPais();
	}

	public void deshabilitaSelPais() {
		comb_departamento.getItems().clear();
		comb_departamento.setSelectedItem(null);
		comb_departamento.setDisabled(true);
		comb_departamento.setValue(null);
		comb_ciudad.getItems().clear();
		comb_ciudad.setValue(null);
		comb_ciudad.setSelectedItem(null);
		comb_ciudad.setDisabled(true);
	}

	public void onSelectPais() {
		System.out.println("método onSelectPais");
		deshabilitaSelPais();

		Pais paisSeleccionado = comb_pais.getSelectedItem().getValue();
		List<Departamento> departamentos = poblacionDAO.listarDepartamentosPorPais(paisSeleccionado.getId());
		cargarComboboxDepartamento(departamentos);
		comb_departamento.setDisabled(false);
	}

	private void cargarComboboxDepartamento(List<Departamento> departamentos) {
		System.out.println("método cargarComboboxDepartamento");
		deshabilitaSelPais();

		for (Departamento d : departamentos) {
			Comboitem item = new Comboitem(d.getNombre());
			item.setValue(d);
			comb_departamento.appendChild(item);
		}

	}

	public void deshabilitaSelDep() {
		comb_ciudad.getItems().clear();
		comb_ciudad.setSelectedItem(null);
		comb_ciudad.setDisabled(true);
		comb_ciudad.setValue(null);
	}

	public void onSelectDepartamento() {
		deshabilitaSelDep();
		Departamento depto = comb_departamento.getSelectedItem().getValue();
		List<Ciudad> ciudades = poblacionDAO.listarCiudadesPorDepartamento(depto.getId());
		cargarComboboxCiudad(ciudades);
		comb_ciudad.setDisabled(false);
	}

	private void cargarComboboxCiudad(List<Ciudad> ciudades) {
		deshabilitaSelDep();

		for (Ciudad c : ciudades) {
			Comboitem item = new Comboitem(c.getNombre());
			item.setValue(c);
			comb_ciudad.appendChild(item);
		}
	}

	public void isMobile() {
		boolean esMovil = Executions.getCurrent().getBrowser("mobile") != null;
		Listhead listhead = listbox_proveedor.getListhead();
		if (listhead != null) {
			List<Listheader> headers = listhead.getChildren();
			for (Listheader header : headers) {
				if (esMovil) {
					header.setHflex("min");
				} else {
					header.setHflex("true");
				}
			}
		}
	}

	private void createListitem(Proveedor proveedor) {
		Listitem listitem = new Listitem();
		this.updateListitem(proveedor, listitem);
		this.listitems.put(proveedor.getId().toString(), listitem);
		this.listbox_proveedor.appendChild(listitem);
	}

	private void updateListitem(Proveedor proveedor, Listitem listitem) {
		listitem.getChildren().clear();
		listitem.appendChild(new Listcell(proveedor.getNit().toString()));
		listitem.appendChild(new Listcell(proveedor.getNombre()));
		listitem.appendChild(new Listcell(proveedor.getEmail()));
		listitem.appendChild(new Listcell(proveedor.getTelefono()));
		listitem.appendChild(new Listcell(proveedor.getCiudad().getNombre()));
		listitem.appendChild(new Listcell(proveedor.getPersonaContacto()));
		listitem.appendChild(new Listcell(proveedor.getDireccion()));
		listitem.appendChild(new Listcell(
				proveedor.getTipo() != null && proveedor.getTipo().equals(TipoProveedor.NATURAL) ? "Natural"
						: "Juridico"));

		/* Estado */
		Switch checkActive = ComponentsUtil.getSwitch(proveedor.getEstado().equals(Estado.ACTIVO));
		checkActive.addEventListener(Switch.ON_TOGGLE, new SerializableEventListener<Event>() {
			private static final long serialVersionUID = 1L;

			public void onEvent(Event event) throws Exception {
				boolean check = (Boolean) event.getData();
				Estado estado = check ? Estado.ACTIVO : Estado.INACTIVO;
				proveedorDAO.actualizarEstado(estado, proveedor.getId());
			}
		});
		listitem.appendChild(ComponentsUtil.getListcell(null, checkActive));

		/* Acciones */
		Button btnEdit = ComponentsUtil.getButton("", "btn btn-primary", "z-icon-pencil");
		btnEdit.addEventListener(Events.ON_CLICK, (e) -> {
			this.cargarWinProveedorForm(proveedor);
		});
		Button btnDelete = ComponentsUtil.getButton("", "btn btn-danger", "z-icon-trash-o");
		btnDelete.addEventListener(Events.ON_CLICK, (e) -> {
			this.eliminarProveedor(proveedor);
		});

		Div divBtnGroup = ComponentsUtil.getDiv("btn-group btn-group-sm", btnEdit, btnDelete);
		listitem.appendChild(ComponentsUtil.getListcell("", divBtnGroup));

		listitem.setValue(proveedor);
	}

	public void filtrarListProveedor(String filter) {
		filter = filter.trim().toUpperCase();
		for (Listitem listitem : this.listbox_proveedor.getItems()) {
			Proveedor attribute = listitem.getValue();
			boolean matchNit = attribute.getNit().toUpperCase().contains(filter);
			boolean matchNombre = attribute.getNombre().toUpperCase().contains(filter);
			boolean matchEmail = attribute.getEmail().toUpperCase().contains(filter);
			boolean matchTelefono = attribute.getTelefono().toUpperCase().contains(filter);
			boolean matchCiudad = attribute.getCiudad().getNombre().toUpperCase().contains(filter);
			boolean matchDireccion = attribute.getDireccion().toUpperCase().contains(filter);
			listitem.setVisible(filter.isEmpty() || matchNit || matchNombre || matchEmail || matchTelefono
					|| matchCiudad || matchDireccion);
		}
	}

	private void eliminarProveedor(Proveedor proveedor) {
		DialogUtil.showConfirmDialog("¿Está seguro de que desea eliminar este registro?", "Confirmación")
				.thenAccept(confirmed -> {
					if (confirmed) {
						this.proveedorDAO.delete(proveedor);
						this.cargarListaProveedor();
						DialogUtil.showShortMessage("success", "Proveedor Eliminado Exitosamente");
					}
				});
	}

	public void filtrarComboCiudad(String filter) {
		filtrarCombo(this.comb_ciudad, filter);
	}

	public void filtrarComboPais(String filter) {
		filtrarCombo(this.comb_pais, filter);
	}

	public void filtrarComboDepartamento(String filter) {
		filtrarCombo(this.comb_departamento, filter);
	}

	public void filtrarComboTipo(String filter) {
		filtrarCombo(this.comb_tipo_proveedor, filter);
	}

	public void filtrarCombo(Combobox combo, String filter) {
		filter = filter.trim().toUpperCase();
		for (Comboitem comboItem : combo.getItems()) {
			String label = comboItem.getLabel();
			comboItem.setVisible(filter.isEmpty() || label.toUpperCase().contains(filter));
		}
	}

	public void cargarWinProveedorForm(Proveedor proveedor) {
		text_nit.setValue("");
		text_nombre.setValue("");
		text_persContacto.setValue("");
		text_email.setValue("");
		text_telefono.setValue("");
		text_direccion.setValue("");
		comb_pais.setSelectedItem(null);
		comb_departamento.getItems().clear();
		comb_ciudad.getItems().clear();
		comb_departamento.setDisabled(true);
		comb_ciudad.setDisabled(true);
		this.comb_tipo_proveedor.setSelectedItem(null);
		if (proveedor != null) {
			text_nit.setValue(proveedor.getNit());
			text_nombre.setValue(proveedor.getNombre());
			text_persContacto.setValue(proveedor.getPersonaContacto());
			text_email.setValue(proveedor.getEmail());
			text_telefono.setValue(proveedor.getTelefono());
			text_direccion.setValue(proveedor.getDireccion());
			for (Comboitem item : comb_tipo_proveedor.getItems()) {
				if (item.getValue() == proveedor.getTipo()) {
					comb_tipo_proveedor.setSelectedItem(item);
					break;
				}
			}
			Ciudad ciudad = proveedor.getCiudad();
			Departamento departamento = ciudad.getDepartamento();
			Pais pais = departamento.getPais();
			for (Comboitem item : comb_pais.getItems()) {
				if (((Pais) item.getValue()).getId().equals(pais.getId())) {
					comb_pais.setSelectedItem(item);
					break;
				}
			}

			List<Departamento> departamentos = poblacionDAO.listarDepartamentosPorPais(pais.getId());
			cargarComboboxDepartamento(departamentos);
			comb_departamento.setDisabled(false);
			for (Comboitem item : comb_departamento.getItems()) {
				if (((Departamento) item.getValue()).getId().equals(departamento.getId())) {
					comb_departamento.setSelectedItem(item);
					break;
				}
			}

			List<Ciudad> ciudades = poblacionDAO.listarCiudadesPorDepartamento(departamento.getId());
			cargarComboboxCiudad(ciudades);
			comb_ciudad.setDisabled(false);
			for (Comboitem item : comb_ciudad.getItems()) {
				if (((Ciudad) item.getValue()).getId().equals(ciudad.getId())) {
					comb_ciudad.setSelectedItem(item);
					break;
				}
			}
		} else {
			deshabilitaSelPais();
		}
		this.win_proveedor_form.setAttribute("CLIENTE", proveedor);
		this.win_proveedor_form.doModal();
	}

	@SuppressWarnings("unused")
	public void guardarWinProveedorForm() {

		String nit = this.text_nit.getValue().trim();
		String nombre = this.text_nombre.getValue().trim();
		String personaContacto = this.text_persContacto.getValue().trim();
		String email = this.text_email.getValue().trim();
		String telefono = this.text_telefono.getValue();
		Ciudad ciudad = comb_ciudad.getSelectedItem() != null ? (Ciudad) comb_ciudad.getSelectedItem().getValue()
				: null;
		String direccion = this.text_direccion.getValue();
		Comboitem sel = comb_tipo_proveedor.getSelectedItem();
		TipoProveedor tipo = (TipoProveedor) sel.getValue();
		String mensaje = "Proveedor Guardado Exitosamente";
		// Validaciones
		if (sel == null) {
			DialogUtil.showError("Debe ingresar el tipo.");
			return;
		}
		if (StringUtils.isBlank(nit)) {
			DialogUtil.showError("El NIT es Obligatorio.");
			return;
		}
		if (StringUtils.isBlank(nombre)) {
			DialogUtil.showError("El nombre es Obligatorio.");
			return;
		}
		if (StringUtils.isBlank(personaContacto)) {
			DialogUtil.showError("El contacto es Obligatorio.");
			return;
		}
		if (StringUtils.isBlank(email)) {
			DialogUtil.showError("El E-mail es Obligatorio.");
			return;
		}
		if (telefono.length() != 14) {
			DialogUtil.showError("El telefono no cumple la longitud.");
			return;
		}
		if (ciudad == null) {
			DialogUtil.showError("La ciudad es obligatoria.");
			return;
		}
		if (StringUtils.isBlank(direccion)) {
			DialogUtil.showError("La dirección es Obligatoria.");
			return;
		}

		Proveedor proveedor = (Proveedor) this.win_proveedor_form.getAttribute("CLIENTE");

		if (proveedor == null) {
			proveedor = new Proveedor();
			proveedor.setNit(nit);
			proveedor.setNombre(nombre);
			proveedor.setEmail(email);
			proveedor.setTelefono(telefono);
			proveedor.setCiudad(ciudad);
			proveedor.setTipo(tipo);
			proveedor.setDireccion(direccion);
			proveedor.setPersonaContacto(personaContacto);
			proveedor.setEstado(Estado.ACTIVO);
			this.proveedorDAO.save(proveedor);
		} else {
			proveedor.setNit(nit);
			proveedor.setNombre(nombre);
			proveedor.setEmail(email);
			proveedor.setTelefono(telefono);
			proveedor.setCiudad(ciudad);
			proveedor.setTipo(tipo);
			proveedor.setDireccion(direccion);
			proveedor.setPersonaContacto(personaContacto);
			this.proveedorDAO.update(proveedor);
			mensaje = "Proveedor Editado Exitosamente";
		}
		this.cargarListaProveedor();
		this.win_proveedor_form.setVisible(false);
		DialogUtil.showShortMessage("success", mensaje);
	}
}