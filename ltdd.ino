#include <WiFi.h>
#include <Firebase_ESP_Client.h>
#include "addons/TokenHelper.h"
#include "addons/RTDBHelper.h"
#include <DHT.h> 
#include <WiFiMulti.h>

WiFiMulti wifiMulti;

#define API_KEY "AIzaSyDE7Ti8zWG4KKtPjy7c3O6bTwOoOUb69_g"
#define DATABASE_URL "nhakinhthongminh-b3a94-default-rtdb.asia-southeast1.firebasedatabase.app"

// Khôi phục về chân số 4 cho ESP32-S3
#define SOIL_PIN 4 
#define LIGHT_SENSOR_PIN 5 
#define SERVO_PIN 18
#define LED_PIN 15 
#define DHTPIN 2
#define DHTTYPE DHT22 

const int freq = 50;   //50Hz cho Servo
const int resolution = 12;  //Độ phân giải 12-bit 
const int servoMin = 102; // 0 độ       
const int servoMax = 512; //180 độ      

FirebaseData fbdo;
FirebaseAuth auth;
FirebaseConfig config;
DHT dht(DHTPIN, DHTTYPE); // KHỞI TẠO ĐỐI TƯỢNG DHT

unsigned long sendDataPrevMillis = 0;
unsigned long readControlPrevMillis = 0;

int currentPumpState = -1; 
int currentLightState = -1; 
int isDark = 0; 
int lastDarkState = -1; 

// Biến lưu độ ẩm mục tiêu (0 = Chế độ tưới thủ công)
int targetMoisture = 0; 

void setup() {
  Serial.begin(115200);

  pinMode(LED_PIN, OUTPUT);
  digitalWrite(LED_PIN, LOW);
  
  pinMode(LIGHT_SENSOR_PIN, INPUT);
  ledcAttach(SERVO_PIN, freq, resolution); 
  dht.begin(); // KHỞI ĐỘNG CẢM BIẾN DHT22 
  Serial.println("--- TEST KHOI DONG ---");
  ledcWrite(SERVO_PIN, servoMax); 
  delay(1000);
  ledcWrite(SERVO_PIN, servoMin); 
  delay(500);

  // Ép chế độ trạm và xóa cache mạng
  WiFi.mode(WIFI_STA);
  WiFi.disconnect(true); 
  delay(500);

  wifiMulti.addAP("Phong 1", "11111111");                // Wi-Fi mặc định
  wifiMulti.addAP("Zone Six 107TDN", "107trandainghia");   // Thay bằng Wi-Fi chỗ mới
  wifiMulti.addAP("Hao", "11111111");            // Wi-Fi phát từ điện thoại dự phòng

  Serial.print("Dang quet va ket noi WiFi");
  int attempts = 0;
  while (wifiMulti.run() != WL_CONNECTED && attempts < 30) {
    delay(500);
    Serial.print(".");
    attempts++;
  }

  // Nếu quá 15 giây không vào được mạng, reset mạch
  if (WiFi.status() != WL_CONNECTED) {
    Serial.println("\n[LỖI] Khong tim thay mang nao! Dang reset mach...");
    delay(1000);
    ESP.restart(); 
  }
  Serial.println("\n[OK] WiFi Connected!");

  config.api_key = API_KEY;
  config.database_url = DATABASE_URL;
  
  if (Firebase.signUp(&config, &auth, "", "")) {
    Serial.println("[OK] Firebase Signed Up!");
  }

  Firebase.begin(&config, &auth);
  Firebase.reconnectWiFi(true);
}

void loop() {
  if (Firebase.ready()) {
    
    // LUỒNG 1: ĐỌC LỆNH VÀ CẬP NHẬT TRẠNG THÁI (Mỗi 0.5s)
    if (millis() - readControlPrevMillis > 500) {
      readControlPrevMillis = millis();
      isDark = digitalRead(LIGHT_SENSOR_PIN);
      
      if (lastDarkState != -1) { 
        if (isDark == HIGH && lastDarkState == LOW) {
          Firebase.RTDB.setInt(&fbdo, "/Control/Light", 1);
          Serial.println("[AUTO] Troi vua toi -> Tu dong BAT den");
        } 
        else if (isDark == LOW && lastDarkState == HIGH) {
          Firebase.RTDB.setInt(&fbdo, "/Control/Light", 0);
          Serial.println("[AUTO] Troi vua sang -> Tu dong TAT den");
        }
      }
      lastDarkState = isDark;

      if (Firebase.RTDB.getInt(&fbdo, "/Control/Light")) {
        currentLightState = fbdo.intData();
        digitalWrite(LED_PIN, (currentLightState == 1) ? HIGH : LOW);
      }

      // ĐỌC THÔNG SỐ ĐỘ ẨM LÝ TƯỞNG TỪ THƯ VIỆN CÂY TRỒNG
      if (Firebase.RTDB.getInt(&fbdo, "/Control/TargetMoisture")) {
        targetMoisture = fbdo.intData();
      }

      if (Firebase.RTDB.getInt(&fbdo, "/Control/Pump")) {
        int pumpStatus = fbdo.intData();
        if (pumpStatus != currentPumpState) {
          currentPumpState = pumpStatus;
          Serial.printf("[RX] MAY BOM: %d\n", pumpStatus);
          
          if (pumpStatus == 1) {
            ledcWrite(SERVO_PIN, servoMax); 
          } else {
            ledcWrite(SERVO_PIN, servoMin); 
          }
        }
      }
    }

    // LUỒNG 2: ĐỌC CẢM BIẾN VÀ BƠM NƯỚC TỰ ĐỘNG (Mỗi 2s)
    if (millis() - sendDataPrevMillis > 2000) {
      sendDataPrevMillis = millis();

      int soilRaw = analogRead(SOIL_PIN);
      int soilPercent = map(soilRaw, 4095, 0, 0, 100);
      soilPercent = constrain(soilPercent, 0, 100);
      Firebase.RTDB.setInt(&fbdo, "/Sensor/SoilMoisture", soilPercent);

      Firebase.RTDB.setInt(&fbdo, "/Sensor/LightStatus", isDark);

      // --- LOGIC BƠM NƯỚC TỰ ĐỘNG THEO LOẠI CÂY ---
      if (targetMoisture > 0) {
        if (soilPercent < targetMoisture) {
          // Đất khô hơn mức cây cần -> Tự bật bơm
          if (currentPumpState == 0) {
            ledcWrite(SERVO_PIN, servoMax);
            currentPumpState = 1;
            Firebase.RTDB.setInt(&fbdo, "/Control/Pump", 1); // Đồng bộ trạng thái công tắc App
            Serial.println("[AUTO] Dat kho -> BAT bom tu dong");
          }
        } 
        else if (soilPercent >= targetMoisture + 5) {
          // Đất đã đủ ẩm (có cộng thêm 5% Hysteresis chống nhiễu) -> Tự tắt bơm
          if (currentPumpState == 1) {
            ledcWrite(SERVO_PIN, servoMin);
            currentPumpState = 0;
            Firebase.RTDB.setInt(&fbdo, "/Control/Pump", 0); // Đồng bộ trạng thái công tắc App
            Serial.println("[AUTO] Dat da am -> TAT bom tu dong");
          }
        }
      }

      float h = dht.readHumidity();
      float t = dht.readTemperature(); 

      if (isnan(h) || isnan(t)) {
        Serial.println("[LỖI] Không thể đọc dữ liệu từ DHT22. Vui lòng kiểm tra lại dây DATA và trở Pull-up!");
        Serial.printf("[TX] Dat: %d%% | Sang: %s\n", soilPercent, (isDark == HIGH ? "TOI" : "SANG"));
      } else {
        int temp = round(t);
        int humid = round(h);
        Firebase.RTDB.setInt(&fbdo, "/Sensor/Temperature", temp);
        Firebase.RTDB.setInt(&fbdo, "/Sensor/AirHumidity", humid);
        
        Serial.printf("[TX] Dat: %d%% | Sang: %s | Nhiet do: %d°C | Do am KK: %d%%\n", 
                      soilPercent, (isDark == HIGH ? "TOI" : "SANG"), temp, humid);
      }
    }
  }
}