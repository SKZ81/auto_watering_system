#include "avr_uart.h"
#include "avr_timer.h"

#include "scale_i2c_interface.h"
#include "I2CSlave.h" // for i2c_slave_busy/ready
#include "I2CSlave_state_machine.h"
#include "i2c_callbacks.h"

#include "debug.h"

#include <avr/interrupt.h>
#include <avr/cpufunc.h>
#include <stdint.h>
#include <stdbool.h>
#include <util/delay.h>
#ifndef STUB_HX711
  #include "HX711.h"
#endif

i2c_slaveSM_command_t commands[] = {
    /* WRITE commands */
    {SCALE_I2C_POWER_DOWN,         0,             scale_i2c_power_down},
    {SCALE_I2C_POWER_UP,           0,             scale_i2c_power_up},
    {SCALE_I2C_TARE,               1,             scale_i2c_tare},
    {SCALE_I2C_SET_ZERO_OFFSET,    3,             scale_i2c_set_zero_offset},
    {SCALE_I2C_SET_CALIBRATION,    sizeof(float), scale_i2c_set_calibration},
    {SCALE_I2C_SET_ASYNC_NB_READS, 1,             scale_i2c_set_async_nb_reads},
    {SCALE_I2C_SET_ASYNC_PERIOD,   1,             scale_i2c_set_async_period},
    {SCALE_I2C_ASYNC_TARE,         1,             scale_i2c_async_tare},
    /* READ commands */
    {SCALE_I2C_GET_ZERO_OFFSET,    0,             scale_i2c_get_zero_offset},
    {SCALE_I2C_READ,               1,             scale_i2c_read},
    {SCALE_I2C_GET_VALUE,          1,             scale_i2c_get_value},
    {SCALE_I2C_GET_CALIBRATION,    0,             scale_i2c_get_calibration},
    {SCALE_I2C_GET_ASYNC_VALUE,    0,             scale_i2c_get_async_value},
};


static uint8_t i2c_buffer[SCALE_I2C_BUFFER_SIZE]={0};

// Variables for ASYNC READ
long     async_raw_value = 0;
uint8_t  async_nb_reads = SCALE_I2C_DEFAULT_NB_READS;
bool     trigger_measurement = false;
// Variables for ASYNC TARE
bool     trigger_async_tare = false;
uint8_t  async_tare_nbread = 0;


void set_trigger_mesurement() {
    // called from an ISR, so no need for cli/sei
    trigger_measurement = true;
}

void init(void) {
    avr_uart_init();
    stdout = &avr_uart_output;
    stdin  = &avr_uart_input_echo;
    i2c_slaveSM_init(SCALE_I2C_ADDRESS, SCALE_I2C_FREQUENCY,
                     commands, sizeof(commands)/sizeof(i2c_slaveSM_command_t),
                     i2c_buffer, SCALE_I2C_BUFFER_SIZE);
#ifndef STUB_HX711
    HX711_init(128);
    HX711_set_scale(SCALE_I2C_DEFAULT_CALIBRATION);
#endif
    avr_timer_init(SCALE_I2C_DEFAULT_ASYNC_PERIOD,
                   set_trigger_mesurement);
}

int main(void) {
    int16_t last_count = -1;
    init();

#ifdef STUB_HX711
    printf("i2c scale STUB (for testing i²c communication)\n");
#else
    printf("i2c scale\n");
    async_raw_value = HX711_read_average(async_nb_reads);
    printf("** initial Async value = 0x%lx\n", async_raw_value);
#endif

    while(1) {
        int16_t count = avr_timer_get_seconds();
        if (count != last_count) {
            printf("%d / %d\n", count, avr_timer_get_threshold());
            last_count = count;
        }

        if (trigger_measurement) {
            cli();
            trigger_measurement = false;
            sei();
#ifdef STUB_HX711
            async_raw_value = 0x987654;
#else
            i2c_slave_busy();
            async_raw_value = HX711_read_average(async_nb_reads);
            i2c_slave_ready();
#endif
            printf("** Async read, got value = %lx\n", async_raw_value);
            printf("Config: calibration: %f, Zero Offset: %ld (0x%lx), value=%f\n",
                   HX711_get_scale(), HX711_get_offset(), HX711_get_offset(),
                   (async_raw_value - HX711_get_offset())/HX711_get_scale());
        }

        if(trigger_async_tare) {
            printf("** Async tare\n");
            i2c_slave_busy();
            HX711_tare(async_tare_nbread ?
                          async_tare_nbread :
                          SCALE_I2C_DEFAULT_NB_READS);
            cli();
            trigger_async_tare = false;
            i2c_slave_ready();
            sei();
        }
    }
}
