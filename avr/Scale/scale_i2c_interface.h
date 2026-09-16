#ifndef __SCALE_I2C_ITF_H__
#define __SCALE_I2C_ITF_H__

// NOTE : works well with "standalone" I²C connection on very short range.
// With mid-range connection (2.5m) external pullups were needed (used 1.2kOhm as I did not found 4.7k ones... Stronger pullup means more power consuption when pulling down the bus.)


// Address
#define SCALE_I2C_ADDRESS 0x17
// Frequency
#define SCALE_I2C_FREQUENCY 100000 // Bauds

// ===========   WRITE commands (no return)  ==================

#define SCALE_I2C_POWER_DOWN         0x01  // no arg
#define SCALE_I2C_POWER_UP           0x02  // no arg
#define SCALE_I2C_TARE               0x03  // arg : nb reads to mean. 0 is default (SCALE_I2C_DEFAULT_NB_READS)
// NB : tare is NOT done at boot (as for now)
#define SCALE_I2C_SET_ZERO_OFFSET    0x04  // "manual tare". arg : 3 bytes (MSB first), becomes the new "zero" reading value
#define SCALE_I2C_SET_CALIBRATION    0x05  // arg : float (4bytes, MSB first)

#define SCALE_I2C_SET_ASYNC_NB_READS 0x06  // arg : 1 bytes :
// number of times to read before meaning values in ASYNC_READ.
// 0 deactivate ASYNC_READ mecanism.nothing
// (defaults to SCALE_I2C_DEFAULT_NB_READS at boot).
#define SCALE_I2C_SET_ASYNC_PERIOD   0x07  // arg : 1 bytes
// Number of seconds between 2 ASYNC_READ divided by 10.
// E.g. passing 6 here will trigger an ASYNC_READ every minute (6*10 s)
#define SCALE_I2C_ASYNC_TARE         0x08  // arg : nb reads to mean. 0 is default (SCALE_I2C_DEFAULT_NB_READS).
// Triggers an async tare


// ===========   READ commands   ==============================

#define SCALE_I2C_GET_ZERO_OFFSET    0x80  // No arg. returns 3 bytes (MSB first) corresponding to absolute reading of zero offset.
#define SCALE_I2C_READ               0x81  // arg : 1 bytes, number of times to read before meaning values, 0 is default (SCALE_I2C_DEFAULT_NB_READS)
// return 3 bytes (mean value read over 10 times MINUS zero offset)

#define SCALE_I2C_GET_VALUE          0x82  // arg : 1 bytes, number of times to read before meaning values, 0 is default (SCALE_I2C_DEFAULT_NB_READS).
// return float (4 bytes, MSB first) : (mean_read_value - zero_offset) / calibration
// see SCALE_I2C_SET_ZERO_OFFSET and SCALE_I2C_CALIBRATION to set parameters of this formula
#define SCALE_I2C_GET_CALIBRATION    0x83  // No arg. Returns float (4 bytes, MSB first).

#define SCALE_I2C_GET_ASYNC_VALUE    0x84  // No arg.
// return : same as GET_VALUE, but returns the last ASYNC_READ value

#define SCALE_I2C_GET_CONFIG_BYTE    0xF0  // No arg.
// return : 1 config byte, each biut corresponding to wether a config value has been initialized from i²c sinc boot or not.
// bit 0 : calibration configured
// bit 1 : zero offset configured
// bit 2 : async nb read configured
// bit 3 : async period configured

// -----------   arbitrary/default values ---------------------
#define SCALE_I2C_DEFAULT_NB_READS   10   // Number of reads meaned, when 0 is passed to SCALE_I2C_GET_VALUE or SCALE_I2C_READ
#define SCALE_I2C_BUFFER_SIZE        8    // Size of argument/reply buffer. Actually sizeof(float) == 4 should be enough.
#define SCALE_I2C_DEFAULT_ASYNC_PERIOD 60 // in seconds.
                                          // 0 deactivate ASYNC_READ
#define SCALE_I2C_DEFAULT_CALIBRATION (float)12800
#endif
