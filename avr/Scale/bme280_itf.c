/**
 * Copyright (C) 2020 Bosch Sensortec GmbH. All rights reserved.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *
 * Adapted to avr-i2c-slave by SKZ81, 2026.09
 */

#include <stdint.h>
#include <stdlib.h>
#include <stdio.h>

#include <util/delay.h>

#include "bme280.h"
#include "common.h"


/******************************************************************************/
/*!                Static variable definition                                 */

/*! Variable that holds the I2C device address or SPI chip selection */
static uint8_t dev_addr;

/******************************************************************************/
/*!                User interface functions                                   */

#define CHECK_COMM(x) if ( !(x) ) { `\
    i2c_master_done(); \
    return BME280_E_COMM_FAIL; \
}

/*!
 * I2C read function map to avr-i2c-slave in MASTER mode
 */
BME280_INTF_RET_TYPE bme280_i2c_read(uint8_t reg_addr, uint8_t *reg_data, uint32_t length, void *intf_ptr)
{
    CHECK_COMM(i2c_master_init(dev_addr, MasterTransmit))
    CHECK_COMM(i2c_master_write(reg_addr, true))
    i2c_master_done();

    CHECK_COMM(i2c_master_init(dev_addr, MasterReceive))

    uint32_t count = 0;
    while (count < length) {
        CHECK_COMM(i2c_master_read(reg_data + count, false))
        count++;
    }
    CHECK_COMM(i2c_master_read(reg_data + count, true))
    i2c_master_done();

    return BME280_OK;
}

/*!
 * I2C read function map to avr-i2c-slave in MASTER mode
 */
BME280_INTF_RET_TYPE bme280_i2c_write(uint8_t reg_addr, const uint8_t *reg_data, uint32_t length, void *intf_ptr)
{
    CHECK_COMM(i2c_master_init(dev_addr, MasterTransmit))
    CHECK_COMM(i2c_master_write(reg_addr, true))
    uint32_t count = 0;
    while (count < length) {
        CHECK_COMM(i2c_master_write(reg_data + count, false))
        count++;
    }
    CHECK_COMM(i2c_master_write(reg_data + count, true))

    return BME280_OK;
}

/*!
 * SPI read function map to nothing
 */
BME280_INTF_RET_TYPE bme280_spi_read(uint8_t reg_addr, uint8_t *reg_data, uint32_t length, void *intf_ptr)
{
    return BME280_E_DEV_NOT_FOUND;
}

/*!
 * SPI write function map to nothing
 */
BME280_INTF_RET_TYPE bme280_spi_write(uint8_t reg_addr, const uint8_t *reg_data, uint32_t length, void *intf_ptr)
{
    return BME280_E_DEV_NOT_FOUND;
}

/*!
 * Delay function map to COINES platform
 */
void bme280_delay_us(uint32_t period, void *intf_ptr)
{
    _delay_us(period);
}

/*!
 *  @brief Prints the execution status of the APIs.
 */
void bme280_error_codes_print_result(const char api_name[], int8_t rslt)
{
    if (rslt != BME280_OK)
    {
        printf("%s\t", api_name);

        switch (rslt)
        {
            case BME280_E_NULL_PTR:
                printf("Error [%d] : Null pointer error.", rslt);
                printf(
                    "It occurs when the user tries to assign value (not address) to a pointer, which has been initialized to NULL.\r\n");
                break;

            case BME280_E_COMM_FAIL:
                printf("Error [%d] : Communication failure error.", rslt);
                printf(
                    "It occurs due to read/write operation failure and also due to power failure during communication\r\n");
                break;

            case BME280_E_DEV_NOT_FOUND:
                printf("Error [%d] : Device not found error. It occurs when the device chip id is incorrectly read\r\n",
                       rslt);
                break;

            case BME280_E_INVALID_LEN:
                printf("Error [%d] : Invalid length error. It occurs when write is done with invalid length\r\n", rslt);
                break;

            default:
                printf("Error [%d] : Unknown error code\r\n", rslt);
                break;
        }
    }
}

/*!
 *  @brief Function to select the interface between SPI and I2C.
 */
int8_t bme280_interface_selection(struct bme280_dev *dev, uint8_t intf)
{
    int8_t rslt = BME280_OK;
    struct coines_board_info board_info;

    if (dev != NULL)
    {
        /* Bus configuration : I2C */
        if (intf == BME280_I2C_INTF)
        {
            printf("I2C Interface\n");

            dev_addr = BME280_I2C_ADDR_PRIM;
            dev->read = bme280_i2c_read;
            dev->write = bme280_i2c_write;
            dev->intf = BME280_I2C_INTF;
        }
        /* Bus configuration : SPI */
        else if (intf == BME280_SPI_INTF)
        {
            printf("SPI Interface : Unavailable\n");

            dev_addr = 0xFF;
            dev->read = NULL;
            dev->write = NULL;
            rslt = BME280_E_DEV_NOT_FOUND;
        }

        // TODO : should not be statically allocated !
        /* Holds the I2C device addr or SPI chip selection */
        dev->intf_ptr = &dev_addr;

        /* Configure delay in microseconds */
        dev->delay_us = bme280_delay_us;
    }
    else
    {
        rslt = BME280_E_NULL_PTR;
    }

    return rslt;
}

