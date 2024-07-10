#ifndef __AUTOWATS_FLOWCOUNTER_H_
#define __AUTOWATS_FLOWCOUNTER_H_

#include "driver/pcnt.h"
#include "freertos/FreeRTOS.h"
#include "freertos/queue.h"


/* (de)install ISR routine and background task */
void flowcounter_init();
void flowcounter_deinit();

/* If you need PCNT for other uses, please alloc() / free() them as well.
   You can use the returned unit (from PORT_0) as wish
   TODO: be more flexible & allow alloc w/ explicit unit ID + intr robustness */
pcnt_unit_t flowcounter_alloc(int gpio, unsigned int rate_uL,
                              unsigned int precision_mL);
void flowcounter_free(pcnt_unit_t id);

/* Force counter stop & reset */
void flowcounter_stop(pcnt_unit_t id);

typedef void (*flowcounter_callback_t)(int unit, int value);
/* Start counting until given limit is reach
 * (if non NULL, callback is called on limit reach) */
void flowcounter_start(pcnt_unit_t id, unsigned int limit_mL,
                       flowcounter_callback_t callback);

unsigned int flowcounter_get_volume_mL(int unit);

#endif /*__AUTOWATS_FLOWCOUNTER_H_*/
