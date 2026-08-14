#ifndef AVR_TIMER_H
#define AVR_TIMER_H

#include <stdint.h>

// NOTE: Timer callback should as quick as possible since executed in an ISR context with SREG IE flag cleared
typedef void (*avr_timer_callback_t)(void);

int avr_timer_is_init(void);

void avr_timer_init(uint16_t threshold_seconds,
                    avr_timer_callback_t callback);

void avr_timer_set_threshold(uint16_t threshold_seconds);

void avr_timer_deinit(void);

void avr_timer_reset_count(void);

uint16_t avr_timer_get_seconds(void);
uint16_t avr_timer_get_threshold(void);

#endif
