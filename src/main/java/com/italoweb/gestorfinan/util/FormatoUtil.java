package com.italoweb.gestorfinan.util;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Locale;

public class FormatoUtil {

	// Formatos de fecha
	private static final SimpleDateFormat FORMATO_FECHA = new SimpleDateFormat("dd/MM/yyyy");
	private static final SimpleDateFormat FORMATO_ISO = new SimpleDateFormat("yyyy-MM-dd");
	private static final SimpleDateFormat FORMATO_COMPLETO = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");

	// Formato numérico
	private static final NumberFormat FORMATO_MONEDA = new DecimalFormat("#,##0.00");

	// --- CONVERSIÓN ENTRE TIPOS ---

	public static LocalDate convertirADateLocal(Date date) {
		return date != null ? date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate() : null;
	}

	public static Date convertirALocalDateTime(LocalDateTime localDateTime) {
		if (localDateTime == null) {
			return null;
		}
		return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
	}

	public static Date convertirALocalDateDate(LocalDate localDate) {
		return localDate != null ? Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant()) : null;
	}

	// --- FORMATEADORES ---

	public static String formatearFecha(Date fecha) {
		return fecha != null ? FORMATO_FECHA.format(fecha) : "";
	}

	public static String formatearFechaISO(Date fecha) {
		return fecha != null ? FORMATO_ISO.format(fecha) : "";
	}

	public static String formatearFechaCompleta(Date fecha) {
		return fecha != null ? FORMATO_COMPLETO.format(fecha) : "";
	}

	public static String formatearMoneda(BigDecimal valor) {
		return valor != null ? FORMATO_MONEDA.format(valor) : "0.00";
	}
	
	public static String convertirAStringLocal(LocalDate fecha) {
	    if (fecha == null) return "";
	    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
	    return fecha.format(formatter);
	}

	// --- PARSEADORES ---

	public static Date parsearFecha(String fechaStr) {
		try {
			return fechaStr != null && !fechaStr.trim().isEmpty() ? FORMATO_FECHA.parse(fechaStr.trim()) : null;
		} catch (Exception e) {
			return null;
		}
	}

	public static BigDecimal parsearMoneda(String valorStr) {
		try {
			if (valorStr == null || valorStr.trim().isEmpty())
				return BigDecimal.ZERO;
			Number number = FORMATO_MONEDA.parse(valorStr.trim());
			return new BigDecimal(number.toString());
		} catch (ParseException e) {
			return BigDecimal.ZERO;
		}
	}

	// --- VALIDADORES ---

	public static boolean esNumerico(String valor) {
		if (valor == null || valor.trim().isEmpty())
			return false;
		try {
			new BigDecimal(valor.trim());
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	public static boolean esFechaValida(String fechaStr) {
		try {
			FORMATO_FECHA.parse(fechaStr.trim());
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	// --- FECHAS COMUNES ---

	public static String obtenerFechaActualFormateada() {
		return formatearFecha(new Date());
	}

	public static Date obtenerFechaActual() {
		return new Date();
	}

	public static String formatearFechaCompleta(LocalDateTime fecha) {
		if (fecha == null)
			return "";
		Date date = convertirALocalDateTime(fecha);
		return formatearFechaCompleta(date);
	}

	public static boolean esEmailValido(String email) {
		if (email == null || email.isEmpty()) {
			return false;
		}
		String regex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";
		return email.matches(regex);
	}
	
	// ---  MONETARIOS ----
	@SuppressWarnings("deprecation")
	public static String formatDecimal(BigDecimal value) {
		if (value == null)
			return "0,00"; // Notar coma si es formato CO
		NumberFormat nf = NumberFormat.getNumberInstance(new Locale("es", "CO"));
		nf.setMinimumFractionDigits(2);
		nf.setMaximumFractionDigits(2);
		return nf.format(value);
	}

}
