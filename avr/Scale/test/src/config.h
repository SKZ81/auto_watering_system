#ifndef __TEST_I2C_SCALE_CONFIG_H__
#define __TEST_I2C_SCALE_CONFIG_H__

#if defined(ARDUINO_ESP8266_NODEMCU)
  #define PIN_I2C_SDA D1
  #define PIN_I2C_SCL D2
#elif defined(ARDUINO_AVR_UNO)
  #define PIN_I2C_SDA A4
  #define PIN_I2C_SCL A5
#else // not building using platformio, so UNO/Nano target
  #define PIN_I2C_SDA A4
  #define PIN_I2C_SCL A5
#endif




#endif
