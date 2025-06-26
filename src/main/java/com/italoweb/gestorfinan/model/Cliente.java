package com.italoweb.gestorfinan.model;

import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "clientes")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Cliente {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private String nombre;

	@Column(nullable = false)
	private String nit;

	@Column(unique = true, nullable = false)
	private String email;

	@Column(nullable = false)
	private String telefono;

	@Column(nullable = false)
	private Estado estado;

	@Column(name = "tipo", nullable = false)
	private TipoProveedor tipo;

	@Column(nullable = true)
	private Date fechaCreacion;

}