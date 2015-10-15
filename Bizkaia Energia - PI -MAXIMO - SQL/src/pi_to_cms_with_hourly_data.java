
import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class pi_to_cms_with_hourly_data {
	
	static Propiedades oPropiedades = new Propiedades();
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		Log.info("Iniciada la importación ficheros sistema PI al CMS"); 
		
		//Cargar propiedades
		oPropiedades.load();
		if (oPropiedades.isLoaded()) {	
			
			boolean bucle = oPropiedades.out_generacion_bucle;
			
			do {	
			
				esperar_al_minuto(oPropiedades.out_generacion_minuto);

				try {
						//Obtenemos la hora de referencia de la medida
						Fecha fecha_menos_1H = new Fecha();
						fecha_menos_1H.setFecha(Fecha.getFecha(-1)); 
					
						//Se obtiene el snapshot de FiscalMeter.Tag
						String archivo_entrada_1 = "0ZIVtag.txt";
						String texto_entrada_1 = "@echo off\r\n@table pisnap\r\n@mode list\r\n@ostr tag, value\r\n@sele tag=FiscalMeter.Tag\r\n@ends\r\n";					
						Fichero.crear(archivo_entrada_1,texto_entrada_1);
						String archivo_salida_1 = oPropiedades.cms_out_path_temporal + "AmorebietaCMSPlantData_" + fecha_menos_1H.getString() + "_ZIV.dat";
						String cmd_1 = oPropiedades.cmd_pi_configuracion + " < " + archivo_entrada_1 + " > " + archivo_salida_1;
						ejecutar(cmd_1);	
						Fichero.mover(archivo_salida_1,oPropiedades.cms_out_path_importacion);
					
						//Espera 10000 milisegundos
						Thread.sleep(10000);
	
						// HOURLY VALUES PLANT DATA
						// A veces los valores .TAG se quedan sin recalcular.
						// Por eso añado esta función para recalcular 2 horas hacia atrás antes exportar los valores
						// A continuación se obtiene el snapshot de los Tag indicados por properties
						
						String archivo_entrada_2 = oPropiedades.file_out_comandos;
						String texto_entrada_2 = "@echo off\r\n@table pisnap\r\n@mode list\r\n@ostr tag, value\r\n";
						for (int I = oPropiedades.tag_num; (--I) >= 0;) {
							recalcularTag(oPropiedades.tag[I],oPropiedades.cmd_pi_horas_previas);
							texto_entrada_2 += "@sele tag=" + oPropiedades.tag[I] + "\r\n@ends\r\n" ;
						}					
						Fichero.crear(archivo_entrada_2,texto_entrada_2);
						String archivo_salida_2 = oPropiedades.cms_out_path_temporal + oPropiedades.file_out_resultados + fecha_menos_1H.getString() + ".dat";
						String cmd_2 = oPropiedades.cmd_pi_configuracion + " < "+ archivo_entrada_2 +" > " + archivo_salida_2;
						ejecutar(cmd_2);
						Fichero.mover(archivo_salida_2,oPropiedades.cms_out_path_importacion);
					
				} catch (IOException ioe) {
					ioe.printStackTrace();
					Log.excepcion(ioe);
				}catch (InterruptedException ie) {
					ie.printStackTrace();
					Log.excepcion(ie);
				}	 
			} while (bucle == true);
		} else {
			Log.error("Propiedades de la aplicación no cargadas"); 
		}

	}
	
	/** Ejecuta comandos en PI
	 * @param sComando
	 */	
	
	private static void ejecutar(String sComando) throws IOException, InterruptedException {
		Runtime rt = Runtime.getRuntime();
		Process pr = rt.exec(sComando);
		BufferedReader input = new BufferedReader(new InputStreamReader(pr.getInputStream()));
		String line=null;
		while((line=input.readLine()) != null) {
			Log.debug(line); 
			int exitVal = pr.waitFor();
			Log.debug("Comando: " + sComando + ". Resultado: "+exitVal); 
		}
	}
	
	/** A veces los valores .TAG se quedan sin recalcular.
	 * 	Por eso añado esta función para recalcular 2 horas hacia atrás antes exportar los valores
	 * @param sNombreTag
	 */	

	private static void recalcularTag(String sNombreTag, int nHorasPrevias) throws IOException, InterruptedException {
		ejecutar(oPropiedades.cmd_pi_recalculo + " /ex=" +sNombreTag +",*-" + nHorasPrevias + "h,*");
	}

	/** Se establece un retardo hasta que el minuto de la hora actual coincida con el minuto para
	 * @param nMinuto
	 */	

	private static void esperar_al_minuto(int nMinuto)  {
		Log.info("Se generará el ficheros en los " + nMinuto + " minutos de cada hora");
		if (nMinuto > 0) {
			Integer	generar_ahora= 0;
			do {
				//Espera un tiempo 
				try {
					//Espera 10000 milisegundos
					Thread.sleep(10000);
	
					//Cogemos fecha y hora actuales.
					Fecha fecha_actual= new Fecha();
					fecha_actual.setFecha(Fecha.getFecha()); 
	
					String nuevo_bucle_a_generar = fecha_actual.getString();
					
					 //importante admiración delante de ultimo_bucle_generado ....
					 if ( (Integer.parseInt(fecha_actual.getMinutos())-nMinuto) == 0 && !oPropiedades.out_generacion_ultimo.equals(nuevo_bucle_a_generar) ){
						 generar_ahora = 1;
						 oPropiedades.out_generacion_ultimo = nuevo_bucle_a_generar;
					 } else {
						 generar_ahora = 0;
					 }
				} catch (InterruptedException e) {
					e.printStackTrace();
					Log.excepcion(e);
				}
					
			} while(generar_ahora != 1);	
		}
	}

}
