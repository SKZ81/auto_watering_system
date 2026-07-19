#ifndef __SCALE_I2C_H__
#define __SCALE_I2C_H__

#include "I2CSlave.h"

void i2c_scale_init(); // start listening i²c bus as slave (i²c address defined in i2c_slave_interface.h)

void i2c_scale_requested(request_t req_type); //

void i2c_scale_receive(uint8_t data);

#endif
