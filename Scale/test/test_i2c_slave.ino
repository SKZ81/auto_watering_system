#include <Wire.h>
#include "../scale_i2c_interface.h"
// #include <string.h>
// #include <stdlib.h>

typedef struct {
    uint8_t code;
    uint8_t arg_len;
    void (*read_callback) (uint8_t*);
//     uint8_t (*callback)(uint8_t*, uint8_t);
    uint8_t reply_len;
} command_t;


void no_read(uint8_t *buffer) {
    Serial.println("No arg needed...");
}

void read_uint8(uint8_t *buffer) {
    long l = -1;
    while (l<0 || l>255) {
        Serial.print("Enter uint8_t value ([0..255], decimal base) : ");
        l = Serial.readString().toInt();
    }
    Serial.print("Read value : ");
    Serial.println(l, DEC);
    buffer[0] = uint8_t(l&0xFF);
}


void read_uint24(uint8_t *buffer) {
    long l = -9000000;
    while (l<-8388608 || l>8388607) {
        Serial.print("Enter int24_t value ([-8388608..8388607], decimal base) : ");
        l = Serial.readString().toInt();
    }
    Serial.print("Read value : ");
    Serial.println(l, DEC);
    buffer[0] = (uint8_t)((l&0x00FF0000)>>16);
    buffer[1] = (uint8_t)((l&0x0000FF00)>>8);
    buffer[2] = (uint8_t)((l&0x000000FF));
}

void read_float(uint8_t *buffer) {
    float f = 0.0;
    Serial.print("Enter a float : ");
    f = Serial.readString().toFloat();
    Serial.print("Read value : ");
    Serial.println(f, 6);
    memcpy(buffer, &f, sizeof(float));
}

command_t commands[] = {
    {SCALE_I2C_POWER_DOWN,          0,              no_read,        0},
    {SCALE_I2C_POWER_UP,            0,              no_read,        0},
    {SCALE_I2C_TARE,                1,              read_uint8,     0},
    {SCALE_I2C_SET_ZERO_OFFSET,     3,              read_uint24,    0},
    {SCALE_I2C_SET_CALIBRATION,     sizeof(float),  read_float,     0},
    {SCALE_I2C_GET_ZERO_OFFSET,     0,              no_read,        3},
    {SCALE_I2C_READ,                1,              read_uint8,     3},
    {SCALE_I2C_GET_VALUE,           1,              read_uint8,     sizeof(float)},
    {SCALE_I2C_GET_CALIBRATION,     0,              no_read,        sizeof(float)}
};







void setup(void) {
    Wire.begin();
    Serial.begin(115200);
    Serial.setTimeout(5000);
}


void loop(void) {
    uint8_t buffer[4] = {0};

    Serial.println();
    Serial.println("______________________");
    Serial.println(" 1 - POWER_DOWN");
    Serial.println(" 2 - POWER_UP");
    Serial.println(" 3 - TARE");
    Serial.println(" 4 - SET_ZERO_OFFSET");
    Serial.println(" 5 - SET_CALIBRATION");
    Serial.println(" 6 - GET_ZERO_OFFSET");
    Serial.println(" 7 - READ");
    Serial.println(" 8 - GET_VALUE");
    Serial.println(" 9 - GET_CALIBRATION");
    Serial.print("Enter choice : ");
    while(!Serial.available()) {}
    char char_cmd = Serial.read();
    uint8_t cmd = 0xFF;

    if (char_cmd<'1' || char_cmd>'9') {
        Serial.print(">>> Unknown command ");
        Serial.println(char_cmd);
        return; // next loop()
    } else {
        Serial.println();
    }

    cmd = char_cmd - '1';

    commands[cmd].read_callback(buffer);

    Serial.print(">>> I2C communication, will send command_code: ");
    Serial.print(commands[cmd].code, DEC);
    Serial.print(", with ");
    Serial.print(commands[cmd].arg_len, DEC);
    Serial.print(" bytes of data = [ ");
    for(uint8_t i=0; i<commands[cmd].arg_len; i++) {
        Serial.print(buffer[i], HEX);
        Serial.print(" ");
    }

    Serial.println("]");
    Serial.print(">>> Reply (");
    Serial.print(commands[cmd].reply_len);
    Serial.print(" expected...)");

    Wire.beginTransmission((uint8_t)SCALE_I2C_ADDRESS);
    Wire.write((uint8_t)commands[cmd].code);
    for(uint8_t i=0; i<commands[cmd].arg_len; i++) {
        Wire.write((uint8_t)buffer[i]);
    }
    if (commands[cmd].reply_len == 0) {
        // nothing to read, free the bus
        Wire.endTransmission( true );
        Serial.println(" ...done");
    } else {
        Wire.endTransmission( false );
        Serial.print("  : [ ");
        Wire.requestFrom((uint8_t)SCALE_I2C_ADDRESS, (uint8_t)commands[cmd].reply_len);
        while(Wire.available()) {
            uint8_t result = Wire.read() ;
            Serial.print(result, HEX);
            Serial.print(" ");
        }
        Serial.println("]");
    }
}
