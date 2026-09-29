#pragma once

#include "esphome/core/component.h"
#include "esphome/components/i2c/i2c.h"
#include "esphome/components/sensor/sensor.h"

namespace esphome {
namespace i2c_scale {

class I2CScaleTareButton;
class I2CScaleCalibrationNumber;

class I2CScale : public sensor::Sensor,
                 public PollingComponent,
                 public i2c::I2CDevice
{
  public:
    void setup() override;
    void update() override;
    void dump_config() override;

    // void set_sensor(sensor::Sensor *sensor) {
    //     sensor_ = sensor;
    // }

    void set_always_on(bool value) {
        always_on_ = value;
    }

    void set_read_samples(uint8_t value) {
        read_samples_ = value;
    }

    void set_maintenance_samples(uint8_t value) {
        maintenance_samples_ = value;
    }

    bool tare();
    bool set_zero_offset(uint32_t zero_offset);
    bool get_zero_offset(uint32_t *zero_offset);

    bool set_calibration(float calibration);

    void set_tare_button(I2CScaleTareButton *btn) {
        this->tare_button_ = btn;
    }

    void set_calibration_number(I2CScaleCalibrationNumber *num) {
        this->calibration_number_ = num;
    }

  protected:
    bool power_up_();
    bool power_down_();
    bool get_value_(float &value, uint8_t samples);
    void async_read_zero_offset_();

    // sensor::Sensor *sensor_{nullptr};

    bool always_on_{false};
    bool powered_{false};

    uint8_t read_samples_{0};
    uint8_t maintenance_samples_{0};

    I2CScaleTareButton *tare_button_{nullptr};
    I2CScaleCalibrationNumber *calibration_number_{nullptr};

    ESPPreferenceObject zero_offset_pref_;
    uint32_t zero_offset_{0};
    bool zero_offset_loaded_{false};
};

}
}
