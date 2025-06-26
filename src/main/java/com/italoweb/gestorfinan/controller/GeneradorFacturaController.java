package com.italoweb.gestorfinan.controller;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.*;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import com.italoweb.gestorfinan.model.Cliente;
import com.italoweb.gestorfinan.model.factura.FacturaImpuesto;
import com.italoweb.gestorfinan.util.FacturaHelper;
import com.italoweb.gestorfinan.util.FormatoUtil;

public class GeneradorFacturaController {

	public static class ItemFactura {
		public String codigo;
		public String nombre;
		public String descripcion;
		public int cantidad;
		public double descuento;
		public double precioUnitario;

		public ItemFactura(String codigo, String nombre, String descripcion, int cantidad, double descuento,
				double precioUnitario) {
			this.codigo = codigo;
			this.nombre = nombre;
			this.descripcion = descripcion;
			this.cantidad = cantidad;
			this.descuento = descuento;
			this.precioUnitario = precioUnitario;
		}

		public double getTotal() {
			return cantidad * precioUnitario;
		}
	}

	@SuppressWarnings("deprecation")
	public File generarFactura(String nombreArchivo, String nombreEmpresa, String nitEmpresa, String direccionEmpresa,
			String telefonoEmpresa, String emailEmpresa, Cliente cliente, String fecha, String numeroFactura,
			List<ItemFactura> items, BigDecimal subTotal, BigDecimal totalIVA, BigDecimal totalDcto,
			BigDecimal totalVenta, String vendedor, List<FacturaImpuesto> impuestos) throws IOException {

		final float PAGE_WIDTH = PDRectangle.LETTER.getWidth();
		final float PAGE_HEIGHT = PDRectangle.LETTER.getHeight() / 2;
		final float LOGO_WIDTH = 180;
		final float LOGO_HEIGHT = 50;

		File archivo = new File(System.getProperty("java.io.tmpdir"), nombreArchivo);

		try (PDDocument documento = new PDDocument()) {
			float marginLeft = 30;
			float marginRight = 30;
			float marginTop = 20;
			float marginBottom = 30;
			float leading = 14;
			PDType1Font fuente = PDType1Font.HELVETICA;
			int maxItemsPerPage = 6;

			int totalPages = (int) Math.ceil((double) items.size() / maxItemsPerPage);

			for (int pageIndex = 0; pageIndex < totalPages; pageIndex++) {
				PDPage pagina = new PDPage(new PDRectangle(PAGE_WIDTH, PAGE_HEIGHT));
				documento.addPage(pagina);

				try (PDPageContentStream contenido = new PDPageContentStream(documento, pagina)) {
					float y = PAGE_HEIGHT - marginTop;

					if (pageIndex == 0) {
						String logoPath = FacturaHelper.obtenerRutaLogo();
						File logoFile = new File(logoPath);
						if (logoFile.exists()) {
							PDImageXObject logoImage = PDImageXObject.createFromFile(logoFile.getAbsolutePath(),
									documento);
							float xLogo = (PAGE_WIDTH - LOGO_WIDTH) / 2;
							contenido.drawImage(logoImage, xLogo, y - LOGO_HEIGHT, LOGO_WIDTH, LOGO_HEIGHT);
							y -= LOGO_HEIGHT + 5;
						}

						float xEmpresa = marginLeft;
						contenido.beginText();
						contenido.setFont(PDType1Font.HELVETICA_BOLD, 14);
						contenido.newLineAtOffset(xEmpresa, y);
						contenido.showText(nombreEmpresa);
						contenido.endText();

						y -= 14;
						contenido.setFont(fuente, 10);
						String[] datosEmpresa = { "NIT: " + nitEmpresa, "Dirección: " + direccionEmpresa,
								"Teléfono: " + telefonoEmpresa, "E-mail: " + emailEmpresa, "N°: " + numeroFactura,
								"Fecha: " + fecha };

						for (String linea : datosEmpresa) {
							contenido.beginText();
							contenido.newLineAtOffset(xEmpresa, y);
							contenido.showText(linea);
							contenido.endText();
							y -= leading;
						}

						// FACTURADA A
						contenido.setNonStrokingColor(0, 112, 192);
						contenido.addRect(marginLeft, y - 1, 250, 15);
						contenido.fill();

						contenido.setNonStrokingColor(255, 255, 255);
						contenido.beginText();
						contenido.setFont(PDType1Font.HELVETICA_BOLD, 10);
						contenido.newLineAtOffset(marginLeft + 5, y + 2);
						contenido.showText("FACTURADA A:");
						contenido.endText();

						y -= 18;
						contenido.setNonStrokingColor(0, 0, 0);
						contenido.beginText();
						contenido.setFont(fuente, 10);
						contenido.newLineAtOffset(marginLeft + 5, y);
						contenido.showText("Nombre: " + (cliente.getNombre() != null ? cliente.getNombre() : "N/A"));
						contenido.newLineAtOffset(0, -leading);
						contenido.showText("NIT: " + (cliente.getNit() != null ? cliente.getNit() : "N/A"));
						contenido.newLineAtOffset(0, -leading);
						contenido.showText("Correo: " + (cliente.getEmail() != null ? cliente.getEmail() : "N/A"));
						contenido.newLineAtOffset(0, -leading);
						contenido.showText(
								"Teléfono: " + (cliente.getTelefono() != null ? cliente.getTelefono() : "N/A"));
						contenido.endText();

						y -= 45;
					} else {
						y -= 10;
					}

					float tableTopY = y;
					float rowHeight = 15;
					float tableWidth = PAGE_WIDTH - marginLeft - marginRight;
					float[] colWidths = { 50, 100, 150, 40, 70, 70, 60 };
					float[] colPositions = new float[colWidths.length + 1];
					colPositions[0] = marginLeft;
					for (int i = 0; i < colWidths.length; i++) {
						colPositions[i + 1] = colPositions[i] + colWidths[i];
					}

					contenido.setNonStrokingColor(0, 112, 192);
					contenido.addRect(marginLeft, y, tableWidth, rowHeight);
					contenido.fill();

					contenido.setNonStrokingColor(255, 255, 255);
					contenido.beginText();
					contenido.setFont(PDType1Font.HELVETICA_BOLD, 9);
					contenido.newLineAtOffset(marginLeft + 3, y + 4);
					contenido.showText("CÓDIGO");
					contenido.newLineAtOffset(colWidths[0], 0);
					contenido.showText("NOMBRE");
					contenido.newLineAtOffset(colWidths[1], 0);
					contenido.showText("DESCRIPCIÓN");
					contenido.newLineAtOffset(colWidths[2], 0);
					contenido.showText("CANT");
					contenido.newLineAtOffset(colWidths[3], 0);
					contenido.showText("PRECIO");
					contenido.newLineAtOffset(colWidths[4], 0);
					contenido.showText("DCTO");
					contenido.newLineAtOffset(colWidths[5], 0);
					contenido.showText("VALOR");
					contenido.endText();

					y -= rowHeight;

					int start = pageIndex * maxItemsPerPage;
					int end = Math.min(start + maxItemsPerPage, items.size());
					float currentY = y;

					for (int i = start; i < end; i++) {
						ItemFactura item = items.get(i);
						contenido.setNonStrokingColor(0, 0, 0);
						contenido.beginText();
						contenido.setFont(fuente, 8);
						contenido.newLineAtOffset(marginLeft + 3, currentY + 4);
						contenido.showText(item.codigo);
						contenido.newLineAtOffset(colWidths[0], 0);
						contenido.showText(truncar(item.nombre.toUpperCase(), 25));
						contenido.newLineAtOffset(colWidths[1], 0);
						contenido.showText(truncar(item.descripcion.toUpperCase(), 25));
						contenido.newLineAtOffset(colWidths[2], 0);
						contenido.showText(String.valueOf(item.cantidad));
						contenido.newLineAtOffset(colWidths[3], 0);
						contenido.showText(FormatoUtil.formatDecimal(BigDecimal.valueOf(item.precioUnitario)));
						contenido.newLineAtOffset(colWidths[4], 0);
						contenido.showText(FormatoUtil.formatDecimal(BigDecimal.valueOf(item.descuento)) + "%");
						// Total
						BigDecimal qty = BigDecimal.valueOf(item.cantidad);
						BigDecimal unitPrice = BigDecimal.valueOf(item.precioUnitario);
						BigDecimal discountPct = BigDecimal.valueOf(item.descuento).divide(BigDecimal.valueOf(100), 4,
								RoundingMode.HALF_UP);

						BigDecimal bruto = unitPrice.multiply(qty);
						BigDecimal total = bruto.multiply(BigDecimal.ONE.subtract(discountPct)).setScale(2,
								RoundingMode.HALF_UP);
						contenido.newLineAtOffset(colWidths[5], 0);
						contenido.showText(FormatoUtil.formatDecimal(total));
						contenido.endText();
						currentY -= rowHeight;
					}

					for (int i = 0; i <= (end - start) + 1; i++) {
						float lineY = tableTopY - i * rowHeight;
						contenido.moveTo(marginLeft, lineY);
						contenido.lineTo(marginLeft + tableWidth, lineY);
					}
					contenido.stroke();

					for (float x : colPositions) {
						contenido.moveTo(x, tableTopY);
						contenido.lineTo(x, currentY + rowHeight);
					}
					contenido.stroke();

					if (pageIndex == totalPages - 1) {
						float espacioTotales = (impuestos.size() + 4) * leading;
						float yStart = y - 10;
						if (yStart - espacioTotales < marginBottom) {
							PDPage paginaTotales = new PDPage(new PDRectangle(PAGE_WIDTH, PAGE_HEIGHT));
							documento.addPage(paginaTotales);
							try (PDPageContentStream contenidoTotales = new PDPageContentStream(documento,
									paginaTotales)) {
								dibujarTotalesConImpuestos(contenidoTotales, marginLeft, PAGE_HEIGHT - marginTop,
										subTotal, impuestos, totalDcto, totalVenta);
							}
						} else {
							dibujarTotalesConImpuestos(contenido, marginLeft, yStart, subTotal, impuestos, totalDcto,
									totalVenta);
						}
					}
					contenido.beginText();
					contenido.setFont(fuente, 9);
					String textoPagina = "Pág. " + (pageIndex + 1) + " de " + totalPages;
					float textoWidth = fuente.getStringWidth(textoPagina) / 1000 * 9;
					contenido.newLineAtOffset(PAGE_WIDTH - marginRight - textoWidth, marginBottom - 10);
					contenido.showText(textoPagina);
					contenido.endText();
				}
			}
			documento.save(archivo);
		}
		return archivo;
	}

	private String truncar(String texto, int maxLength) {
		if (texto == null)
			return "";
		return texto.length() > maxLength ? texto.substring(0, maxLength) : texto;
	}

	public File generarFacturaTicket80mm(String nombreArchivo, String nombreEmpresa, String nitEmpresa,
			String direccionEmpresa, String telefonoEmpresa, String emailEmpresa, Cliente cliente, String fecha,
			String numeroFactura, List<ItemFactura> items, BigDecimal subTotal, BigDecimal totalIVA,
			BigDecimal totalDcto, BigDecimal totalVenta, String vendedor, List<FacturaImpuesto> impuestos)
			throws IOException {
		// Fuentes y tamaños
		final float FONT_SIZE_TITULO = 8f;
		final float FONT_SIZE_CABECERA = 9f;
		PDType1Font FONT_REGULAR = PDType1Font.HELVETICA;
		PDType1Font FONT_BOLD = PDType1Font.HELVETICA_BOLD;

		// Alto dinámico
		int lineas = 40 + items.size() * 2; // más espacio para posibles wraps
		float alto = Math.max(400, FONT_SIZE_CABECERA * lineas);
		PDRectangle ticketSize = new PDRectangle(226.77f, alto);

		File archivo = new File(System.getProperty("java.io.tmpdir"), nombreArchivo);
		try (PDDocument documento = new PDDocument()) {
			PDPage pagina = new PDPage(ticketSize);
			documento.addPage(pagina);
			try (PDPageContentStream contenido = new PDPageContentStream(documento, pagina)) {
				float margin = 5f;
				float width = ticketSize.getWidth() - 2 * margin;
				float y = ticketSize.getHeight() - margin - FONT_SIZE_CABECERA;

				// ===== Nombre Empresa ===== (centrado)
				contenido.beginText();
				contenido.setFont(FONT_BOLD, FONT_SIZE_CABECERA);
				float titleWidth = FONT_BOLD.getStringWidth(nombreEmpresa) / 1000 * FONT_SIZE_CABECERA;
				float titleX = (ticketSize.getWidth() - titleWidth) / 2;
				contenido.newLineAtOffset(titleX, y);
				contenido.showText(nombreEmpresa);
				contenido.endText();
				y -= (FONT_SIZE_CABECERA + 4);

				// ===== Cabecera Empresa =====
				contenido.beginText();
				contenido.setFont(FONT_BOLD, FONT_SIZE_TITULO);
				contenido.newLineAtOffset(margin, y);
				contenido.showText("NIT:");
				contenido.setFont(FONT_REGULAR, FONT_SIZE_TITULO);
				contenido.showText(" " + nitEmpresa);
				contenido.endText();
				y -= (FONT_SIZE_TITULO + 2);

				contenido.beginText();
				contenido.setFont(FONT_BOLD, FONT_SIZE_TITULO);
				contenido.newLineAtOffset(margin, y);
				contenido.showText("Tel:");
				contenido.setFont(FONT_REGULAR, FONT_SIZE_TITULO);
				contenido.showText(" " + telefonoEmpresa);
				contenido.endText();
				y -= (FONT_SIZE_TITULO + 2);

				// Dirección con wrap
				List<String> linesDir = wrapText(direccionEmpresa, FONT_REGULAR, FONT_SIZE_TITULO, width);
				for (String linea : linesDir) {
					contenido.beginText();
					contenido.setFont(FONT_REGULAR, FONT_SIZE_TITULO);
					contenido.newLineAtOffset(margin, y);
					contenido.showText(linea);
					contenido.endText();
					y -= (FONT_SIZE_TITULO + 2);
				}

				contenido.beginText();
				contenido.setFont(FONT_BOLD, FONT_SIZE_TITULO);
				contenido.newLineAtOffset(margin, y);
				contenido.showText("E-mail:");
				contenido.setFont(FONT_REGULAR, FONT_SIZE_TITULO);
				contenido.showText(" " + emailEmpresa);
				contenido.endText();
				y -= (FONT_SIZE_TITULO + 6);

				// ===== Info Factura / Vendedor =====
				contenido.beginText();
				contenido.setFont(FONT_BOLD, FONT_SIZE_TITULO);
				contenido.newLineAtOffset(margin, y);
				contenido.showText("Factura:");
				contenido.setFont(FONT_REGULAR, FONT_SIZE_TITULO);
				contenido.showText(" " + numeroFactura);
				contenido.endText();
				y -= (FONT_SIZE_TITULO + 2);

				contenido.beginText();
				contenido.setFont(FONT_BOLD, FONT_SIZE_TITULO);
				contenido.newLineAtOffset(margin, y);
				contenido.showText("Fecha:");
				contenido.setFont(FONT_REGULAR, FONT_SIZE_TITULO);
				contenido.showText(" " + fecha);
				contenido.endText();
				y -= (FONT_SIZE_TITULO + 2);

				contenido.beginText();
				contenido.setFont(FONT_BOLD, FONT_SIZE_TITULO);
				contenido.newLineAtOffset(margin, y);
				contenido.showText("Vendedor:");
				contenido.setFont(FONT_REGULAR, FONT_SIZE_TITULO);
				contenido.showText(" " + vendedor);
				contenido.endText();
				y -= (FONT_SIZE_TITULO + 6);

				// Línea separadora
				contenido.setLineWidth(0.3f);
				contenido.moveTo(margin, y);
				contenido.lineTo(ticketSize.getWidth() - margin, y);
				contenido.stroke();
				y -= (FONT_SIZE_TITULO + 4);

				// ===== Cliente =====
				contenido.beginText();
				contenido.setFont(FONT_BOLD, FONT_SIZE_TITULO);
				contenido.newLineAtOffset(margin, y);
				contenido.showText("Cliente:");
				contenido.setFont(FONT_REGULAR, FONT_SIZE_TITULO);
				contenido.showText(" " + cliente.getNombre());
				contenido.endText();
				y -= (FONT_SIZE_TITULO + 2);

				contenido.beginText();
				contenido.setFont(FONT_BOLD, FONT_SIZE_TITULO);
				contenido.newLineAtOffset(margin, y);
				contenido.showText("NIT:");
				contenido.setFont(FONT_REGULAR, FONT_SIZE_TITULO);
				contenido.showText(" " + cliente.getNit());
				contenido.endText();
				y -= (FONT_SIZE_TITULO + 2);

				contenido.beginText();
				contenido.setFont(FONT_BOLD, FONT_SIZE_TITULO);
				contenido.newLineAtOffset(margin, y);
				contenido.showText("Tel:");
				contenido.setFont(FONT_REGULAR, FONT_SIZE_TITULO);
				contenido.showText(" " + cliente.getTelefono());
				contenido.endText();
				y -= (FONT_SIZE_TITULO + 2);

				if (cliente.getEmail() != null) {
					contenido.beginText();
					contenido.setFont(FONT_BOLD, FONT_SIZE_TITULO);
					contenido.newLineAtOffset(margin, y);
					contenido.showText("E-mail:");
					contenido.setFont(FONT_REGULAR, FONT_SIZE_TITULO);
					contenido.showText(" " + cliente.getEmail());
					contenido.endText();
					y -= (FONT_SIZE_TITULO + 4);
				} else {
					y -= (FONT_SIZE_TITULO + 2);
				}

				// Separador antes de ítems
				contenido.moveTo(margin, y);
				contenido.lineTo(ticketSize.getWidth() - margin, y);
				contenido.stroke();
				y -= (FONT_SIZE_TITULO + 4);

				// ===== Encabezado Tabla =====
				float colNameX = margin;
				float colCantX = margin + width * 0.25f;
				float colUnitX = margin + width * 0.35f;
				float colDctoX = margin + width * 0.6f;
				float colTotalX = margin + width * 0.75f;
				contenido.beginText();
				contenido.setFont(FONT_BOLD, FONT_SIZE_TITULO);
				contenido.newLineAtOffset(colNameX, y);
				contenido.showText("Nombre");
				contenido.newLineAtOffset(colCantX - colNameX, 0);
				contenido.showText("Cant");
				contenido.newLineAtOffset(colUnitX - colCantX, 0);
				contenido.showText("Unit");
				contenido.newLineAtOffset(colDctoX - colUnitX, 0);
				contenido.showText("Dcto");
				contenido.newLineAtOffset(colTotalX - colDctoX, 0);
				contenido.showText("Total");
				contenido.endText();
				y -= (FONT_SIZE_TITULO + 4);

				// ===== Detalle Ítems =====
				contenido.setFont(FONT_REGULAR, FONT_SIZE_TITULO);
				for (ItemFactura item : items) {
					// 1. Truncar nombre a 10 chars + "..."
					int maxChars = 10;
					String nombre = item.nombre.length() > maxChars ? item.nombre.substring(0, maxChars) + "..."
							: item.nombre;

					// Nombre
					contenido.beginText();
					contenido.newLineAtOffset(colNameX, y);
					contenido.showText(nombre);
					contenido.endText();

					// Cantidad
					contenido.beginText();
					contenido.newLineAtOffset(colCantX, y);
					contenido.showText(String.valueOf(item.cantidad));
					contenido.endText();

					// Unitario (precio lista)
					contenido.beginText();
					contenido.newLineAtOffset(colUnitX, y);
					contenido.showText(FormatoUtil.formatDecimal(BigDecimal.valueOf(item.precioUnitario)));
					contenido.endText();

					// Descuento (reserva espacio aunque vacío)
					contenido.beginText();
					contenido.newLineAtOffset(colDctoX, y);
					String dctoText = item.descuento > 0 ? item.descuento + "%" : "";
					contenido.showText(dctoText);
					contenido.endText();

					// Total
					BigDecimal qty = BigDecimal.valueOf(item.cantidad);
					BigDecimal unitPrice = BigDecimal.valueOf(item.precioUnitario);
					BigDecimal discountPct = BigDecimal.valueOf(item.descuento).divide(BigDecimal.valueOf(100), 4,
							RoundingMode.HALF_UP);

					BigDecimal bruto = unitPrice.multiply(qty);
					BigDecimal total = bruto.multiply(BigDecimal.ONE.subtract(discountPct)).setScale(2,
							RoundingMode.HALF_UP);

					contenido.beginText();
					contenido.newLineAtOffset(colTotalX, y);
					contenido.showText(FormatoUtil.formatDecimal(total));
					contenido.endText();

					// Avanzar línea
					y -= (FONT_SIZE_TITULO + 4);
				}

				// Separador antes de totales
				contenido.moveTo(margin, y);
				contenido.lineTo(ticketSize.getWidth() - margin, y);
				contenido.stroke();
				y -= (FONT_SIZE_TITULO + 6);

				// ===== Totales finales =====
				float labelX = margin;
				float valueX = margin + width;
				// Subtotal
				contenido.beginText();
				contenido.setFont(FONT_BOLD, FONT_SIZE_TITULO);
				contenido.newLineAtOffset(labelX, y);
				contenido.showText("Subtotal:");
				contenido.endText();
				contenido.beginText();
				contenido.setFont(FONT_REGULAR, FONT_SIZE_TITULO);
				float textWidth = FONT_REGULAR.getStringWidth(FormatoUtil.formatDecimal(subTotal)) / 1000
						* FONT_SIZE_TITULO;
				contenido.newLineAtOffset(valueX - textWidth, y);
				contenido.showText(FormatoUtil.formatDecimal(subTotal));
				contenido.endText();
				y -= (FONT_SIZE_TITULO + 2);
				// IVA
				contenido.beginText();
				contenido.setFont(FONT_BOLD, FONT_SIZE_TITULO);
				contenido.newLineAtOffset(labelX, y);
				contenido.showText("IVA:");
				contenido.endText();
				contenido.beginText();
				contenido.setFont(FONT_REGULAR, FONT_SIZE_TITULO);
				textWidth = FONT_REGULAR.getStringWidth(FormatoUtil.formatDecimal(totalIVA)) / 1000 * FONT_SIZE_TITULO;
				contenido.newLineAtOffset(valueX - textWidth, y);
				contenido.showText(FormatoUtil.formatDecimal(totalIVA));
				contenido.endText();
				y -= (FONT_SIZE_TITULO + 2);
				// Descuento
				if (totalDcto.compareTo(BigDecimal.ZERO) > 0) {
					contenido.beginText();
					contenido.setFont(FONT_BOLD, FONT_SIZE_TITULO);
					contenido.newLineAtOffset(labelX, y);
					contenido.showText("Descuento:");
					contenido.endText();
					contenido.beginText();
					contenido.setFont(FONT_REGULAR, FONT_SIZE_TITULO);
					textWidth = FONT_REGULAR.getStringWidth(FormatoUtil.formatDecimal(totalDcto)) / 1000
							* FONT_SIZE_TITULO;
					contenido.newLineAtOffset(valueX - textWidth, y);
					contenido.showText(FormatoUtil.formatDecimal(totalDcto));
					contenido.endText();
					y -= (FONT_SIZE_TITULO + 2);
				}
				// Total
				contenido.beginText();
				contenido.setFont(FONT_BOLD, FONT_SIZE_TITULO);
				contenido.newLineAtOffset(labelX, y);
				contenido.showText("Total:");
				contenido.endText();
				contenido.beginText();
				contenido.setFont(FONT_REGULAR, FONT_SIZE_TITULO);
				textWidth = FONT_REGULAR.getStringWidth(FormatoUtil.formatDecimal(totalVenta)) / 1000
						* FONT_SIZE_TITULO;
				contenido.newLineAtOffset(valueX - textWidth, y);
				contenido.showText(FormatoUtil.formatDecimal(totalVenta));
				contenido.endText();
				float yPos = y - (FONT_SIZE_TITULO + 2); // empieza un poco más abajo que el Total
				for (FacturaImpuesto imp : impuestos) {
					if (imp.getPorcentaje().compareTo(BigDecimal.ZERO) == 0
							|| imp.getValor().compareTo(BigDecimal.ZERO) == 0) {
						continue;
					}
					// Etiqueta IVA X %:
					contenido.beginText();
					contenido.setFont(FONT_BOLD, FONT_SIZE_TITULO);
					contenido.newLineAtOffset(labelX, yPos);
					contenido.showText("IVA " + imp.getPorcentaje().setScale(2, RoundingMode.HALF_UP) + "%:");
					contenido.endText();
					// Valor
					contenido.beginText();
					contenido.setFont(FONT_REGULAR, FONT_SIZE_TITULO);
					contenido.newLineAtOffset(
							valueX - (FONT_REGULAR.getStringWidth(FormatoUtil.formatDecimal(imp.getValor())) / 1000
									* FONT_SIZE_TITULO),
							yPos);
					contenido.showText(FormatoUtil.formatDecimal(imp.getValor()));
					contenido.endText();

					yPos -= (FONT_SIZE_TITULO + 2);
				}
			}
			documento.save(archivo);
		}
		return archivo;
	}

	/**
	 * Divide un texto en varias líneas para que quepa dentro de un ancho dado.
	 */
	private List<String> wrapText(String text, PDFont font, float fontSize, float maxWidth) throws IOException {
		List<String> lines = new ArrayList<>();
		String[] words = text.split(" ");
		StringBuilder line = new StringBuilder();
		for (String word : words) {
			String tmp = line.length() == 0 ? word : line + " " + word;
			float size = font.getStringWidth(tmp) / 1000 * fontSize;
			if (size > maxWidth && line.length() > 0) {
				lines.add(line.toString());
				line = new StringBuilder(word);
			} else {
				line = new StringBuilder(tmp);
			}
		}
		if (line.length() > 0)
			lines.add(line.toString());
		return lines;
	}

	/**
	 * Dibuja subtotal, cada porcentaje de IVA y sus valores, descuento y total
	 */
	private void dibujarTotalesConImpuestos(PDPageContentStream contenido, float xStart, float yStart,
			BigDecimal subTotal, List<FacturaImpuesto> impuestos, BigDecimal totalDcto, BigDecimal totalVenta)
			throws IOException {
		final float rowHeight = 14f;
		PDType1Font bold = PDType1Font.HELVETICA_BOLD;
		PDType1Font regular = PDType1Font.HELVETICA;
		float labelX = xStart;
		float valueX = xStart + 200; // ajusta este offset según tu layout
		float y = yStart;

		// 1) Subtotal
		contenido.beginText();
		contenido.setFont(bold, 10);
		contenido.newLineAtOffset(labelX, y);
		contenido.showText("SUBTOTAL:");
		contenido.endText();

		contenido.beginText();
		contenido.setFont(regular, 10);
		contenido.newLineAtOffset(valueX, y);
		contenido.showText(FormatoUtil.formatDecimal(subTotal));
		contenido.endText();

		y -= rowHeight;

		// 2) Desglose de IVA por porcentaje
		for (FacturaImpuesto imp : impuestos) {
			if (imp.getPorcentaje().compareTo(BigDecimal.ZERO) == 0 || imp.getValor().compareTo(BigDecimal.ZERO) == 0) {
				continue;
			}
			contenido.beginText();
			contenido.setFont(bold, 10);
			contenido.newLineAtOffset(labelX, y);
			contenido.showText("IVA " + imp.getPorcentaje().setScale(2, RoundingMode.HALF_UP) + " %:");
			contenido.endText();

			contenido.beginText();
			contenido.setFont(regular, 10);
			contenido.newLineAtOffset(valueX, y);
			contenido.showText(FormatoUtil.formatDecimal(imp.getValor()));
			contenido.endText();

			y -= rowHeight;
		}

		// 3) Descuento (si aplica)
		if (totalDcto.compareTo(BigDecimal.ZERO) > 0) {
			contenido.beginText();
			contenido.setFont(bold, 10);
			contenido.newLineAtOffset(labelX, y);
			contenido.showText("DESCUENTO:");
			contenido.endText();

			contenido.beginText();
			contenido.setFont(regular, 10);
			contenido.newLineAtOffset(valueX, y);
			contenido.showText(FormatoUtil.formatDecimal(totalDcto));
			contenido.endText();

			y -= rowHeight;
		}

		// 4) Total final
		contenido.beginText();
		contenido.setFont(bold, 10);
		contenido.newLineAtOffset(labelX, y);
		contenido.showText("TOTAL:");
		contenido.endText();

		contenido.beginText();
		contenido.setFont(regular, 10);
		contenido.newLineAtOffset(valueX, y);
		contenido.showText(FormatoUtil.formatDecimal(totalVenta));
		contenido.endText();
	}

}
