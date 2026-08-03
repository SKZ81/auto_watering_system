#ifndef __TEST_I2C_SCALE_CONFIG_H__
#define __TEST_I2C_SCALE_CONFIG_H__

#if defined(ARDUINO_ARCH_ESP8266)
  #define PIN_I2C_SDA D1
  #define PIN_I2C_SCL D2
  #define I2C_PINS D1,D2
#elif defined(ARDUINO_ARCH_ESP32)
  #define PIN_I2C_SDA 18
  #define PIN_I2C_SCL 19
  #define I2C_PINS 18,19
#elif defined(ARDUINO_ARCH_AVR)
  #define PIN_I2C_SDA A4
  #define PIN_I2C_SCL A5
  #define I2C_PINS
#else
  #error "Hardware platform architecture unspecified or not supported. Please use compile flag -DARDUINO_ARCH_(ESP8266|ESP32|AVR)"
#endif




#endif
