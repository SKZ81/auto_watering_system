#include "i2c_scale_button.h"
#include "i2c_scale.h"

namespace esphome {
namespace i2c_scale {

void I2CScaleTareButton::press_action()
{
    parent_->tare();
}

}
}
