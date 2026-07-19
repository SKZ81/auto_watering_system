# test for i2c communication

## description


This little sketche is intended to test the i2c slave system contained in parent directory.

It is written in "Arduino API C++" in order to :
* do the test using a proven lib.
* compile/run the sketche on ESP8266/ESP32 modules/boards (or other)

## About Building

**NOTE** : this sketche has first been written for Arduino Uno board, using Arduino-Makefile to build, then I switched to platformio, to handle both Arduino & NodeMCU v0.9 (esp8266).
Original content of the directory has been moved into src/ by that time.

As for now, both building/flashing methods should work. Either :
* do a `make/make upload` in the `src` subdirectory (you will probably need to edit the path to Arduino-Makefile, fitting your own install) -- *UNO ONLY* ! --, or
* do a `platformio run && platformio run --target upload` in this directory (**NOTE** : optionnaly provide -e {uno,nodemcu} to build for a specific target. PIO calls it "environment")
