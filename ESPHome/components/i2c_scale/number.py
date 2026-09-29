import esphome.codegen as cg
import esphome.config_validation as cv

from esphome.components import number
from esphome.const import (
    CONF_ID,
    CONF_MIN_VALUE,
    CONF_MAX_VALUE,
    CONF_STEP,
)

from . import scale_ns, I2CScale

CONF_CALIBRATION = "calibration"

I2CScaleCalibrationNumber = scale_ns.class_(
    "I2CScaleCalibrationNumber",
    number.Number
)

# CONFIG_SCHEMA = number.number_schema(
#     I2CScaleCalibrationNumber,
#     # min_value=0.0,
#     # max_value=1000000.0,
#     # step=0.1,
# ).extend({
#     cv.Optional(CONF_MIN_VALUE, default=0.0): cv.float_,
#     cv.Optional(CONF_MAX_VALUE, default=1000000.0): cv.float_,
#     cv.Optional(CONF_STEP, default=0.1): cv.float_,
# })

async def new_calibration_number(config, parent):
    # var = cg.new_Pvariable(config[CONF_ID])
    var = await number.new_number(
        config,
        min_value=config[CONF_MIN_VALUE],
        max_value=config[CONF_MAX_VALUE],
        step=config[CONF_STEP],
    )
    cg.add(var.set_parent(parent))
    return var
