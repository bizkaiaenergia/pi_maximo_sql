import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;




public class Fecha {
	
	static String LOCALE = "us_US";
	
	public static String hora;
	public static String minutos;
	public static String dia;
	public static String a�o;
	public static String a�o_largo;	
	public static String mes;
	public static String mes_corto;
	
	
	public String getLOCALE() {
		return LOCALE;
	}

	public void setLOCALE(String lOCALE) {
		LOCALE = lOCALE;
	}

	public String getHora() {
		return hora;
	}

	public void setHora(String hora) {
		Fecha.hora = hora;
	}

	public String getMinutos() {
		return minutos;
	}

	public void setMinutos(String minutos) {
		Fecha.minutos = minutos;
	}

	public String getDia() {
		return dia;
	}

	public void setDia(String dia) {
		Fecha.dia = dia;
	}

	public String getA�o() {
		return a�o;
	}

	public void setA�o(String a�o) {
		Fecha.a�o = a�o;
	}

	public String getA�o_largo() {
		return a�o_largo;
	}

	public void setA�o_largo(String a�o_largo) {
		Fecha.a�o_largo = a�o_largo;
	}

	public String getMes() {
		return mes;
	}

	public void setMes(String mes) {
		Fecha.mes = mes;
	}

	public String getMes_corto() {
		return mes_corto;
	}

	public void setMes_corto(String mes_corto) {
		Fecha.mes_corto = mes_corto;
	}

	public String getString() {
		return a�o_largo + mes_corto + dia + hora + minutos;
	}
	
	/**
	 * @param fecha
	 */
	public void setFecha(Date fecha) {
		Format formatter = new SimpleDateFormat("HH");
		hora = formatter.format(fecha);
		formatter = new SimpleDateFormat("mm");
		minutos = formatter.format(fecha);
		formatter = new SimpleDateFormat("dd");
		dia = formatter.format(fecha);
		formatter = new SimpleDateFormat("yy");
		a�o = formatter.format(fecha);
		formatter = new SimpleDateFormat("yyyy");
		a�o_largo = formatter.format(fecha);
		formatter = new SimpleDateFormat("MM");
		mes_corto = formatter.format(fecha);
		formatter = new SimpleDateFormat("MMM",new Locale(LOCALE));
		mes= formatter.format(fecha);
	}
	
	public static Date getFecha() {
		return getFecha(0,0);
	}
	
	public static Date getFecha(int iDesplazamientoEnHoras) {
		return getFecha(0,iDesplazamientoEnHoras);
	}
	
	public static Date getFecha(int iDesplazamientoEnDias,int iDesplazamientoEnHoras) {
		//Partimos de la fecha actual
		Date fechaActual = new Date();
		//Y le sumanos/restamos d�as
		if (iDesplazamientoEnDias!=0) {
			GregorianCalendar gcCalendario = new GregorianCalendar ();
			gcCalendario.setTime(fechaActual);
			gcCalendario.add(Calendar.DAY_OF_YEAR,iDesplazamientoEnDias);
			fechaActual = gcCalendario.getTime();
		//Y le sumanos/restamos horas	
		} else if (iDesplazamientoEnHoras!=0) {
			GregorianCalendar gcCalendario = new GregorianCalendar ();
			gcCalendario.setTime(fechaActual);
			gcCalendario.add(Calendar.HOUR_OF_DAY,iDesplazamientoEnHoras);
			fechaActual = gcCalendario.getTime();
		}
		return fechaActual;
	}

}
