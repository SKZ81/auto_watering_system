#include <stdio.h>
#include <string.h>
#include <avr/pgmspace.h>
#include "i2c_callbacks.h"
#ifndef STUB_HX711
  #include "HX711.h"
#endif

// static int32_t zero_offset = 0;
// static float calibration_factor = 1.0;
#ifndef DEBUG
#define DEBUG 1
#endif

#if DEBUG
    #define dbg(x, ...) printf_P(PSTR(x) ,##__VA_ARGS__)
#else
    #define dbg(x, ...)
#endif

#ifdef STUB_HX711
// when STUB, those variable are to "emulate" HX711, which stores those values internally
static int32_t zero_offset = 0;
static float calibration_factor = 1.0;
#endif



uint8_t scale_i2c_power_down       (uint8_t *buffer, uint8_t buffer_len) {
    dbg("POWER DOWN\n");
#ifndef STUB_HX711
    HX711_power_up();
#endif
    return 0;
}



uint8_t scale_i2c_power_up         (uint8_t *buffer, uint8_t buffer_len) {
    dbg("POWER UP\n");
#ifndef STUB_HX711
    HX711_power_down();
#endif
    return 0;
}



uint8_t scale_i2c_tare             (uint8_t *buffer, uint8_t buffer_len) {
    dbg("TARE, nb_times=%d\n", buffer[0]);
#ifndef STUB_HX711
    HX711_tare(buffer[0]);
#endif
    return 0;
}



uint8_t scale_i2c_set_zero_offset  (uint8_t *buffer, uint8_t buffer_len) {
    long offset = ((int32_t)buffer[0]) << 16 | ((int32_t)buffer[1]) << 8 | ((int32_t)buffer[2]);
    dbg("SET_ZERO_OFFSET to %ld\n", offset);
#ifndef STUB_HX711
    HX711_set_offset(offset);
#else
    zero_offset = offset;
#endif
    return 0;
}



uint8_t scale_i2c_set_calibration  (uint8_t *buffer, uint8_t buffer_len) {
    float calib = 0.0;
    memcpy(&calib, buffer, sizeof(float));
    dbg("SET_CALIBRATION to %f\n", calib);
#ifndef STUB_HX711
    HX711_set_scale(calib);
#else
    calibration_factor = calib;
#endif
    return 0;
}



uint8_t scale_i2c_get_zero_offset  (uint8_t *buffer, uint8_t buffer_len) {
    long offset
#ifndef STUB_HX711
                = HX711_get_offset();
#else
                = zero_offset;
#endif
    dbg("GET_ZERO_OFFSET (it is %ld)\n", offset);
    buffer[0] = (offset & 0x00FF0000)>>16;
    buffer[1] = (offset & 0x0000FF00)>>8;
    buffer[2] = (offset & 0x000000FF);
    return 3;
}



uint8_t scale_i2c_read             (uint8_t *buffer, uint8_t buffer_len) {
    long raw_value
#ifndef STUB_HX711
                    = HX711_read_average(buffer[0]);
#else
                    = 0x00654321;
#endif
    dbg("READ (%d), nb_times=%d\n", raw_value, buffer[0]);
    buffer[0] = (raw_value & 0x00FF0000)>>16;
    buffer[1] = (raw_value & 0x0000FF00)>>8;
    buffer[2] = (raw_value & 0x000000FF);

    return 3;
}



uint8_t scale_i2c_get_value        (uint8_t *buffer, uint8_t buffer_len) {
    float value
#ifndef STUB_HX711
                = HX711_get_mean_units(buffer[0]);
#else
                = 1234.56;
#endif
    dbg("GET_VALUE (%f), nb_times=%d\n", value, buffer[0]);
    memcpy(buffer, &value, sizeof(float));
    return sizeof(float);
}



uint8_t scale_i2c_get_calibration  (uint8_t *buffer, uint8_t buffer_len) {
    float calib
#ifndef STUB_HX711
                = HX711_get_scale();
#else
                = calibration_factor;
#endif
    dbg("GET_CALIBRATION (it is %f)\n", calib);
    memcpy(buffer, &calib, sizeof(float));
    return sizeof(float);
}


