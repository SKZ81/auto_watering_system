#ifndef __I2C_CALLBACKS_H__
#define __I2C_CALLBACKS_H__

uint8_t scale_i2c_power_down       (uint8_t *buffer, uint8_t buffer_len);
uint8_t scale_i2c_power_up         (uint8_t *buffer, uint8_t buffer_len);
uint8_t scale_i2c_tare             (uint8_t *buffer, uint8_t buffer_len);
uint8_t scale_i2c_set_zero_offset  (uint8_t *buffer, uint8_t buffer_len);
uint8_t scale_i2c_set_calibration  (uint8_t *buffer, uint8_t buffer_len);
uint8_t scale_i2c_get_zero_offset  (uint8_t *buffer, uint8_t buffer_len);
uint8_t scale_i2c_read             (uint8_t *buffer, uint8_t buffer_len);
uint8_t scale_i2c_get_value        (uint8_t *buffer, uint8_t buffer_len);
uint8_t scale_i2c_get_calibration  (uint8_t *buffer, uint8_t buffer_len);

#endif
