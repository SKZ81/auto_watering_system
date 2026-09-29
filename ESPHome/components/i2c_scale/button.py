import esphome.codegen as cg
import esphome.config_validation as cv

from esphome.components import button
from esphome.const import (
    CONF_ID,
    CONF_NAME,
)

from . import scale_ns, I2CScale

CONF_TARE = "tare"

I2CScaleTareButton = scale_ns.class_(
    "I2CScaleTareButton",
    button.Button
)

# CONFIG_SCHEMA = button.button_schema(
#     I2CScaleTareButton
# )

async def new_tare_button(config, parent):
    var = await button.new_button(config)
    cg.add(var.set_parent(parent))
    return var
