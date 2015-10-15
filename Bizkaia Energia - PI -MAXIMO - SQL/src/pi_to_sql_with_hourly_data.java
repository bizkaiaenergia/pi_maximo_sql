
import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Hashtable;
import java.util.Stack;
import java.util.Vector;

public class pi_to_sql_with_hourly_data {
	
	private static Propiedades oPropiedades = new Propiedades();
	
	private static Connection conexion = null;
	
	private static Hashtable<String,Stack<String[]>> tTags = new Hashtable<String, Stack<String[]>>();

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		Log.info("Iniciada la importación ficheros sistema PI al SQL Server"); 

		//Cargar propiedades
		oPropiedades.load();
		
		if (oPropiedades.isLoaded()) {
			
			boolean bucle = oPropiedades.out_generacion_bucle;
			
			do {	
				
				if (oPropiedades.out_generacion_minuto > 0)
					esperar_al_minuto(oPropiedades.out_generacion_minuto);

				try {
					
						//OBTENCION DE LOS DATOS DE PI
			
						//Obtenemos la hora de referencia de la medida
						Fecha fecha_menos_1H = new Fecha();
						fecha_menos_1H.setFecha(Fecha.getFecha(-1)); 
	
						// HOURLY VALUES PLANT DATA
						// A veces los valores .TAG se quedan sin recalcular.
						// Por eso añado esta función para recalcular 2 horas hacia atrás antes exportar los valores
						// A continuación se obtiene el snapshot de los Tag indicados por properties
						
						String archivo_entrada = oPropiedades.file_out_comandos;
						String texto_entrada = "@echo off\r\n@table pisnap\r\n@mode list\r\n@ostr tag, value\r\n";
						for (int I = oPropiedades.tag_num; (--I) >= 0;) {
							recalcularTag(oPropiedades.tag[I],oPropiedades.cmd_pi_horas_previas);
							texto_entrada += "@sele tag=" + oPropiedades.tag[I] + "\r\n@ends\r\n" ;
						}					
						Fichero.crear(archivo_entrada,texto_entrada);
						String archivo_salida = oPropiedades.cms_out_path_temporal + oPropiedades.file_out_resultados + fecha_menos_1H.getString() + ".dat";
						String cmd = oPropiedades.cmd_pi_configuracion + " < "+ archivo_entrada +" > " + archivo_salida;
						ejecutar(cmd);

						try {
							abrirConexionBBDD();
							if (conexion!=null) {
								//Cargamos la tabla que relaciona el TAG con el ACTIVO correspondiente
								cargarTagActivo();							
								//Guardamos los resultados del PI en Maximmo
								guardarMedidasTagActivo(archivo_salida);
								cerrarConexionBBDD();
							}
						} catch (SQLException e) {
							e.printStackTrace();
							Log.excepcion(e);
						}
						
					
				} catch (IOException ioe) {
					ioe.printStackTrace();
					Log.excepcion(ioe);
				} catch (InterruptedException ie) {
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
			Log.debug("Comando: " + sComando + " // Resultado: "+exitVal); 
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
	
	/** Abrir conexion a maximo
	 * @throws SQLException 
	 * @result true si la operacion ha sido correcta o false si ha sido erronea
	 */	
	
	private static void abrirConexionBBDD() throws SQLException {
		if (conexion == null) {
				conexion = SQL.getConexion(oPropiedades);
		}
	}
	
	/** Cerrar conexion a maximo
	 * @throws SQLException 
	 * @result true si la operacion ha sido correcta o false si ha sido erronea
	 */	
	
	private static void cerrarConexionBBDD() throws SQLException {
		if (conexion != null) {
			conexion.close();
			conexion=null;
		}
	}
	
	/** Consulta maximo para obtener la relación de Tag con el Activo correspondiente
	 * @param sNombreTag
	 */	

	@SuppressWarnings("rawtypes")
	private static void cargarTagActivo() {
		
		String sSQL = "SELECT [metername],[assetmeterid],[assetnum] FROM [MAXDB75].[dbo].[assetmeter] order by [metername]";
				
		Vector vResultado= SQL.obtener(conexion,sSQL);
		
		String sResultado = (String)vResultado.get(1);
		Hashtable tResultado = (Hashtable) vResultado.get(0);
		Log.debug("Sentencia: " +sSQL+ " // Nº Resultados: " + tResultado.size() + " // Resultado: " + sResultado);
		
		if (sResultado.equals(SQL.SENTENCIA_CORRECTA) &&  tResultado.size() > 0 ) {
	        int nColumnas=((Integer)tResultado.get("f0c0")).intValue();
			int nFilas=((tResultado.size()-1)/nColumnas)-1;
			String sIdTag = tResultado.get("f1c1").toString();
			Stack<String[]> pIndices = new Stack<String[]>(); 
			for(int i=1; i<=nFilas; i++) {
				//Cuando cambia el tag
				if (!tResultado.get("f"+i+"c1").toString().equals(sIdTag)) {
					//Salvamos la pila de pares id meter - id asset asociados al tag
					tTags.put(sIdTag.toUpperCase(), pIndices);
					//Y reiniciamos la pila de indices
					sIdTag = tResultado.get("f"+i+"c1").toString();
					pIndices = new Stack<String[]>(); 
				}

				//Añadimos un par de valores (id. metrica - id. asset) asociados a ese tag 
				String sIdMeter = tResultado.get("f"+i+"c2").toString();
				String sIdAsset = tResultado.get("f"+i+"c3").toString();
				String[] sValorTag = {sIdMeter,sIdAsset};
				pIndices.push(sValorTag); 
				
				//Log.debug("TAG: " + sIdTag + " // METER: " +sIdMeter+ " // ASSET: " +sIdAsset);
			}
			//Guardamos el ultimo tag
			tTags.put(sIdTag.toUpperCase(), pIndices);	
		}

	}
	
	/** Guearda en maximo las medidas de los tags
	 * @param sNombreTag
	 * @throws IOException 
	 */	

	private static void guardarMedidasTagActivo(String sNombreArchivo) throws IOException {
		
		//Creamos las sentencias para ejecutar en el SQL Server
		StringBuffer sbSQL = new StringBuffer(); 
		sbSQL.append(oPropiedades.bbdd_out_transaccion_init).append(SQL.SEPARADOR_SENTENCIAS);
		
		
		//Borramos las transacción
		sbSQL.append("DELETE FROM [MAXDB75].[dbo].mxin_inter_trans where ifacename = 'ASI_METER_IN'").append(SQL.SEPARADOR_SENTENCIAS);
		sbSQL.append("DELETE FROM [MAXDB75].[dbo].asi_meter_in").append(SQL.SEPARADOR_SENTENCIAS);
		
		@SuppressWarnings("rawtypes")
		Vector vResultado = Fichero.leer(sNombreArchivo);
		int nContador = (Integer) vResultado.get(0);
		Fecha fecha_actual= new Fecha();
		fecha_actual.setFecha(Fecha.getFecha()); 
		long nTransId = Long.valueOf(fecha_actual.getString());
		@SuppressWarnings("unchecked")
		Hashtable<Integer,String> tLineas = (Hashtable<Integer, String>) vResultado.get(1);
		int contadorId=0;
		for (int i = 0; i<nContador; i++ ) {
			String sLinea = tLineas.get(i);
			String[] asValores = sLinea.split(",");
			if (asValores.length == 2) {
				String sTagNum = asValores[0];
				String sValor = asValores[1];
				
				//Redondeamos el valor a 2 decimales
				try {
					double nValor = Double.parseDouble(sValor);
					double nValorRedondeado = Math.rint(nValor*100)/100;
					sValor = String.valueOf(nValorRedondeado);
				} catch (NumberFormatException nfe) {
					Log.error("El valor del TAG " + sTagNum + " no es numerico: " + sValor);
				}
				
				//Cambiamos punto por coma decimal para que lo interprete Maximo 
				sValor = sValor.replace('.', ',');
				
				Log.info(sTagNum + "=" + sValor);
				
				Stack<String[]> pIndices = tTags.get(sTagNum.toUpperCase());
				
				if (pIndices!=null) {
					while (!pIndices.empty()) {
						String[] asIndices = pIndices.pop();
					
						String sMeterNum = asIndices[0];
						String sAssetNum = asIndices[1];
					
						//Si obtenemos los codigos del asset y meter asociado 
						if (sAssetNum != null ) {
							//Log.debug( ( nTransId + i ) + " // TAG: " + sTagNum + " // ASSET:" + sAssetNum + " // METER:" + sMeterNum +" // VALOR:" + sValor);
							
							//Añadimos la transacción
							sbSQL.append("INSERT INTO [MAXDB75].[dbo].mxin_inter_trans (action, ifacename, extsysname, translanguage, transid) VALUES ('AddChange','ASI_METER_IN','ASI_LOAD','EN',").append(nTransId+contadorId).append(")").append(SQL.SEPARADOR_SENTENCIAS);
							
							//Metemos las medidas en esa transacción 
							//Podemos añadir solo una transacción (transid) y por cada linea darle un numero de secuencia diferente (transseq)
							sbSQL.append("INSERT INTO [MAXDB75].[dbo].asi_meter_in (active,assetmeterid,assetnum,lastreading,lastreadingdate,lastreadinginspctr,linearassetmeterid,metername,newreading,newreadingdate,orgid,readingtype,rolldownsource,siteid,transid,transseq) VALUES (1,")
							.append(sMeterNum).append(",'").append(sAssetNum).append("','").append(sValor).append("',").append(oPropiedades.bbdd_out_current_date).append(",'PI',0,'").append(sTagNum).append("','").append(sValor).append("',").append(oPropiedades.bbdd_out_current_date).append(",'CCGT','ACTUAL','ASSET','BOROA',").append(nTransId+contadorId).append(",0)").append(SQL.SEPARADOR_SENTENCIAS);
							contadorId++;
						}
					}
				} else {
					Log.error("El tag del medidor " + sTagNum.toUpperCase()+ " no se puede encontrar en MAXIMO o no está asignado a ningún asset");
				}
			}
		}
		sbSQL.append(oPropiedades.bbdd_out_transaccion_end).append(SQL.SEPARADOR_SENTENCIAS);
		
		String sResultado = SQL.actualizar(conexion,sbSQL.toString());	
		
		if(!sResultado.equals(SQL.SENTENCIA_CORRECTA)) 
			Log.error("Sentencia: " + sbSQL.toString() + " // Resultado: " + sResultado );
	}
	
}
