#include "DHT.h"

#include <ArduinoJson.h>

#include "WebSockets.h"
#include "WebSocketsClient.h"
#include "WebSocketsServer.h"

#include <ESP8266WiFi.h>
#include <ESP8266WiFiMulti.h>

const size_t bufferSize = JSON_ARRAY_SIZE(2) + JSON_OBJECT_SIZE(3);

const char* ssid     = "vodafoneF7B4";
const char* password = "5HJPMJTBJ3Q5UX";
const char* host = "192.168.0.154";

#define USE_SERIAL Serial
#define DHTPIN D0
#define LEDPIN D8

StaticJsonBuffer<300> jb;
StaticJsonBuffer<200> jb_event;

WebSocketsClient webSocket;
ESP8266WiFiMulti wifiMulti;

String output;
int temp, hum, pot;

DHT dht(DHTPIN, DHT11);
  
void webSocketEvent(WStype_t type, uint8_t * payload, size_t length) {

  switch(type) {
    case WStype_DISCONNECTED:
      Serial.printf("[WSc] Disconnected!\n");
      
      break;
    case WStype_CONNECTED: 
      Serial.printf("[WSc] Connected to url: %s\n", payload);
      
      break;
    case WStype_TEXT:
      Serial.printf("[WSc] get text: %s\n", payload);
      
      // Parseando json para encender el LED
      JsonObject& root_ev = jsonBuffer.parseObject(payload);
      if (!root_ev.success()) {
          Serial.println("parseObject() failed");
          break;
      }
      String value = root["value"];
      if (value.equals("ON")) digitalWrite(LEDPIN, HIGH);
      else if(value.equals("OFF")) digitalWrite(LEDPIN, LOW);


      break;
    case WStype_BIN:
      Serial.printf("[WSc] get binary length: %u\n", length);
      hexdump(payload, length);

      break;
  }

}

void setup() {
  
  Serial.begin(9600);
  Serial.setDebugOutput(true);

  Serial.println();
  Serial.println();
  Serial.println();

  // Pin del led
  pinMode(LEDPIN, OUTPUT);

  wifiMulti.addAP(ssid, password);
  
  for(uint8_t t = 4; t > 0; t--) {
    Serial.printf("[SETUP] BOOT WAIT %d...\n", t);
    Serial.flush();
    delay(1000);
  }
  
  Serial.println("Connecting Wifi...");
  while(wifiMulti.run() != WL_CONNECTED) {
    Serial.println("WiFi not connected! Retry");
    delay(1000);
  }

  Serial.println("");
  Serial.println("WiFi connected");
  Serial.println("IP address: ");
  Serial.println(WiFi.localIP());

  // server address, port and URL
  webSocket.begin(host, 8080, "/AlarmadoServer/actions");

  // event handler
  webSocket.onEvent(webSocketEvent);

  // try ever 5000 again if connection has failed
  webSocket.setReconnectInterval(5000);

  dht.begin();
  
}

void loop() {
  webSocket.loop();

  JsonObject& root = jb.createObject();
  
  root["sensorID"] = "ID_" + (String) WiFi.localIP();
  root["date"] = ""; // El servidor se encargará de introducir la hora

  JsonArray& sensorNames = root.createNestedArray("sensorNames");
  JsonArray& values = root.createNestedArray("values");
  
  temp = dht.readTemperature();
  hum = dht.readHumidity();
  pot = analogRead(A0);
  
  sensorNames.add("Temperatura (°C)");
  values.add(temp);
  
  Serial.print("Temperature: ");
  Serial.print(temp);
  Serial.println(" *C");
  
  sensorNames.add("Humedad (%)");
  values.add(hum);
  
  Serial.print("Humedad: ");
  Serial.print(hum);
  Serial.println(" %");
  
  sensorNames.add("Potenciometro");
  values.add(pot);

  Serial.print("Potenciometro: ");
  Serial.print(pot);

  // Enviamos el JSON con los sensores
  root.printTo(output);
  webSocket.sendTXT(output);
  output = ""; // Limpiamos String 
  jb.clear();

  delay(2000);
}
