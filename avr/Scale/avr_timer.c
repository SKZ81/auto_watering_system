#include "avr_timer.h"

#include <avr/io.h>
#include <avr/interrupt.h>

static volatile uint16_t elapsed_seconds = 0;
static volatile uint16_t threshold = 0;

static avr_timer_callback_t timer_callback = 0;
static int avr_timer_running = 0;

int avr_timer_is_init() {
    return avr_timer_running;
}

void avr_timer_init(uint16_t threshold_seconds,
                    avr_timer_callback_t callback)
{
    elapsed_seconds = 0;
    threshold = threshold_seconds;
    timer_callback = callback;

    TCCR1A = 0;
    TCCR1B = 0;
    TCNT1 = 0;

    /*
     * 16 MHz
     * prescaler 1024
     *
     * 16000000 / 1024 = 15625 Hz
     *
     * Compare every second.
     */

    OCR1A = F_CPU / 1024;
    TCCR1B |= (1 << WGM12);
    TCCR1B |= (1 << CS12) | (1 << CS10);
    TIMSK1 |= (1 << OCIE1A);

    avr_timer_running = 1;
}

void avr_timer_set_threshold(uint16_t threshold_seconds)
{
    uint8_t s = SREG;
    cli();
    threshold = threshold_seconds;
    SREG =  s;
}

void avr_timer_deinit(void)
{
    TIMSK1 &= ~(1 << OCIE1A);

    TCCR1A = 0;
    TCCR1B = 0;

    avr_timer_running = 0;
}

void avr_timer_reset_count(void)
{
    uint8_t s = SREG;
    cli();
    elapsed_seconds = 0;
    SREG = s;
}

uint16_t avr_timer_get_seconds(void)
{
    uint16_t value;
    uint8_t s = SREG;

    cli();
    value = elapsed_seconds;
    SREG = s;

    return value;
}

uint16_t avr_timer_get_threshold(void)
{
    uint16_t value;
    uint8_t s = SREG;

    cli();
    value = threshold;
    SREG = s;

    return value;
}

ISR(TIMER1_COMPA_vect)
{
    elapsed_seconds++;

    if (timer_callback &&
        threshold &&
        elapsed_seconds >= threshold)
    {
        elapsed_seconds = 0;
        timer_callback();
    }
}
