#include <stdio.h>
#include <string.h>
#include <avr/pgmspace.h>
#include "i2c_callbacks.h"

static uint32_t zero_offset = 0;
static float calibration_factor = 1.0;

uint8_t scale_i2c_power_down       (uint8_t *buffer, uint8_t buffer_len) {
    printf_P(PSTR("POWER DOWN\n"));
    return 0;
}



uint8_t scale_i2c_power_up         (uint8_t *buffer, uint8_t buffer_len) {
    printf_P(PSTR("POWER UP\n"));
    return 0;
}



uint8_t scale_i2c_tare             (uint8_t *buffer, uint8_t buffer_len) {
    printf_P(PSTR("TARE\n"));
    return 0;
}



uint8_t scale_i2c_set_zero_offset  (uint8_t *buffer, uint8_t buffer_len) {
    zero_offset = ((uint32_t)buffer[0]) << 16 | ((uint32_t)buffer[1]) << 8 | ((uint32_t)buffer[0]);
    printf_P(PSTR("SET_ZERO_OFFSET to %d\n"), zero_offset);
    return 0;
}



uint8_t scale_i2c_set_calibration  (uint8_t *buffer, uint8_t buffer_len) {
    memcpy(&calibration_factor, buffer, sizeof(float));
    printf_P(PSTR("SET_CALIBRATION to %f\n"), calibration_factor);
    return 0;
}



uint8_t scale_i2c_get_zero_offset  (uint8_t *buffer, uint8_t buffer_len) {
    printf_P(PSTR("GET_ZERO_OFFSET (it is %d)\n"), zero_offset);
    buffer[0] = (zero_offset & 0x00FF0000)>>16;
    buffer[1] = (zero_offset & 0x0000FF00)>>8;
    buffer[2] = (zero_offset & 0x000000FF);
    return 3;
}



uint8_t scale_i2c_read             (uint8_t *buffer, uint8_t buffer_len) {
    printf_P(PSTR("READ (0x654321), nb_times=%d\n"), buffer[0]);
    buffer[0] = 0x65;
    buffer[1] = 0x43;
    buffer[2] = 0x21;
    return 3;
}



uint8_t scale_i2c_get_value        (uint8_t *buffer, uint8_t buffer_len) {
    float tmp = 1234.56;
    printf_P(PSTR("GET_VALUE (%f), nb_times=%d\n"), tmp, buffer[0]);
    memcpy(&tmp, buffer, sizeof(float));
    return sizeof(float);
}



uint8_t scale_i2c_get_calibration  (uint8_t *buffer, uint8_t buffer_len) {
    printf_P(PSTR("GET_CALIBRATION (it is %f)\n"), calibration_factor);
    memcpy(buffer, &calibration_factor, sizeof(float));
    return 0;
}


