#include "scale_i2c_interface.h"
#include "scale_i2c.h"
#include "i2c_callbacks.h"
#include <avr/pgmspace.h>
#include <stdio.h>
#include <stdlib.h>

#define DEBUG 1

#if DEBUG
    #define dbg(x, ...) printf_P(PSTR(x) ,##__VA_ARGS__)
#else
    #define dbg(x, ...)
#endif

// ========= Data and Dattypes ========================================
typedef struct {
    uint8_t code;
    uint8_t arg_len;
    uint8_t (*callback)(uint8_t*, uint8_t);
    //uint8_t reply_len;
} command_t;

command_t commands[] = {
    {SCALE_I2C_POWER_DOWN, 0, scale_i2c_power_down/*, 0*/},
    {SCALE_I2C_POWER_UP, 0, scale_i2c_power_up/*, 0*/},
    {SCALE_I2C_TARE, 1, scale_i2c_tare/*, 0*/},
    {SCALE_I2C_SET_ZERO_OFFSET, 3, scale_i2c_set_zero_offset/*, 0*/},
    {SCALE_I2C_SET_CALIBRATION, sizeof(float), scale_i2c_set_calibration/*, 0*/},
    {SCALE_I2C_GET_ZERO_OFFSET, 0, scale_i2c_get_zero_offset/*, 3*/},
    {SCALE_I2C_READ, 1, scale_i2c_read/*, 3*/},
    {SCALE_I2C_GET_VALUE, 1, scale_i2c_get_value/*, sizeof(float)*/},
    {SCALE_I2C_GET_CALIBRATION, 0, scale_i2c_get_calibration/*, sizeof(float)*/} //,
};

static command_t *current_command = NULL;


typedef enum {
    WAITING = 0,
    READING_ARGS = 1,
    READY = 2,
    TALKING = 3/*,
    WAIT_DONE = 4*/
} i2c_scale_status_t;

static i2c_scale_status_t status;


static uint8_t buffer[BUFFER_SIZE]={0};
static uint8_t buffer_index = 0;
static uint8_t reply_len = 0;

// =====================================================================

static inline void reset_state() {
    status = WAITING;
    reply_len = 0;
    current_command = NULL;
    buffer_index = 0;
    //memset(buffer, 0, BUFFER_SIZE);
}

command_t *get_command(uint8_t command_code) {
    command_t *command_ptr = NULL;
    for(uint8_t i = 0; i < sizeof(commands)/sizeof(command_t); i++) {
        if (commands[i].code == command_code) {
            command_ptr = &(commands[i]);
        }
    }
    if (command_ptr == NULL) {
        dbg("get_command() : Could not not find command code %d\n", command_code);
    } else {
        dbg("get_command() : command code %d found, OK\n", command_code);
    }
    return command_ptr;
}


void i2c_scale_init() {
    i2c_slave_setCallbacks(i2c_scale_receive, i2c_scale_requested);
    i2c_slave_init(SCALE_I2C_ADDRESS);
    reset_state();
}



void i2c_scale_requested(request_t req_type) {
    switch(req_type) {
         case INITIAL:
            if (status == READY) {
                status = TALKING;
            } else {
                // we were not READY to reply, then forget all previous crap said by master
                dbg("Initial query but state is not READY (status == %d)\n", status);
                reset_state();
                break;
            }
        // NOTE : if no error => no break !! execute following statement
        case CONTINUATION:
            if (status != TALKING) {
                dbg("Requested to tranmit data, while not in TALKING state (status == %d)\n", status);
                reset_state();
                break;
            }
            if (buffer_index >= reply_len) {
                // abnormal situation, give a trace here
                dbg("ERR : buffer_index >= reply_len\n");
                reset_state();
                break;
            }
            dbg("send I2C data : %x %s\n", buffer[buffer_index], (buffer_index+1)==reply_len?"(last)":"");
            i2c_slave_transmitByte(buffer[buffer_index]);
            buffer_index++;
            if (buffer_index == reply_len) { // We're done !!
                dbg("Succesfully transmitted %d bytes\n", reply_len);
                reset_state();
            }
            break;

//         case DONE:
//             if (status != WAIT_DONE) {
//                 dbg("'warning !! got i2c request 'DONE', while status not DONE (status == %d)\n", status);
//             } else {
//                 dbg("Communication end.\n");
//             }
//             reset_state();
//             break;

        default:
            dbg("i2c_scale_requested : unknown req_type %d\n", req_type);
            reset_state();
            break;
    }
}



void i2c_scale_receive(uint8_t data) {
    dbg("I2C received byte : %x\n", data);
    switch(status) {
        case WAITING:
            current_command = get_command(data);
            if (current_command != NULL) {
                buffer_index = 0;
                if (current_command->arg_len == 0) {
                    // no arg to get, just execute callback
                    reply_len = current_command->callback(buffer, BUFFER_SIZE);
                    if (reply_len > 0) {
                        status = READY; // to send data
                        buffer_index = 0;
                    } else {
                        // nothing to send, we're done
                        reset_state();
                    }
                } else {
                    // We have data arg to read
                    status = READING_ARGS;
                }
            }
            break;

        case READING_ARGS:
#if DEBUG
            if(!current_command) {
                dbg("READING_ARGS but current_command is NULL !\n");
                reset_state();
                break;
            }
#endif
            buffer[buffer_index++] = data;
            if (buffer_index == current_command->arg_len) {
                reply_len = current_command->callback(buffer, BUFFER_SIZE);
                if (reply_len > 0) {
                    status = READY; // to send data
                    buffer_index = 0;
                } else {
                    reset_state();
                }
            }
            break;

        default:
            dbg("Got data while state not WAITING nor READING_ARGS (status == %d)\n", status);
            reset_state();
    }
}
