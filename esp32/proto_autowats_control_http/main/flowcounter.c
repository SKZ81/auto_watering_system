
// #include "freertos/portmacro.h"
// #include "freertos/queue.h"
// #include "driver/periph_ctrl.h"
#include <alloca.h>
// #include "esp_attr.h"
#include "esp_log.h"
#include <soc/pcnt_reg.h>
#include <soc/pcnt_struct.h>

#include "flowcounter.h"

static pcnt_isr_handle_t user_isr_handle = NULL; //user's ISR service handle

typedef struct {
    int unit;  // the PCNT unit that originated an interrupt
    uint32_t status; // information on the event type that caused the interrupt
} pcnt_evt_t;

static xQueueHandle event_queue;

typedef struct {
    int gpio_num;
    unsigned int flow_per_pulse_uL;
    unsigned int resolution_mL;
    unsigned int limit_mL;
    unsigned int volume_mL;
    flowcounter_callback_t callback;
} flowcounter_handle_t;

static flowcounter_handle_t* allocated[PCNT_UNIT_MAX];

/* Decode what PCNT's unit originated an interrupt
 * and pass this information together with the event type
 * the main program using a queue.
 */
static void IRAM_ATTR flowcounter_intr_handler(void *arg)
{
    pcnt_evt_t evt;

    // Iterate over all PCNT units
    for (int i = 0; i < PCNT_UNIT_MAX; i++) {
        // Get interrupt status for the PCNT unit
        uint32_t intr_status = PCNT.int_st.val;
        if (intr_status & (BIT(i))) {
            uint32_t status = PCNT.status_unit[i].val;
            PCNT.int_clr.val = BIT(i);  // Clear the interrupt

            if (status & PCNT_EVT_H_LIM) {
                // Populate event information
                evt.unit = i;
                evt.status = status;

                // Send event to the queue
                xQueueSendFromISR(event_queue, &evt, NULL);
            }
        }
    }
}

pcnt_unit_t flowcounter_alloc(int gpio, unsigned int rate_uL, unsigned int resolution) {
    int id = 0;
    while(id < PCNT_UNIT_MAX && allocated[id]) id++;
    if (id == PCNT_UNIT_MAX)
        return -1;

    allocated[id] = malloc(sizeof(flowcounter_handle_t));
    allocated[id]->gpio_num = gpio;
    allocated[id]->flow_per_pulse_uL = rate_uL;
    allocated[id]->resolution_mL = resolution;

    /* Prepare configuration for the PCNT unit */
    pcnt_config_t pcnt_config = {
        // Set PCNT input signal and control GPIOs
        .pulse_gpio_num = gpio,
        .ctrl_gpio_num = PCNT_PIN_NOT_USED,
        .channel = PCNT_CHANNEL_0,
        .unit = id,
        // Use both edges to increase precision
        .pos_mode = PCNT_COUNT_INC,
        .neg_mode = PCNT_COUNT_INC,
        .counter_l_lim = 0,
        .counter_h_lim = (2000 * resolution) / rate_uL
    };
    /* Initialize PCNT unit */
    pcnt_unit_config(&pcnt_config);

    /* Configure and enable the filter with nearly max value */
    pcnt_set_filter_value(id, 1000);
    pcnt_filter_enable(id);

    /* Disable all events but H_LIM */
    pcnt_event_disable(id, PCNT_EVT_THRES_0);
    pcnt_event_disable(id, PCNT_EVT_THRES_1);
    pcnt_event_disable(id, PCNT_EVT_ZERO);
    pcnt_event_disable(id, PCNT_EVT_L_LIM);
    pcnt_event_enable(id, PCNT_EVT_H_LIM);

    /* Initialize PCNT's counter */
    pcnt_counter_pause(id);
    pcnt_counter_clear(id);

    pcnt_intr_enable(id);

    return id;
}
void flowcounter_free(pcnt_unit_t id) {
    if (!allocated[id]) return;
    pcnt_counter_pause(id);
    pcnt_counter_clear(id);
    pcnt_intr_disable(id);
    free(allocated[id]);
}

void flowcounter_stop(pcnt_unit_t id) {
    if (!allocated[id]) return;
    pcnt_counter_pause(id);
    pcnt_counter_clear(id);
}

void flowcounter_start(pcnt_unit_t id, unsigned int limit_mL,
                       flowcounter_callback_t callback) {
    if (!allocated[id]) return;
    allocated[id]->limit_mL = limit_mL;
    allocated[id]->callback = callback;
    allocated[id]->volume_mL = 0;
    pcnt_counter_clear(id);
    pcnt_counter_resume(id);
}

unsigned int flowcounter_get_volume_mL(int id) {
    if (allocated[id])
        return allocated[id]->volume_mL;
    return -1;
}

static void pcnt_event_handler_task(void *arg) {
    pcnt_evt_t evt;
    while (1) {
        // Wait for an event in the queue
        if (xQueueReceive(event_queue, &evt, portMAX_DELAY)) {
            // Check if it's a high limit event
            if (evt.status & PCNT_EVT_H_LIM) {
                flowcounter_handle_t *handle = allocated[evt.unit];
                if (handle) {
                    // Increase volume by resolution
                    handle->volume_mL += handle->resolution_mL;
                    ESP_LOGI("FLOWCNT", "Unit: %d volume: %d mL", evt.unit, handle->volume_mL);

                    // Check if volume exceeds or equals the limit
                    if (handle->volume_mL >= handle->limit_mL) {
                        // Stop the PCNT unit
                        pcnt_counter_pause(evt.unit);
                        pcnt_counter_clear(evt.unit);

                        // Call the callback if it's not NULL
                        if (handle->callback) {
                            handle->callback(evt.unit, handle->volume_mL);
                        }

                        // Reset volume to zero
                        handle->volume_mL = 0;
                    }
                }
            }
        }
    }
}

static TaskHandle_t flowcounter_bgtask_hdl;

void flowcounter_init() {
    if(user_isr_handle) return;

    pcnt_isr_register(flowcounter_intr_handler, NULL, 0, &user_isr_handle);
    event_queue = xQueueCreate(10, sizeof(pcnt_evt_t));
    if (!event_queue) {
        pcnt_isr_unregister(user_isr_handle);
        user_isr_handle = NULL;
        return;
    }

    xTaskCreate(pcnt_event_handler_task, "pcnt_event_handler_task", 2048, NULL, 10, &flowcounter_bgtask_hdl);

    for(int id = PCNT_UNIT_0; id < PCNT_UNIT_MAX; id++) {
        allocated[id] = NULL;
    }
}

void flowcounter_deinit() {
    if(!user_isr_handle) return;

    pcnt_isr_unregister(user_isr_handle);
    vTaskDelete(flowcounter_bgtask_hdl);
    vQueueDelete(event_queue);
    for(int id = PCNT_UNIT_0; id < PCNT_UNIT_MAX; id++) {
        if (allocated[id]) {
            flowcounter_stop(id);
            free(allocated[id]);
            allocated[id] = NULL;
        }
    }
    user_isr_handle = NULL;
}
