package com.italoweb.gestorfinan.controller;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.event.SerializableEventListener;
import org.zkoss.zk.ui.ext.AfterCompose;
import org.zkoss.zul.A;
import org.zkoss.zul.Button;
import org.zkoss.zul.Div;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Menuitem;
import org.zkoss.zul.Menupopup;
import org.zkoss.zul.Span;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Window;

import com.italoweb.gestorfinan.components.Chosenbox;
import com.italoweb.gestorfinan.components.Switch;
import com.italoweb.gestorfinan.config.MenuManager;
import com.italoweb.gestorfinan.model.Roles;
import com.italoweb.gestorfinan.navigation.EstadoMenu;
import com.italoweb.gestorfinan.navigation.MenuItem;
import com.italoweb.gestorfinan.repository.RolesDAO;
import com.italoweb.gestorfinan.util.ComponentsUtil;
import com.italoweb.gestorfinan.util.DialogUtil;
import com.italoweb.gestorfinan.util.HEXUtil;

public class MenuController extends Window implements AfterCompose {

    private static final long serialVersionUID = 5639370625853679716L;
	private Div sidebarNav;
    private List<MenuItem> menuItems;
    private Textbox text_id, text_nombre, text_url, text_icono;
    private Intbox text_orden;
    private Button btn_guardar;

    private Window win_menu_form;
    private Label label_padre;
    private Textbox text_nombre_win, text_url_win, text_icono_win;
    private Div div_estado;
    private Switch switch_estado;
    private Div div_roles;
    private Chosenbox<Roles> chosen_roles;

    private MenuManager manager;
    private RolesDAO rolesDAO;

    @Override
    public void afterCompose() {
        ComponentsUtil.connectVariablesController(this);
        this.manager = new MenuManager();
        this.cargarComponentes();
    }

    public void cargarComponentes(){
        this.initMenu();
        this.text_orden.setReadonly(true);
        this.btn_guardar.setDisabled(true);
        this.btn_guardar.addEventListener(Events.ON_CLICK, e -> {
            MenuItem item = (MenuItem) btn_guardar.getAttribute("MENU_ITEM");
            String id = this.text_id.getValue().trim();
            String nombre = this.text_nombre.getValue().trim();
            if (!nombre.isEmpty()){
                if (item != null){
                    item.setTitle(nombre);
                    item.setHref(bookmarkUrl(text_url.getValue()));
                    item.setIconClass(text_icono.getValue());
                    item.setOrder(text_orden.getValue());
                    item.setRoles(chosen_roles.getSelectedItems());
                    MenuManager menu = new MenuManager();
                    menu.updateById(id, item);
                    Executions.sendRedirect(null);
                }
            }else{
                DialogUtil.showError("El Nombre del Menú es un campo obligatorio.");
            }
        });
        
        /*Estado*/
        this.switch_estado = ComponentsUtil.getSwitch(false);
        this.switch_estado.addEventListener(Switch.ON_TOGGLE, new SerializableEventListener<Event>() {
            private static final long serialVersionUID = 1711205549535046996L;
            public void onEvent(Event event) throws Exception {
                boolean check = (Boolean) event.getData();
                EstadoMenu estado = check ? EstadoMenu.ACTIVO : EstadoMenu.INACTIVO;
                String id = text_id.getValue().trim();
                if (StringUtils.isNotBlank(id)) {
                    MenuManager menu = new MenuManager();
                    MenuItem item = menu.getById(id);
                    item.setStatus(estado);
                    menu.updateById(id, item);
                    Executions.sendRedirect(null);
				}
            }
        });
        this.div_estado.appendChild(this.switch_estado);
        this.rolesDAO = new RolesDAO();
        
        List<Roles> roles = this.rolesDAO.findAll();
        this.chosen_roles = new Chosenbox<Roles>();
        this.chosen_roles.setModel(roles, Roles::getNombre);
        this.chosen_roles.setMultiple(true);
        this.chosen_roles.setWrapMode("scroll");
        this.div_roles.appendChild(this.chosen_roles);
    }

    // Inicializa los elementos del menú
    private void initMenu() {
        //MenuModel menuModel = new MenuModel();
        //manager.saveMenu(menuModel.cargarMenu());
        this.menuItems = new ArrayList<>();
        this.menuItems = manager.getMenu();
        this.menuItems.sort(Comparator.comparingInt(MenuItem::getOrder));
        this.buildSidebar();
    }

    // Construye el menú dinámicamente en la vista
    private void buildSidebar() {
        sidebarNav.appendChild(createSidebarHeader("Menú"));
        for (MenuItem item : menuItems) {
            if (item.getSubMenu().isEmpty()) {
                sidebarNav.appendChild(createSidebarItem(item));
            } else {
                sidebarNav.appendChild(createSidebarWithDropdown(item));
            }
        }
    }

    private Div createSidebarHeader(String title) {
        Div header = new Div();
        header.setSclass("sidebar-header");
        header.appendChild(new Label(title));
        return header;
    }

    private Div createSidebarItem(MenuItem item) {
        Div div = new Div();
        div.setSclass("sidebar-item");

        A link = new A();
        link.setSclass("sidebar-link-config sidebar-link");
        link.setStyle("padding: 10px;");
        if (item.getIconClass() != null) {
            link.setIconSclass(item.getIconClass());
        }
        link.setLabel(item.getTitle());

        link.addEventListener(Events.ON_CLICK, event -> {
            text_id.setValue(item.getId());
            text_nombre.setValue(item.getTitle());
            text_url.setValue(item.getHref());
            text_url.setValue(desifrarBookmarkUrl(item.getHref()));
            text_icono.setValue(item.getIconClass());
            text_url.setReadonly(false);
            text_orden.setValue(item.getOrder());
            btn_guardar.setAttribute("MENU_ITEM", item);
            btn_guardar.setDisabled(false);
            switch_estado.setChecked(item.getStatus() == EstadoMenu.ACTIVO ? true : false);
            chosen_roles.clearSelection();
            if (item.getRoles() != null) {
                List<Roles> getRoles = new ArrayList<>(item.getRoles());
                chosen_roles.setSelectedItems(getRoles);
			}
        });

        // Crear menú contextual
        Menupopup popup = new Menupopup();

        Menuitem crear = new Menuitem("Crear");
        crear.setIconSclass("z-icon-plus-circle");
        crear.addEventListener(Events.ON_CLICK, e -> {
            this.win_menu_form.setAttribute("ITEM", item);
            String titulo = item != null ? String.format("Item Padre: %s" , item.getTitle()) : "";
            this.label_padre.setValue(titulo);
            this.win_menu_form.doModal();
        });

        Menuitem eliminar = new Menuitem("Eliminar");
        eliminar.setIconSclass("z-icon-trash-o");
        eliminar.addEventListener(Events.ON_CLICK, e -> {
            String msjConfirm = String.format("¿Está seguro de que desea eliminar el item: %s y sus hijos", item.getTitle());
            DialogUtil.showConfirmDialog(msjConfirm, "Confirmación")
                    .thenAccept(confirmed -> {
                        if (confirmed) {
                            boolean res = manager.deleteById(item.getId());
                            if (res)
                                DialogUtil.showInformation("Se ha eliminado el item del menú corectamente");
                                Executions.sendRedirect(null);
                        }
                    });
        });
        Menuitem subir = new Menuitem("Subir");
        subir.setIconSclass("fa fa-arrow-up");
        subir.addEventListener(Events.ON_CLICK, e -> {
            manager.moverArriba(item.getId());
            Executions.sendRedirect(null);
        });

        Menuitem bajar = new Menuitem("Bajar");
        bajar.setIconSclass("fa fa-arrow-down");
        bajar.addEventListener(Events.ON_CLICK, e -> {
            manager.moverAbajo(item.getId());
            Executions.sendRedirect(null);
        });

        popup.appendChild(crear);
        popup.appendChild(eliminar);
        popup.appendChild(subir);
        popup.appendChild(bajar);
        link.setContext(popup);
        div.appendChild(link);
        div.appendChild(popup);
        return div;
    }

    private Div createSidebarWithDropdown(MenuItem item) {
        // Contenedor principal del menú desplegable
        Div div = new Div();
        div.setSclass("sidebar-item sidebar-parent");

        // Enlace principal (padre del dropdown)
        A link = new A();
        link.setSclass("sidebar-link-config sidebar-link sidebar-toggle");
        link.setStyle("padding: 10px;");
        if (item.getIconClass() != null) {
            link.setIconSclass(item.getIconClass());
        }
        link.setLabel(item.getTitle());

        Span caret = new Span();
        caret.setSclass("submenu-caret fa fa-chevron-down");
        caret.setId("caret-" + item.getId()); // Asigna un ID único si lo necesitas
        link.appendChild(caret);

        link.addEventListener(Events.ON_CLICK, event -> {
            // Encuentra el ícono dentro del link
            Span caretIcon = (Span) link.getFellowIfAny("caret-" + item.getId());

            if (caretIcon != null) {
                String currentClass = caretIcon.getSclass();
                if (currentClass.contains("chevron-down")) {
                    caretIcon.setSclass("submenu-caret fa fa-chevron-up");
                } else {
                    caretIcon.setSclass("submenu-caret fa fa-chevron-down");
                }
            }

            //setiar campos
            text_id.setValue(item.getId());
            text_nombre.setValue(item.getTitle());
            text_url.setValue(item.getHref());
            text_url.setReadonly(true);
            text_icono.setValue(item.getIconClass());
            text_orden.setValue(item.getOrder());
            btn_guardar.setAttribute("MENU_ITEM", item);
            btn_guardar.setDisabled(false);
            switch_estado.setChecked(item.getStatus() == EstadoMenu.ACTIVO ? true : false);
            chosen_roles.clearSelection();
            if (item.getRoles() != null) {
                List<Roles> getRoles = new ArrayList<>(item.getRoles());
                chosen_roles.setSelectedItems(getRoles);
			}
        });

        // Crear menú contextual
        Menupopup popup = new Menupopup();

        Menuitem crear = new Menuitem("Crear");
        crear.setIconSclass("z-icon-plus-circle");
        crear.addEventListener(Events.ON_CLICK, e -> {
            this.win_menu_form.setAttribute("ITEM", item);
            String titulo = item != null ? String.format("Item Padre: %s" , item.getTitle()) : "";
            this.label_padre.setValue(titulo);
            this.win_menu_form.doModal();
        });

        Menuitem eliminar = new Menuitem("Eliminar");
        eliminar.setIconSclass("z-icon-trash-o");
        eliminar.addEventListener(Events.ON_CLICK, e -> {
            String msjConfirm = String.format("¿Está seguro de que desea eliminar el item: %s y sus hijos", item.getTitle());
            DialogUtil.showConfirmDialog(msjConfirm, "Confirmación")
                    .thenAccept(confirmed -> {
                        if (confirmed) {
                            boolean res = manager.deleteById(item.getId());
                            if (res)
                                DialogUtil.showInformation("Se ha eliminado el item del menú corectamente.");
                                Executions.sendRedirect(null);
                        }
                    });
        });

        Menuitem subir = new Menuitem("Subir");
        subir.setIconSclass("fa fa-arrow-up");
        subir.addEventListener(Events.ON_CLICK, e -> {
            manager.moverArriba(item.getId());
            Executions.sendRedirect(null);
        });

        Menuitem bajar = new Menuitem("Bajar");
        bajar.setIconSclass("fa fa-arrow-down");
        bajar.addEventListener(Events.ON_CLICK, e -> {
            manager.moverAbajo(item.getId());
            Executions.sendRedirect(null);
        });

        popup.appendChild(crear);
        popup.appendChild(eliminar);
        popup.appendChild(subir);
        popup.appendChild(bajar);
        link.setContext(popup);

        div.appendChild(link);
        div.appendChild(popup);

        // Contenedor del dropdown
        Div dropdown = new Div();
        dropdown.setSclass("sidebar-dropdown-config list-unstyled collapse sidebar-pages");

        for (MenuItem subItem : item.getSubMenu()) {
            if (subItem.getSubMenu().isEmpty()) {
                dropdown.appendChild(createSidebarItem(subItem));
            } else {
                dropdown.appendChild(createSidebarWithDropdown(subItem));
            }
        }

        div.appendChild(dropdown);
        return div;
    }

    public String desifrarBookmarkUrl(String src) {
        if (src != null && src.startsWith("#")) {
            src = src.substring(1); // Elimina el primer carácter
        }
        System.out.println(src);
        return HEXUtil.convertirAOriginalDesdeHexLargo(src);
    }

    public String bookmarkUrl(String src){
        String bookmark = "#";
        return bookmark+HEXUtil.convertirATextoHexLargo(src);
    }

    public void guardarWinMenu(){
        MenuItem menuItem = (MenuItem) win_menu_form.getAttribute("ITEM");
        String nombre = text_nombre_win.getValue();
        String url = text_url_win.getValue();
        String icono = text_icono_win.getValue();
        if (!nombre.isEmpty()) {
            MenuItem menuItem1 = new MenuItem(nombre, bookmarkUrl(url), icono, manager.getNextOrdenRaiz());
            menuItem1.setStatus(EstadoMenu.ACTIVO);
            if (menuItem != null) {
                menuItem1 = new MenuItem(nombre, bookmarkUrl(url), icono, manager.getNextOrdenHijo(menuItem.getId()));
                menuItem1.setStatus(EstadoMenu.ACTIVO);
                manager.addSubMenu(menuItem.getId(), menuItem1);
            } else {
                manager.create(menuItem1);
            }
            DialogUtil.showInformation("Item agregado Correctamente.");
        }else {
            DialogUtil.showError("El campo nombre es obligatorio.");
        }
        this.win_menu_form.setVisible(false);
        Executions.sendRedirect(null);
    }

    public void crearMenuPrincipal(){
        this.win_menu_form.setAttribute("ITEM", null);
        this.label_padre.setValue("");
        this.win_menu_form.doModal();
    }
}