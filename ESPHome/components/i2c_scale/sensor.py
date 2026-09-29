import esphome.codegen as cg
import esphome.config_validation as cv

from esphome.components import sensor, i2c, button, number
from esphome.const import *

from .button import (
    CONF_TARE,
    new_tare_button,
    I2CScaleTareButton,
)

from .number import (
    CONF_CALIBRATION,
    new_calibration_number,
    I2CScaleCalibrationNumber,
)

CONF_ALWAYS_ON = "always_on"
CONF_READ_SAMPLES = "read_samples"
CONF_MAINTENANCE_SAMPLES = "maintenance_samples"
CONF_RESTORE_VALUE = "restore_value"

scale_ns = cg.esphome_ns.namespace("i2c_scale")

I2CScale = scale_ns.class_(
    "I2CScale",
    cg.PollingComponent,
    i2c.I2CDevice,
)

CONFIG_SCHEMA = sensor.sensor_schema(
    unit_of_measurement="g",
    accuracy_decimals=1,
).extend({
    cv.GenerateID(): cv.declare_id(I2CScale),

    cv.Optional(CONF_ALWAYS_ON, default=False): cv.boolean,
    cv.Optional(CONF_READ_SAMPLES, default=0): cv.int_range(min=0,max=255),
    cv.Optional(CONF_MAINTENANCE_SAMPLES, default=0): cv.int_range(min=0,max=255),

    cv.Optional(CONF_TARE): button.button_schema(
        I2CScaleTareButton
    ),

    cv.Optional(CONF_CALIBRATION): number.number_schema(
        I2CScaleCalibrationNumber,
    ).extend({
        cv.GenerateID(): cv.declare_id(I2CScaleCalibrationNumber),

        cv.Optional(CONF_MIN_VALUE, default=0.0): cv.float_,
        cv.Optional(CONF_MAX_VALUE, default=1000000.0): cv.float_,
        cv.Optional(CONF_STEP, default=0.1): cv.float_,
        cv.Optional(CONF_RESTORE_VALUE, default=False): cv.boolean,
    }),

}).extend(
    cv.polling_component_schema("10s")
).extend(
    i2c.i2c_device_schema(0x17)
)

async def to_code(config):
    var = cg.new_Pvariable(config[CONF_ID])

    await cg.register_component(var, config)
    await sensor.register_sensor(var, config)
    await i2c.register_i2c_device(var, config)

    cg.add(var.set_always_on(config[CONF_ALWAYS_ON]))
    cg.add(var.set_read_samples(config[CONF_READ_SAMPLES]))
    cg.add(var.set_maintenance_samples(config[CONF_MAINTENANCE_SAMPLES]))

    if CONF_TARE in config:
        tare = await new_tare_button(
            config[CONF_TARE],
            var
        )
        cg.add(var.set_tare_button(tare))

    if CONF_CALIBRATION in config:
        calibration = await new_calibration_number(
            config[CONF_CALIBRATION],
            var
        )
        cg.add(var.set_calibration_number(calibration))

        cg.add(calibration.set_restore_value(
            config[CONF_CALIBRATION][CONF_RESTORE_VALUE]
        ))
