package logUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class LogUtils {

	private static FileWriter fileWriter;
	private static FileWriter fileWriterCSV;
    
	static {
		// Introducimos la cabecera sólo una vez
		try {
			File fichero = new File("/Users/Javi/eclipse-workspace/AlarmadoServer/Logs/csv_" + getActualTime()[1] + ".csv");
			if (!fichero.exists()) {
				fichero.createNewFile();
				
				fileWriterCSV = new FileWriter("/Users/Javi/eclipse-workspace/AlarmadoServer/Logs/csv_" + getActualTime()[1] + ".csv", true);
				fileWriterCSV.append("ID Nodo");
				fileWriterCSV.append(",");
				fileWriterCSV.append("Sensor");
				fileWriterCSV.append(",");
				fileWriterCSV.append("Valor");
				fileWriterCSV.append(",");
				fileWriterCSV.append("Fecha");
				fileWriterCSV.append("\n");
			}

		} catch (IOException e) {
			e.printStackTrace();

		} finally {
			try {
				fileWriterCSV.close();
			} catch (IOException e) {

			}
		}
	}
	
	private static String[] getActualTime() {
		String tiempo[] = { "", "" };
		Date ahora = new Date();
		SimpleDateFormat formateador = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
		tiempo[0] = formateador.format(ahora).toString();

		formateador = new SimpleDateFormat("dd-MM-yyyy");
		tiempo[1] = formateador.format(ahora).toString();

		return tiempo;
	}
    
	public static void escribirCSV(String nodo, String sensor, String valor) {
		try {
			fileWriterCSV = new FileWriter("/Users/Javi/eclipse-workspace/AlarmadoServer/Logs/csv_" + getActualTime()[1] + ".csv", true);
			
			fileWriterCSV.append(nodo);
			fileWriterCSV.append(",");
			fileWriterCSV.append(sensor);
			fileWriterCSV.append(",");
			fileWriterCSV.append(valor);
			fileWriterCSV.append(",");
			fileWriterCSV.append(getActualTime()[0]);
			fileWriterCSV.append("\n");

		} catch (IOException e) {
			e.printStackTrace();

		} finally {
			try {
				fileWriterCSV.close();
			} catch (IOException e) {

			}
		}
	}
	
	public static void escribirLog(String mensaje) {

		try {
			File fichero = new File("/Users/Javi/eclipse-workspace/AlarmadoServer/Logs/log_" + getActualTime()[1] + ".txt");
			if (!fichero.exists()) fichero.createNewFile();

			fileWriter = new FileWriter("/Users/Javi/eclipse-workspace/AlarmadoServer/Logs/log_" + getActualTime()[1] + ".txt", true);
			fileWriter.write("[" + getActualTime()[0] + "] : INFO : " + mensaje + "\n");

		} catch (IOException e) {
			e.printStackTrace();

		} finally {
			try {
				fileWriter.close();
			} catch (IOException e) {

			}
		}
	}

	public static void escribirExcepcion(String mensaje, Exception exc) {
		
		try {
			File fichero = new File("/Users/Javi/eclipse-workspace/AlarmadoServer/Logs/log_" + getActualTime()[1] + ".txt");
			if (!fichero.exists()) fichero.createNewFile();

			fileWriter = new FileWriter("/Users/Javi/eclipse-workspace/AlarmadoServer/Logs/log_" + getActualTime()[1] + ".txt", true);
			fileWriter.write("[" + getActualTime()[0] + "] : ERROR : " + mensaje + "\n");
			fileWriter.write("\t\t" + exc.getMessage() + "\n");

		} catch (IOException e) {
			e.printStackTrace();

		} finally {
			try {
				fileWriter.close();
			} catch (IOException e) {

			}
		}
	}
}
