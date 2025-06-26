package com.italoweb.gestorfinan.dto;

import java.math.BigDecimal;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class CompraEstadisticaDTO {

    private String periodo;
    private BigDecimal cantidad;

    public CompraEstadisticaDTO(String periodo, BigDecimal cantidad) {
        this.periodo = periodo;
        this.cantidad = cantidad;
    }

    public String getPeriodo() {
        return periodo;
    }

    public BigDecimal getCantidad() {
        return cantidad;
    }

    public String getCantidadFormateada() {
        if (cantidad == null) return "$ 0";
        return "$ " + String.format("%,.2f", cantidad); // Formato con coma como separador de miles
    }
}

