#include "i2c_scale_number.h"
#include "i2c_scale.h"

namespace esphome {
namespace i2c_scale {

static const char *TAG = "i2c_scale-calibration";

void I2CScaleCalibrationNumber::load_persistant_value(uint8_t address) {
    if (!restore_value_)
        return;

    uint32_t pref_key =
        fnv1_hash(("i2c_scale_calibration_" +
               str_sprintf("%02x", address)).c_str());

    calibration_pref_ =
        global_preferences->make_preference<float>(pref_key);

    if (calibration_pref_.load(&calibration_))
    {
        ESP_LOGI(TAG, "Loaded calibration value for address 0x%x: %f", address, calibration_);

        parent_->set_calibration(calibration_);
        publish_state(calibration_);
    }
}

void I2CScaleCalibrationNumber::control(float value)
{
    if(parent_->set_calibration(value))
    {
        publish_state(value);

        if (restore_value_)
            calibration_pref_.save(&value);
    }}

}
}
