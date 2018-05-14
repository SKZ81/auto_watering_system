#ifndef __SCALE_I2C_ITF_H__
#define __SCALE_I2C_ITF_H__

// Address
#define SCALE_I2C_ADDRESS 23

// ===========   WRITE commands (no return)  ==================

#define SCALE_I2C_POWER_DOWN         1    // no arg
#define SCALE_I2C_POWER_UP           2    // no arg
#define SCALE_I2C_TARE               3    // arg : nb reads to mean. 0 is default (SCALE_I2C_DEFAULT_NB_READS)
                                          // NB : tare is NOT done at boot (as for now)
#define SCALE_I2C_SET_ZERO_OFFSET    4    // "manual tare". arg : 3 bytes (MSB first), becomes the new "zero" reading value
#define SCALE_I2C_SET_CALIBRATION    5    // arg : float (4bytes, MSB first)



// ===========   READ commands   ==============================

#define SCALE_I2C_GET_ZERO_OFFSET    128  // No arg. returns 3 bytes (MSB first) corresponding to absolute reading of zero offset.
#define SCALE_I2C_READ               129  // arg : 1 bytes, number of times to read before meaning values, 0 is default (SCALE_I2C_DEFAULT_NB_READS)
                                          // return 3 bytes (mean value read over 10 times MINUS zero offset)

#define SCALE_I2C_GET_VALUE          130  // arg : 1 bytes, number of times to read before meaning values, 0 is default (SCALE_I2C_DEFAULT_NB_READS).
                                          // return float (4 bytes, MSB first) : (mean_read_value - zero_offset) / calibration
                                          // see SCALE_I2C_SET_ZERO_OFFSET and SCALE_I2C_CALIBRATION to set parameters of this formula
#define SCALE_I2C_GET_CALIBRATION    131  // No arg. Returns float (4 bytes, MSB first).



// -----------   arbitrary/default values ---------------------
#define SCALE_I2C_DEFAULT_NB_READS   10   // Number of reads meaned, when 0 is passed to SCALE_I2C_GET_VALUE or SCALE_I2C_READ
#define BUFFER_SIZE                  8    // Size of argument/reply buffer. Actually sizeof(float) == 4 should be enough.

#endif
