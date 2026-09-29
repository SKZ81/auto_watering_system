#pragma once

#include "esphome/components/button/button.h"

namespace esphome {
namespace i2c_scale {

class I2CScale;

class I2CScaleTareButton : public button::Button {
 public:
    void set_parent(I2CScale *parent) {
        parent_ = parent;
    }

 protected:
    void press_action() override;

    I2CScale *parent_;
};

}
}
