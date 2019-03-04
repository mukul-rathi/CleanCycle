#include <TinyGPS++.h>
#include <SPI.h>
#include <SD.h>
#include <sensorSDS011.h>
#include <Wire.h>

#define LED PB12
#define CSPIN PA4
#define VINPIN PA0
#define RED PB1 // former LED1
#define GREEN PB0
#define BLUE PA1

// The TinyGPS++ object
TinyGPSPlus gps;
File myFile;
int filename;
const int MPU_addr = 0x68; // I2C address of the MPU-6050
double ref[7], acc[7]; //AcX,AcY,AcZ,Tmp,GyX,GyY,GyZ
int16_t AcX, AcY, AcZ, Tmp, GyX, GyY, GyZ;
uint32_t mtime;

float p10, p25, vin = 0;
bool status;
int counter = 0, i_acc = 0, n_cali = 2000;
SDS021 nova;
uint8_t fwdate[3], workmode, asleep;
int ret, gpsUpdated = 0;

void setup() {
    // set up LEDs and turn on
    pinMode(LED, OUTPUT);
    digitalWrite(LED, HIGH);
    pinMode(GREEN, OUTPUT);
    digitalWrite(GREEN, HIGH);
    pinMode(RED, OUTPUT);
    digitalWrite(RED, LOW);

    // set up the air pollution sensor
    nova.begin(&Serial1);
    nova.setDebug ( false );

    // Open serial communications and wait for port to open:
    Serial.begin(9600);
    Serial2.begin(9600); // GPS baudrate is 9600
    delay(50);

    // check if battery level is OK and output it to debug output
    if (!batteryOk()) {
        Serial.print(F("Battery voltage is low: "));
    } else {
        Serial.print(F("Battery voltage is ok: "));
    }
    Serial.println(vin);

    // initialise SD card
    Serial.println("Initialising SD card...");
    if (!SD.begin(CSPIN)) {
        Serial.println("SD initialisation failed!");
    }
    else {
        Serial.println("SD initialisation done.");
    }

    // reset reference acceleration data
    for (int i = 0; i < 7; i++) {
        ref[i] = 0.0;
    }

    // calibrate accelerometer
    Serial.println(F("Calibrating accelerometer..."));
    Wire.begin();
    Wire.beginTransmission(MPU_addr);
    Wire.write(0x6B);  // PWR_MGMT_1 register
    Wire.write(0);     // set to zero (wakes up the MPU-6050)
    Wire.endTransmission();
    averageAcc(n_cali, true); // calibrate the accelerometer
    mtime = millis();
    Serial.println(F("Calibration done."));

    /////////////// Nova init begin
    Serial.println(F("Initialising PM sensor..."));
    nova.sleepWork(&asleep, SDS021_WORKING, SDS021_SET );
    // If module is sleeping, then do not expect an answer. Better ask again after a short delay.
    delay(10);

    nova.sleepWork(&asleep, SDS021_WORKING, SDS021_ASK );
    if ( asleep != 0xFF ) {
        Serial.println("");
        Serial.print("Module set to ");
        Serial.println( asleep == SDS021_SLEEPING ? "Sleep" : "Work" );
    }

    // print air pollution sensor info to debug output
    nova.firmwareVersion( fwdate );
    if ( fwdate[0] != 0xFF ) {
        Serial.print( "fwdate = 20");
        Serial.print( fwdate[0]);
        Serial.print( " month ");
        Serial.print( fwdate[1]);
        Serial.print("day ");
        Serial.println( fwdate[2] );
    }

    // print whether there is a response from the air particle sensor or not
    if (nova.workMode( &workmode, SDS021_QUERYMODE, SDS021_SET)) {
        Serial.println("Mode set.");
    } else {
        Serial.println(F("No response from PM sensor."));
    }
    if ( workmode != 0xFF ) {
        Serial.print( "workmode set to "); Serial.println( workmode == SDS021_REPORTMODE ? "Reportmode" : "Querymode" );
    }
    /////////////// Nova init end

    // print whether GPS data communication is enabled or not
    if (Serial2.available() > 0) {
        Serial.println(F("GPS communication enabled."));
    } else {
        Serial.println(F("GPS not available."));
    }

    // open the file. note that only one file can be open at a time,
    // so you have to close this one before opening another.
    filename = getNextName();
    digitalWrite(LED, LOW);
    myFile = SD.open(String(filename) + ".csv", FILE_WRITE);

    // if the file opened okay, write to it:
    if (myFile) {
        myFile.println(F("Counter,Latitude,Longitude,PM10,PM2.5,gpsUpdated,Speed,Altitude,Satellites,Date,Time,Millis,AccX,AccY,AccZ,Temperature,GyroX,GyroY,GyroZ,nAcc,BatteryVIN"));
        myFile.print("Reference");
        myFile.print(","); myFile.print(filename);
        for (int i = 0; i < 9; i++) { // no reference GPS and PM10/2.5 at this point
            myFile.print(","); myFile.print(0);
        }
        myFile.print(","); myFile.print(mtime);
        for (int i = 0; i < 7; i++) {
            myFile.print(","); myFile.print(ref[i]);
        }
        myFile.print(","); myFile.println(n_cali);
        // close the file:
        myFile.close();
    } else {
        // if the file didn't open, print an error:
        Serial.println("Error while writing SD");
    }

    // print the data to debug output as well
    Serial.println(F("Counter,Latitude,Longitude,PM10,PM2.5,gpsUpdated,Speed,Altitude,Satellites,Date,Time,Millis,AccX,AccY,AccZ,Temperature,GyroX,GyroY,GyroZ,nAcc,BatteryVIN"));
    Serial.print("Reference");
    Serial.print(","); Serial.print(filename);
    for (int i = 0; i < 9; i++) { // no reference GPS and PM10/2.5 at this point
        Serial.print(","); Serial.print(0);
    }
    Serial.print(","); Serial.print(mtime);
    for (int i = 0; i < 7; i++) {
        Serial.print(","); Serial.print(ref[i]);
    }
    Serial.print(","); Serial.println(n_cali);
    digitalWrite(LED, HIGH);
    mtime = millis();
}

void loop() {
    // read acceleration
    readAcc();

    // wait until GPS is available
    while (Serial2.available() > 0) {
        // read & encode GPS data
        gps.encode(Serial2.read());

        // check that 3 seconds have passed, since this is our data collection timeframe
        if (millis() - mtime > 3000) {
            // turn on LED for user feedback
            digitalWrite(LED, LOW);

            // read in battery level
            batteryOk();

            // wait until there is air pollution data
            while (!nova.queryData(&p10, &p25)) {
                delay(50);
            }

            // normalise the acceleration
            finishAcc();

            if (gps.location.isUpdated()) {
                gpsUpdated = 1;
            }

            // open file to write sensor data
            myFile = SD.open(String(filename) + ".csv", FILE_WRITE);
            
            // if the file opened successfully, write the sensor data to it
            if (myFile) {
                myFile.print(counter);
                myFile.print(","); myFile.print(gps.location.lat(), 6);
                myFile.print(","); myFile.print(gps.location.lng(), 6);
                myFile.print(","); myFile.print(p10, 1 );
                myFile.print(","); myFile.print(p25, 1);
                myFile.print(","); myFile.print(gpsUpdated);
                myFile.print(","); myFile.print(gps.speed.kmph());
                myFile.print(","); myFile.print(gps.altitude.meters());
                myFile.print(","); myFile.print(gps.satellites.value());
                myFile.print(","); myFile.print(formatDate());
                myFile.print(","); myFile.print(formatTime());
                myFile.print(","); myFile.print(mtime);
                for (int i = 0; i < 7; i++) {
                    myFile.print(","); myFile.print(acc[i]);
                }
                myFile.print(","); myFile.print(i_acc);
                myFile.print(","); myFile.println(vin);
                myFile.close();
            }

            // turn off LED for user feedback
            digitalWrite(LED, HIGH);

            // print data to serial output for debugging purposes
            Serial.print(counter);
            Serial.print(","); Serial.print(gps.location.lat(), 6);
            Serial.print(","); Serial.print(gps.location.lng(), 6);
            Serial.print(","); Serial.print(p10, 1 );
            Serial.print(","); Serial.print(p25, 1);
            Serial.print(","); Serial.print(gpsUpdated);
            Serial.print(","); Serial.print(gps.speed.kmph());
            Serial.print(","); Serial.print(gps.altitude.meters());
            Serial.print(","); Serial.print(gps.satellites.value());
            Serial.print(","); Serial.print(formatDate());
            Serial.print(","); Serial.print(formatTime());
            Serial.print(","); Serial.print(mtime);
            for (int i = 0; i < 7; i++) {
                Serial.print(","); Serial.print(acc[i]);
            }
            Serial.print(","); Serial.print(i_acc);
            Serial.print(","); Serial.println(vin);

            // set current time
            mtime = millis();

            // reset accelerometer data
            resetAcc();
            
            counter++;
            gpsUpdated = 0;
        }
    }
    delay(10);
}

// read accelerometer values from accelerometer to acc[] array
void readAcc() {
    Wire.beginTransmission(MPU_addr);
    Wire.write(0x3B);  // starting with register 0x3B (ACCEL_XOUT_H)
    Wire.endTransmission();
    Wire.requestFrom(MPU_addr, 14); // request a total of 14 registers
    acc[0] += AcX = Wire.read() << 8 | Wire.read(); // 0x3B (ACCEL_XOUT_H) & 0x3C (ACCEL_XOUT_L)
    acc[1] += AcY = Wire.read() << 8 | Wire.read(); // 0x3D (ACCEL_YOUT_H) & 0x3E (ACCEL_YOUT_L)
    acc[2] += AcZ = Wire.read() << 8 | Wire.read(); // 0x3F (ACCEL_ZOUT_H) & 0x40 (ACCEL_ZOUT_L)
    acc[3] += Tmp = Wire.read() << 8 | Wire.read(); // 0x41 (TEMP_OUT_H) & 0x42 (TEMP_OUT_L)
    acc[4] += GyX = Wire.read() << 8 | Wire.read(); // 0x43 (GYRO_XOUT_H) & 0x44 (GYRO_XOUT_L)
    acc[5] += GyY = Wire.read() << 8 | Wire.read(); // 0x45 (GYRO_YOUT_H) & 0x46 (GYRO_YOUT_L)
    acc[6] += GyZ = Wire.read() << 8 | Wire.read(); // 0x47 (GYRO_ZOUT_H) & 0x48 (GYRO_ZOUT_L)
    i_acc++;
}

// this function will normalize the accelerometer data
void finishAcc() {
  for (int i = 0; i < 7; i++) 
    if (i != 3) {
      acc[i] = acc[i]/i_acc - ref[i];
    }
    else {
      acc[i] = acc[i] / i_acc / 340.00 + 36.53;
    }
}

// reset accelerometer buffer
void resetAcc() {
    i_acc = 0;
    for (int i = 0; i < 7; i++) {
        acc[i] = 0.0;
    }
}


// calculate average acceleration
// if calibrate is set to true, also sets the reference acceleration values for
// normalising data
void averageAcc(int counter, boolean calibrate) {
  digitalWrite(LED, LOW);

  // reset acceleration buffer
  resetAcc();

  // read in accelerometer data into buffer
  for (int i = 0; i < counter; i++) {
    Wire.beginTransmission(MPU_addr);
    Wire.write(0x3B);  // starting with register 0x3B (ACCEL_XOUT_H)
    Wire.endTransmission();
    Wire.requestFrom(MPU_addr, 14); // request a total of 14 registers
    acc[0]+=AcX=Wire.read()<<8|Wire.read();  // 0x3B (ACCEL_XOUT_H) & 0x3C (ACCEL_XOUT_L)    
    acc[1]+=AcY=Wire.read()<<8|Wire.read();  // 0x3D (ACCEL_YOUT_H) & 0x3E (ACCEL_YOUT_L)
    acc[2]+=AcZ=Wire.read()<<8|Wire.read();  // 0x3F (ACCEL_ZOUT_H) & 0x40 (ACCEL_ZOUT_L)
    acc[3]+=Tmp=Wire.read()<<8|Wire.read();  // 0x41 (TEMP_OUT_H) & 0x42 (TEMP_OUT_L)
    acc[4]+=GyX=Wire.read()<<8|Wire.read();  // 0x43 (GYRO_XOUT_H) & 0x44 (GYRO_XOUT_L)
    acc[5]+=GyY=Wire.read()<<8|Wire.read();  // 0x45 (GYRO_YOUT_H) & 0x46 (GYRO_YOUT_L)
    acc[6]+=GyZ=Wire.read()<<8|Wire.read();  // 0x47 (GYRO_ZOUT_H) & 0x48 (GYRO_ZOUT_L)
  }

  // normalise the accelerometer data
  for (int i = 0; i < 7; i++) {
    if (i != 3) {
      acc[i] = acc[i] / counter - ref[i];
    }
    else {
      acc[3] = acc[3] / counter / 340.00 + 36.53;
    }
  }

  // if we are trying to calibrate accelerometer, set ref values, otherwise subtract them from acceleration
  if (calibrate) {
    for (int i = 0; i<7; i++) {
      ref[i] = acc[i];
    }
  } else {
    for (int i = 0; i<7; i++) {
      if (i != 3) {
        acc[i] -= ref[i]; // subtract ref for measurements other than temp
      }
    }
  }
  
  digitalWrite(LED, HIGH);
}


// encode a 28 digit number String into a 12 byte ASCII string
// this function was used to encode more data using fewer bytes
// this function is no longer needed
String encodeMsg28(String msg) {
    if (msg.length() != 28) {
        String ret = "" + msg.length();
        return ret;
    }

    unsigned char bytes[3];
    String out = "";

    for (int j = 0; j < 4; j++)
    {
        String temp = "";
        for (int i = 0; i < 7; i++)
            temp += msg[j * 7 + i];
        unsigned long int t = temp.toInt();

        bytes[0] = (t >> 16);
        bytes[1] = (t >> 8);
        bytes[2] = t;
        //out += (unsigned char) bytes[0] + (unsigned char) bytes[1] + (unsigned char) bytes[2];
        out += (char) bytes[0];
        out += (char) bytes[1];
        out += (char) bytes[2];
    }
    return out;
}

//String trimGPS(double lati) { // returns longitude/latitude trimmed to 5 relevant digits
//  int temp = ((int)(lati*100000))%100000; // also works but might cause loss of precision: =(int)((lati-(int)lati)*100000);
//  String out = (String)temp;
//  temp = 5-out.length(); // fill up with leading zeroes if necessary
//  for (int i=0; i<temp; i++) out="0"+out;
//  return out;
//}

// format date in the following format:
// YYYYMMDD
String formatDate() {
    String out = "";
    out += gps.date.year();
    out += formatInt(gps.date.month(), 2);
    out += formatInt(gps.date.day(), 2);
    return out;
}

// format time in the following format:
// HHMMSS
String formatTime() {
    String out = "";
    out += formatInt(gps.time.hour(), 2);
    out += formatInt(gps.time.minute(), 2);
    out += formatInt(gps.time.second(), 2);
    return out;
}

// format an integer as a string with the required number of digits of precision
// if the number of digits in the integer is larger than precision, return the largest number
// possible using that precision ie. precision number of 9s
String formatInt(int num, int precision) {
  // find the maximum
  int maximum = 1;
  for (int i = 0; i < precision; i++) {
    maximum *= 10;
  }
  maximum--;

  // if the number is too big, return the maximum number that can be represented using precision digits
  if (num > maximum) {
    return String(maximum);
  }

  // otherwise convert the integer and prepend 0s until full
  String out = String(num); // fill up with leading zeroes if necessary
  String zero = "";
  for (int i = 0; i < (precision - out.length()); i++) {
    zero += "0";
  }
  
  return (zero + out);
}

//String getRandomName() { // generate a "random" filename using sensor input
//  uint32_t ran = 0;
//  for (int i = 0; i < 7; i++) ran += ref[i];
//  while (ran > 99999) ran -= ref[0];
//  String out = String(ran);
//  return out;
//}

// check filenames on SD card and increment by one
int getNextName() {
  File root = SD.open("/");
  int out = 10000;
  while (true) {
    // check file
    File entry = root.openNextFile();

    // if opening the file doesn't return a correct file, we are done
    if (!entry) {
      break;
    }

    // check if the filename is in the correct range. If it is, increment the counter
    String temp = entry.name();
    temp = temp.substring(0 ,5);
    int num = temp.toInt();
    if (num < 99999 && num >= out) {
      out = num + 1;
    }

    // close the file
    entry.close();
  }
  return out;
}


// checks if the battery voltage is OK and returns whether it is or not
boolean batteryOk() { // analog input can measure between 0 and supply voltage (3.3V)
    vin = 2 * 3.3 * analogRead(VINPIN) / 4095.0; // calculation based on 50:50 voltage divider to measure battery voltage between 0 and 6.6V
    if (vin < 3.5) {
        digitalWrite(RED, HIGH);
        return false;
    }
    else digitalWrite(RED, LOW);
    return true;
}
