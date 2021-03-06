package logUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class LogUtils {
    
	//private static String filePathMac = "/Users/Javi/Desktop/Logs/";
	private static String filePathWindows = "C:\\Logs\\";
	private static FileWriter fileWriter;
	private static FileWriter fileWriterCSV;
    
	static {
		File fichero = new File(filePathWindows + "csv_" + getActualTime()[1] + ".csv");
		if (!fichero.exists()) {
			try {
				
				fichero.createNewFile();
				// Introducimos la cabecera solo una vez
				fileWriterCSV = new FileWriter(filePathWindows + "csv_" + getActualTime()[1] + ".csv", true);
				fileWriterCSV.append("ID Nodo");
				fileWriterCSV.append(",");
				fileWriterCSV.append("Sensor");
				fileWriterCSV.append(",");
				fileWriterCSV.append("Valor");
				fileWriterCSV.append(",");
				fileWriterCSV.append("Fecha");
				fileWriterCSV.append(",");
				fileWriterCSV.append("Hora");
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
	}
	
	/**
	 *
	 * @return Array de String con fecha, hora y ambos
	 */
	private static String[] getActualTime() {
		String tiempo[] = { "", "", "" };
		Date ahora = new Date();
		SimpleDateFormat formateador = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
		tiempo[0] = formateador.format(ahora).toString();

		formateador = new SimpleDateFormat("dd-MM-yyyy");
		tiempo[1] = formateador.format(ahora).toString();
		
		formateador = new SimpleDateFormat("HH:mm:ss");
		tiempo[2] = formateador.format(ahora).toString();

		return tiempo;
	}
    
	public static void escribirCSV(String nodo, String sensor, String valor) {
		try {
			fileWriterCSV = new FileWriter(filePathWindows + "csv_" + getActualTime()[1] + ".csv", true);
			
			fileWriterCSV.append(nodo);
			fileWriterCSV.append(",");
			fileWriterCSV.append(sensor);
			fileWriterCSV.append(",");
			fileWriterCSV.append(valor);
			fileWriterCSV.append(",");
			fileWriterCSV.append(getActualTime()[1]);
			fileWriterCSV.append(",");
			fileWriterCSV.append(getActualTime()[2]);
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
			File fichero = new File(filePathWindows + "log_" + getActualTime()[1] + ".txt");
			if (!fichero.exists()) fichero.createNewFile();

			fileWriter = new FileWriter(filePathWindows + "log_" + getActualTime()[1] + ".txt", true);
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
			File fichero = new File(filePathWindows + "log_" + getActualTime()[1] + ".txt");
			if (!fichero.exists()) fichero.createNewFile();

			fileWriter = new FileWriter(filePathWindows + "log_" + getActualTime()[1] + ".txt", true);
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
