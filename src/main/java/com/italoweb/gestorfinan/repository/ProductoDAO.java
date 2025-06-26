package com.italoweb.gestorfinan.repository;

import org.hibernate.SessionFactory;
import org.hibernate.Session;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

import org.hibernate.query.Query;
import com.italoweb.gestorfinan.model.Producto;
import com.italoweb.gestorfinan.util.HibernateUtil;

public class ProductoDAO extends GenericDAOImpl<Producto, Long> {
	public ProductoDAO() {
		super(Producto.class);
	}

	public boolean existeProductoConCodigo(String codigo) {
		try (Session session = HibernateUtil.getSessionFactory().openSession()) {
			String hql = "SELECT 1 FROM Producto p WHERE p.codigo = :codigo";
			return session.createQuery(hql, Integer.class).setParameter("codigo", codigo).setMaxResults(1)
					.uniqueResultOptional().isPresent();
		}
	}

	/**
	 * Obtiene productos cuya fechaVencimiento cae en la semana actual (lunes a
	 * domingo).
	 */
	public List<Producto> obtenerPorVencerSemanaActual() {
		LocalDate hoy = LocalDate.now();
		// Lunes de esta semana (incluye hoy si es lunes)
		LocalDate lunes = hoy.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
		LocalDate domingo = lunes.plusDays(6);
		LocalDateTime inicio = lunes.atStartOfDay();
		LocalDateTime fin = domingo.atTime(LocalTime.MAX);

		try (Session session = HibernateUtil.getSessionFactory().openSession()) {
			Query<Producto> q = session.createQuery("from Producto p where p.fechaVencimiento between :inicio and :fin",
					Producto.class);
			q.setParameter("inicio", inicio);
			q.setParameter("fin", fin);
			return q.list();
		}
	}

	/**
	 * Obtiene productos cuya fechaVencimiento cae en la semana siguiente a la
	 * actual (lunes a domingo siguiente).
	 */
	public List<Producto> obtenerPorVencerSemanaSiguiente() {
		LocalDate hoy = LocalDate.now();
		// Lunes de esta semana
		LocalDate lunesEsta = hoy.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
		// Lunes de la siguiente semana
		LocalDate lunesSig = lunesEsta.plusWeeks(1);
		LocalDate domingoSig = lunesSig.plusDays(6);
		LocalDateTime inicio = lunesSig.atStartOfDay();
		LocalDateTime fin = domingoSig.atTime(LocalTime.MAX);

		try (Session session = HibernateUtil.getSessionFactory().openSession()) {
			Query<Producto> q = session.createQuery("from Producto p where p.fechaVencimiento between :inicio and :fin",
					Producto.class);
			q.setParameter("inicio", inicio);
			q.setParameter("fin", fin);
			return q.list();
		}
	}
}