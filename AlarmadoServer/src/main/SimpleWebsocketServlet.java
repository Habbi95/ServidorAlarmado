package main;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import com.google.gson.Gson;

import logUtils.*;

@ServerEndpoint("/actions")
public class SimpleWebsocketServlet {
	
	private static Map<String, Session> nodesID;
	private static Map<String, Integer> alarmCounter;
	private static Map<String, Boolean> alarmActive;
	private static Session frontWS;
	private static Gson gson;
	
	private static String SENSOR_TOKEN_TO_ALARM = "Pot";
	private static int thresholdHIGH = Integer.MAX_VALUE;
	private static int thresholdLOW = Integer.MIN_VALUE;
	private static int numOcurs = 5;
	
	void sendAlarmToNode(String node, String message) throws IOException{
		AlarmNotificationJSON data = new AlarmNotificationJSON();
		data.sensorID = node;
		data.value = message;
		
		nodesID.get(node).getBasicRemote().sendText(gson.toJson(data));
	}
	public SimpleWebsocketServlet() {
		nodesID = Collections.synchronizedMap(new HashMap<String, Session>());
		alarmCounter = Collections.synchronizedMap(new HashMap<String, Integer>());
		alarmActive = Collections.synchronizedMap(new HashMap<String, Boolean>());
		gson = new Gson();
	}
	
	@OnOpen
	public void open(Session session) {
		System.out.println("Conexion abierta con id " + session.getId());
		
	}

	@OnClose
	public void close(Session session) {
		for (Entry<String, Session> e : nodesID.entrySet()) 
		    if(e.getValue() == session) {
		    	nodesID.remove(e.getKey()); // Eliminamos el key a partir del value.
		    	alarmCounter.remove(e.getKey());
		    	alarmActive.remove(e.getKey());
		    }
		
		System.out.println("Conexion cerrada con id " + session.getId());
	}

	@OnError
	public void onError(Throwable error) {
		System.out.println(error.getMessage());
	}

	@OnMessage
	public void handleMessage(String message, Session session) {
		
		int containAlarmSensorIndex = -10; /* El alarmado sólo aplica para el sensor de temperatura de cada nodo.
										     Esta variable guarda la posición del array de sensorNames del JSON donde se encuentra el sensor
									         de temperatura*/
		                  
		
	    try {
	    	if(message.equals("FRONT")) frontWS = session;
	    	else if(session == frontWS) {
	    		// Si nos llega un mensaje del front, asumimos que es por el boton. 
	    		// De este modo, parseamos el json y enviamos mensaje al nodo correspondiente via Websocket.
	    		
	    		AlarmNotificationJSON data = gson.fromJson(message, AlarmNotificationJSON.class);
	    		if(data.sensorID != null && !data.sensorID.equals("")) {
	    			// Si viene informado, es un mensaje al nodo. Reenviamos el mensaje.
	    			sendAlarmToNode(data.sensorID, data.value);
	    			
	    		} else {
	    			// Si no viene informado es una actualizacion de los umbrales. O no se ha seleccionado nodo, lo cual
	    			// es irrelevante
	    			thresholdHIGH = data.thresholdHIGH;
	    			thresholdLOW = data.thresholdLOW;
	    		}
	    		
	    	} else {
	    		SensorDataJSON data = gson.fromJson(message, SensorDataJSON.class);
	    		
	    		nodesID.put(data.sensorID, session); // Asociamos la sesion al ID del nodo
	    		if (!alarmCounter.containsKey(data.sensorID) && !alarmActive.containsKey(data.sensorID)) {
	    			// Es la primera vez que entra el nodo. Inicializamos.
	    			alarmCounter.put(data.sensorID, 0);
	    			alarmActive.put(data.sensorID, true);
	    		}
	    		
	    		Date ahora = new Date();
	    		SimpleDateFormat formateador = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
	    		data.date = formateador.format(ahora);
	    		data.alarmInfo = "";
	    		
	    		System.out.println("Mensaje recibido (id: " + session.getId() + "): " + message);
	    		
	    		for (int i = 0; i < data.sensorNames.length; i++) {
	    			if(data.sensorNames[i].startsWith(SENSOR_TOKEN_TO_ALARM)) containAlarmSensorIndex = i;
	    			LogUtils.escribirLog("ID Nodo: " + data.sensorID + "\tValor bruto recogido del sensor " + data.sensorNames[i] + ": " + data.values[i]);
	    			LogUtils.escribirCSV(data.sensorID, data.sensorNames[i], String.valueOf(data.values[i]));
	    		}
	    		
	    		
	    		// PROCESADO DE LOS DATOS -- Estableceremos una serie de reglas con umbrales para enviar mensajes automaticamente a los nodos.
	    		if (alarmActive.get(data.sensorID) && containAlarmSensorIndex != -10) {
	    			if(data.values[containAlarmSensorIndex] >= thresholdHIGH) {
	    				int counter = alarmCounter.get(data.sensorID);
	    				counter = counter + 1;
	    				alarmCounter.put(data.sensorID, counter);
	    				LogUtils.escribirLog("ID Nodo: " + data.sensorID + "\tALARMADO: Superado el umbral " + thresholdHIGH + " Ocurrencia " + alarmCounter.get(data.sensorID) + " de " + numOcurs + " para enviar alarma");
	    				data.alarmInfo += data.sensorID + " Superado el umbral " + thresholdHIGH + " Ocurrencia " + alarmCounter.get(data.sensorID) + " de " + numOcurs + " para enviar alarma\n";
	    			} else if(alarmCounter.get(data.sensorID) > 0){
	    				// Se resetea la cuenta, ya que el valor es inferior al umbral
	    				alarmCounter.put(data.sensorID, 0);
	    				LogUtils.escribirLog("ID Nodo: " + data.sensorID + "\tALARMADO: Cuenta reseteada. No hay " + numOcurs + " ocurrencias consecutivas");
	    				data.alarmInfo += data.sensorID + " Cuenta reseteada. No hay " + numOcurs + " ocurrencias consecutivas\n";
	    			}
	    			
	    			if(alarmCounter.get(data.sensorID) == numOcurs) {
	    				sendAlarmToNode(data.sensorID, "ON");
	    				LogUtils.escribirLog("ID Nodo: " + data.sensorID + "\tALARMADO: Ocurrencias máximas. Se envía señal ON a nodo " + data.sensorID);
	    				data.alarmInfo += "\n" + data.sensorID + " Ocurrencias máximas. Se envía señal ON a nodo\n"; 
	    				alarmActive.put(data.sensorID, false); // Desactivamos hasta que pase al umbral mínimo
	    				alarmCounter.put(data.sensorID, 0); // Reseteamos el contador del nodo
	    			}
	    		} else if(containAlarmSensorIndex != -10 && data.values[containAlarmSensorIndex] <= thresholdLOW){
	    			// Si entramos aquí es porque el alarmado se ha desactivado. Esperamos hasta que pase al umbral mínimo
	    			sendAlarmToNode(data.sensorID, "OFF"); // Apagamos el actuador
	    			LogUtils.escribirLog("ID Nodo: " + data.sensorID + "\tALARMADO: Se reactiva el alarmado y se envía OFF para el nodo " + data.sensorID + ". Umbral mínimo alcanzado: " + thresholdLOW);
	    			data.alarmInfo += data.sensorID + " Se reactiva el alarmado y se envía OFF. Umbral mínimo alcanzado\n";
	    			// Activamos alarmado
	    			alarmActive.put(data.sensorID, true);
	    		}
	    		
	    		if(frontWS != null) frontWS.getBasicRemote().sendText(gson.toJson(data)); // Enviamos al frontal web los datos para mostrarlos
	    	}
	    } catch (Exception e) {
	    	LogUtils.escribirExcepcion("Ha ocurrido un error en el OnMessage del Websocket", e);
	    }
	}
}