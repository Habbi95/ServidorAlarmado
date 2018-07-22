package main;

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

import logUtils.LogUtils;

@ServerEndpoint("/actions")
public class SimpleWebsocketServlet {
	
	private static Map<String, Session> nodesID;
	private static Session frontWS;
	private static Gson gson;
	
	private static int thresholdHIGH = -10;
	private static int thresholdLOW = -10;
	
	public SimpleWebsocketServlet() {
		nodesID = Collections.synchronizedMap(new HashMap<String, Session>());
		gson = new Gson();
	}
	
	@OnOpen
	public void open(Session session) {
		System.out.println("Conexión abierta con id " + session.getId());
		
	}

	@OnClose
	public void close(Session session) {
		for (Entry<String, Session> e : nodesID.entrySet()) 
		    if(e.getValue() == session) nodesID.remove(e.getKey()); // Eliminamos el key a partir del value.
		
		System.out.println("Conexión cerrada con id " + session.getId());
	}

	@OnError
	public void onError(Throwable error) {
		System.out.println(error.getMessage());
	}

	@OnMessage
	public void handleMessage(String message, Session session) {
	    try {
	    	if(message.equals("FRONT")) frontWS = session;
	    	else if(session == frontWS) {
	    		// Si nos llega un mensaje del front, asumimos que es por el botón. 
	    		// De este modo, parseamos el json y enviamos mensaje al nodo correspondiente via Websocket.
	    		
	    		AlarmNotificationJSON data = gson.fromJson(message, AlarmNotificationJSON.class);
	    		if(!data.sensorID.equals("")) {
	    			// Si viene informado, es un mensaje al nodo. Reenviamos el mensaje.
	    			 nodesID.get(data.sensorID).getBasicRemote().sendText(gson.toJson(data));
	    			
	    		} else {
	    			// Si no viene informado es una actualización de los umbrales. O no se ha seleccionado nodo
	    			thresholdHIGH = data.thresholdHIGH;
	    			thresholdLOW = data.thresholdLOW;
	    		}
	    		
	    	} else {
	    		SensorDataJSON data = gson.fromJson(message, SensorDataJSON.class);
	    		
	    		nodesID.put(data.sensorID, session); // Asociamos la sesion al ID del nodo
	    		Date ahora = new Date();
	    		SimpleDateFormat formateador = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
	    		data.date = formateador.format(ahora);
	    		
	    		System.out.println("Mensaje recibido (id: " + session.getId() + "): " + message);
	    		
	    		for (int i = 0; i < data.sensorNames.length; i++) {
	    			LogUtils.escribirLog("ID Nodo: " + data.sensorID + "\tValor bruto recogido del sensor " + data.sensorNames[i] + ": " + data.values[i]);
	    			LogUtils.escribirCSV(data.sensorID, data.sensorNames[i], String.valueOf(data.values[i]));
	    		}
	    		
	    		if(frontWS != null) frontWS.getBasicRemote().sendText(gson.toJson(data)); // Enviamos al frontal web los datos para mostrarlos
	    		
	    		// PROCESADO DE LOS DATOS -- Estableceremos una serie de reglas con umbrales para enviar mensajes automáticamente a los nodos.
	    		// Sólamente para datos de temperatura
	    	}
	    } catch (Exception e) {
	    	LogUtils.escribirExcepcion("Ha ocurrido un error en el OnMessage del Websocket", e);
	    }
	}
}