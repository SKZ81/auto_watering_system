#include "avr_uart.h"
#include "avr_timer.h"

 Arduino pin 2 -> HX711 CLK
 3 -> DOUT
 5V -> VCC
 GND -> GND
 
 Most any pin on the Arduino Uno will be compatible with DOUT/CLK.
 
 The HX711 board can be powered from 2.7V to 5V so the Arduino 5V power should be fine.
 
*/

#include "debug.h"
#include <avr/cpufunc.h>
#include <stdint.h>
#include <util/delay.h>
#ifndef STUB_HX711
  #include "HX711.h"
#endif



static uint8_t i2c_buffer[SCALE_I2C_BUFFER_SIZE]={0};


float    async_value = 0.0;
uint8_t  async_nb_reads = SCALE_I2C_DEFAULT_NB_READS;
uint8_t  trigger_measurement = 0;

void set_trigger_mesurement() {
    // called from an ISR, so no need for cli/sei
    trigger_measurement = 1;
}

void init(void) {
    avr_uart_init();
    stdout = &avr_uart_output;
    stdin  = &avr_uart_input_echo;
    i2c_scale_init();
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
#endif

    while(1) {
        int16_t count = avr_timer_get_seconds();
        if (count != last_count) {
            dbg("%d / %d\n", count, avr_timer_get_threshold());
            last_count = count;
        }

        if (trigger_measurement) {
            cli();
            trigger_measurement = 0;
            sei();
#ifdef STUB_HX711
            async_value = 9876.54;
#else
            async_value = HX711_get_mean_units(async_nb_reads);
#endif
            dbg("** Async read, got value = %f\n", async_value);
        }
    }
}
