#include <WiFi.h>
#include <Firebase_ESP_Client.h>
#include "addons/TokenHelper.h"
#include "addons/RTDBHelper.h"
#include <DHT.h> 
#include <WiFiMulti.h>
#include <BLEDevice.h>
#include <BLEServer.h>
#include <BLEUtils.h>
#include <BLE2902.h>

WiFiMulti wifiMulti;

#define API_KEY "AIzaSyDE7Ti8zWG4KKtPjy7c3O6bTwOoOUb69_g"
#define DATABASE_URL "nhakinhthongminh-b3a94-default-rtdb.asia-southeast1.firebasedatabase.app"

#define SOIL_PIN 4 
#define LIGHT_SENSOR_PIN 5 
#define SERVO_PIN 18
#define LED_PIN 15 
#define DHTPIN 2
#define DHTTYPE DHT22 

const int freq = 50;   
const int resolution = 12;  
const int servoMin = 102;       
const int servoMax = 512;       

FirebaseData fbdo;
FirebaseAuth auth;
FirebaseConfig config;
DHT dht(DHTPIN, DHTTYPE); 

unsigned long sendDataPrevMillis = 0;
unsigned long readControlPrevMillis = 0;
unsigned long wifiReconnectTimer = 0; 

int currentPumpState = -1; 
int currentLightState = -1; 
int isDark = 0; 
int lastDarkState = -1; 
int targetMoisture = 0; 

//khai bao bien ble
BLEServer *pServer = NULL;
BLECharacteristic * pTxCharacteristic;
bool deviceConnected = false;
bool oldDeviceConnected = false;
bool isOfflineHardwareMode = false; //cong tac cach ly phan cung

//BIẾN ĐỆM AN TOÀN CHO BLE (Chống lỗi Watchdog Core Dump khi đổi mode)
int newBlePump = -1;
int newBleLight = -1;
int newBleMoist = -1;

//UUID
#define SERVICE_UUID           "6E400001-B5A3-F393-E0A9-E50E24DCCA9E"
#define CHARACTERISTIC_UUID_RX "6E400002-B5A3-F393-E0A9-E50E24DCCA9E"
#define CHARACTERISTIC_UUID_TX "6E400003-B5A3-F393-E0A9-E50E24DCCA9E"

//connect ble
class MyServerCallbacks: public BLEServerCallbacks {
    void onConnect(BLEServer* pServer) {
      deviceConnected = true;
      Serial.println("\n[BLE] Da ket noi voi Dien thoai!");
    };
    void onDisconnect(BLEServer* pServer) {
      deviceConnected = false;
      Serial.println("\n[BLE] Dien thoai da ngat ket noi!");
    }
};

//nhan lenh tu app qua ble
class MyCallbacks: public BLECharacteristicCallbacks {
    void onWrite(BLECharacteristic *pCharacteristic) {
      String rxValue = pCharacteristic->getValue().c_str();
      if (rxValue.length() > 0) {
        Serial.print("[BLE RX] Lenh nhan duoc: ");
        Serial.println(rxValue);

        if (rxValue.indexOf("P1") != -1) newBlePump = 1;
        else if (rxValue.indexOf("P0") != -1) newBlePump = 0;
        else if (rxValue.indexOf("L1") != -1) newBleLight = 1;
        else if (rxValue.indexOf("L0") != -1) newBleLight = 0;
        else if (rxValue.startsWith("M:")) newBleMoist = rxValue.substring(2).toInt();
      }
    }
};

void setup() {
  Serial.begin(115200);

  pinMode(LED_PIN, OUTPUT);
  digitalWrite(LED_PIN, LOW);
  
  pinMode(LIGHT_SENSOR_PIN, INPUT);
  ledcAttach(SERVO_PIN, freq, resolution); 
  dht.begin(); 
  Serial.println("--- TEST KHOI DONG ---");
  ledcWrite(SERVO_PIN, servoMax); 
  delay(1000);
  ledcWrite(SERVO_PIN, servoMin); 
  delay(500);

  //KHOI TAO BLE GATT SERVER
  BLEDevice::init("NhaKinh_ESP32S3");
  pServer = BLEDevice::createServer();
  pServer->setCallbacks(new MyServerCallbacks());
  BLEService *pService = pServer->createService(SERVICE_UUID);
  
  pTxCharacteristic = pService->createCharacteristic(CHARACTERISTIC_UUID_TX, BLECharacteristic::PROPERTY_NOTIFY);
  pTxCharacteristic->addDescriptor(new BLE2902());
  
  BLECharacteristic * pRxCharacteristic = pService->createCharacteristic(CHARACTERISTIC_UUID_RX, BLECharacteristic::PROPERTY_WRITE);
  pRxCharacteristic->setCallbacks(new MyCallbacks());
  
  pService->start();
  pServer->getAdvertising()->start();
  Serial.println("--- BLE Da san sang. Doi ket noi tu App... ---");

  //khoi tao wifi
  WiFi.mode(WIFI_STA);
  WiFi.disconnect(true); 
  delay(500);

  wifiMulti.addAP("Phong 1", "11111111");                
  wifiMulti.addAP("Zone Six 107TDN", "107trandainghia");   
  wifiMulti.addAP("Hao", "11111111");            

  Serial.print("Dang quet va ket noi WiFi");
  int attempts = 0;
  while (wifiMulti.run() != WL_CONNECTED && attempts < 15) {
    delay(500);
    Serial.print(".");
    attempts++;
  }

  if (WiFi.status() != WL_CONNECTED) {
    Serial.println("\n[CANH BAO] Khong tim thay mang! Chuyen sang che do OFFLINE 100%");
  } else {
    Serial.println("\n[OK] WiFi Connected!");
    config.api_key = API_KEY;
    config.database_url = DATABASE_URL;
    
    if (Firebase.signUp(&config, &auth, "", "")) {
      Serial.println("[OK] Firebase Signed Up!");
    }
    Firebase.begin(&config, &auth);
    Firebase.reconnectWiFi(true);
  }
}

void loop() {
  if (deviceConnected && !isOfflineHardwareMode) {
      //App vừa kết nối Bluetooth (Bất kể mở mới hay quay lại từ out app) -> Chuyển sang Offline
      isOfflineHardwareMode = true;
      Serial.println("\n[CHUYEN MACH] App da vao mode Offline. NGAT KET NOI WI-FI!");
      WiFi.disconnect(true); 
      WiFi.mode(WIFI_OFF);
  }
  
  if (!deviceConnected && isOfflineHardwareMode) {
      //app vừa ngắt Bluetooth (Khi người dùng chủ động chọn lại Wi-Fi) -> Quay lại Online
      isOfflineHardwareMode = false;
      Serial.println("\n[CHUYEN MACH] App da ngat BLE. KHOI DONG LAI WI-FI!");
      WiFi.mode(WIFI_STA); // Kích hoạt lại trạm thu phát Wi-Fi
      WiFi.disconnect(false); 
  }

  //WI-FI AN TOÀN (Nhịp 4 giây/lần chống treo card mạng khi chuyển chế độ) 
  if (!isOfflineHardwareMode && WiFi.status() != WL_CONNECTED) {
      if (millis() - wifiReconnectTimer > 4000) {
          wifiReconnectTimer = millis();
          Serial.println("[WIFI] Dang quet va ket noi lai...");
          wifiMulti.run();
          if (WiFi.status() == WL_CONNECTED) {
              Serial.println("[WIFI] Kết nối mạng thành công!");
          }
      }
  }

  //Tự động phát lại sóng BLE để chờ App kết nối lại khi quay về từ nền
  if (!deviceConnected && oldDeviceConnected) {
      delay(500); 
      pServer->startAdvertising(); 
      Serial.println("[BLE] Dang phat lai song...");
      oldDeviceConnected = deviceConnected;
  }
  if (deviceConnected && !oldDeviceConnected) {
      oldDeviceConnected = deviceConnected;
  }

  //THỰC THI LỆNH BLE TRONG VÒNG LẶP CHÍNH AN TOÀN
  if (newBlePump != -1) {
      ledcWrite(SERVO_PIN, newBlePump == 1 ? servoMax : servoMin);
      currentPumpState = newBlePump;
      newBlePump = -1; // Xóa cờ đệm
  }
  if (newBleLight != -1) {
      digitalWrite(LED_PIN, newBleLight == 1 ? HIGH : LOW);
      currentLightState = newBleLight;
      newBleLight = -1; // Xóa cờ đệm
  }
  if (newBleMoist != -1) {
      targetMoisture = newBleMoist;
      newBleMoist = -1; // Xóa cờ đệm
  }

  //cbas
  if (millis() - readControlPrevMillis > 500) {
    readControlPrevMillis = millis();
    
    isDark = digitalRead(LIGHT_SENSOR_PIN);
    if (lastDarkState != -1) { 
      if (isDark == HIGH && lastDarkState == LOW) {
        digitalWrite(LED_PIN, HIGH);
        currentLightState = 1;
        Serial.println("[AUTO] Troi vua toi -> Tu dong BAT den");
        if (!isOfflineHardwareMode && WiFi.status() == WL_CONNECTED && Firebase.ready()) Firebase.RTDB.setInt(&fbdo, "/Control/Light", 1);
      } 
      else if (isDark == LOW && lastDarkState == HIGH) {
        digitalWrite(LED_PIN, LOW);
        currentLightState = 0;
        Serial.println("[AUTO] Troi vua sang -> Tu dong TAT den");
        if (!isOfflineHardwareMode && WiFi.status() == WL_CONNECTED && Firebase.ready()) Firebase.RTDB.setInt(&fbdo, "/Control/Light", 0);
      }
    }
    lastDarkState = isDark;

    //chi doc lenh khi co wifi
    if (!isOfflineHardwareMode && WiFi.status() == WL_CONNECTED && Firebase.ready()) {
      if (Firebase.RTDB.getInt(&fbdo, "/Control/Light")) {
        currentLightState = fbdo.intData();
        digitalWrite(LED_PIN, (currentLightState == 1) ? HIGH : LOW);
      }

      if (Firebase.RTDB.getInt(&fbdo, "/Control/TargetMoisture")) {
        targetMoisture = fbdo.intData();
      }

      if (Firebase.RTDB.getInt(&fbdo, "/Control/Pump")) {
        int pumpStatus = fbdo.intData();
        if (pumpStatus != currentPumpState) {
          currentPumpState = pumpStatus;
          Serial.printf("[RX FIREBASE] MAY BOM: %d\n", pumpStatus);
          if (pumpStatus == 1) ledcWrite(SERVO_PIN, servoMax); 
          else ledcWrite(SERVO_PIN, servoMin); 
        }
      }
    }
  }

  //doc cam bien
  if (millis() - sendDataPrevMillis > 2000) {
    sendDataPrevMillis = millis();

    int soilRaw = analogRead(SOIL_PIN);
    int soilPercent = map(soilRaw, 4095, 0, 0, 100);
    soilPercent = constrain(soilPercent, 0, 100);

    //auto bom nuoc
    if (targetMoisture > 0) {
      if (soilPercent < targetMoisture) {
        if (currentPumpState == 0) {
          ledcWrite(SERVO_PIN, servoMax);
          currentPumpState = 1;
          Serial.println("[AUTO] Dat kho -> BAT bom tu dong");
          if (!isOfflineHardwareMode && WiFi.status() == WL_CONNECTED && Firebase.ready()) Firebase.RTDB.setInt(&fbdo, "/Control/Pump", 1); 
        }
      } 
      else if (soilPercent >= targetMoisture + 5) {
        if (currentPumpState == 1) {
          ledcWrite(SERVO_PIN, servoMin);
          currentPumpState = 0;
          Serial.println("[AUTO] Dat da am -> TAT bom tu dong");
          if (!isOfflineHardwareMode && WiFi.status() == WL_CONNECTED && Firebase.ready()) Firebase.RTDB.setInt(&fbdo, "/Control/Pump", 0);
        }
      }
    }

    float h = dht.readHumidity();
    float t = dht.readTemperature(); 

    if (isnan(h) || isnan(t)) {
      Serial.println("[LỖI] Không thể đọc dữ liệu từ DHT22.");
    } else {
      int temp = round(t);
      int humid = round(h);
      
      //xuat du lieu doc lap
      if (isOfflineHardwareMode) {
        //bluetooth
        char txString[25];
        sprintf(txString, "T%d|H%d|S%d|L%d|P%d", temp, humid, soilPercent, isDark, currentPumpState);
        pTxCharacteristic->setValue(txString);
        pTxCharacteristic->notify();
        Serial.println("[TX BLE] Da gui thong so qua Bluetooth");
        
      } else {
        //wifi
        if (WiFi.status() == WL_CONNECTED && Firebase.ready()) {
          Firebase.RTDB.setInt(&fbdo, "/Sensor/SoilMoisture", soilPercent);
          Firebase.RTDB.setInt(&fbdo, "/Sensor/LightStatus", isDark);
          Firebase.RTDB.setInt(&fbdo, "/Sensor/Temperature", temp);
          Firebase.RTDB.setInt(&fbdo, "/Sensor/AirHumidity", humid);
          Serial.println("[TX FIREBASE] Da dong bo len Dam may");
        }
      }
      
      Serial.printf("[INFO] Dat: %d%% | Sang: %s | Nhiet: %d°C | Am KK: %d%%\n", 
                    soilPercent, (isDark == HIGH ? "TOI" : "SANG"), temp, humid);
    }
  }
}