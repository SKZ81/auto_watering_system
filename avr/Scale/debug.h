#ifndef __I2C_SCALE_DEBUG_H__
#define __I2C_SCALE_DEBUG_H__

#include <avr/pgmspace.h>

#ifndef DEBUG
// DEBUG is enabled by defaut, use 0 here or use -DDEBUG=0 on compilation line to deactivate
#define DEBUG 1
#endif

#if DEBUG
    #define dbg(x, ...) printf_P(PSTR(x) ,##__VA_ARGS__)
#else
    #define dbg(x, ...)
#endif

#endif
