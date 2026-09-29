#include "i2c_scale.h"
#include "esphome/core/log.h"
#include "i2c_scale_number.h"
#include "i2c_scale_button.h"

namespace esphome {
namespace i2c_scale {

static const char *TAG = "i2c_scale";

static constexpr uint8_t CMD_POWER_DOWN      = 0x01;
static constexpr uint8_t CMD_POWER_UP        = 0x02;
static constexpr uint8_t CMD_SET_ZERO_OFFSET = 0x04;
static constexpr uint8_t CMD_SET_CALIBRATION = 0x05;
static constexpr uint8_t CMD_TARE            = 0x08;  // ASYNC

static constexpr uint8_t CMD_GET_ZERO_OFFSET = 0x80;
static constexpr uint8_t CMD_GET_VALUE       = 0x84;  // ASYNC

static constexpr uint8_t CMD_GET_CONFIG_BYTE = 0xF0;
static constexpr uint8_t CONFIG_HAS_CALIBRATION  = 0x01;
static constexpr uint8_t CONFIG_HAS_ZERO_OFFSET  = 0x02;
static constexpr uint8_t CONFIG_HAS_ASYNC_NBREAD = 0x03;
static constexpr uint8_t CONFIG_HAS_ASYNC_PERIOD = 0x04;


void I2CScale::setup() {
    if (always_on_)
        power_up_();
    if (calibration_number_) {
        ESP_LOGI(TAG, "Initializing calibration number");
        calibration_number_->load_persistant_value(this->address_);
    }

    uint32_t pref_key =
        fnv1_hash(("i2c_scale_zero_offset_" +
               str_sprintf("%02x", this->address_)).c_str());

    zero_offset_pref_ =
        global_preferences->make_preference<float>(pref_key);

    if (zero_offset_pref_.load(&zero_offset_))
    {
        ESP_LOGI(TAG, "Loaded zero_offset value for address 0x%x: 0x%lx",
                 this->address_, zero_offset_);
        zero_offset_loaded_ = true;
        if (!set_zero_offset(zero_offset_)) {
            ESP_LOGE(TAG, "Could not set scale zero_offset value for address 0x%x",
                     this->address_);
        }
    }
    dump_config();
}

void I2CScale::update() {
    if (!always_on_)
        power_up_();

    float value;

    if (get_value_(value, read_samples_)) {
        publish_state(value);
    }

    if (!always_on_)
        power_down_();
}

void I2CScale::dump_config() {
    ESP_LOGCONFIG(TAG, "Scale I2C");
    LOG_I2C_DEVICE(this);
    LOG_UPDATE_INTERVAL(this);

    ESP_LOGCONFIG(TAG, "Always on: %s", YESNO(always_on_));
    if (calibration_number_)
        ESP_LOGCONFIG(TAG, "Calibration: %f",
                      calibration_number_->get_calibration_value());
    ESP_LOGCONFIG(TAG, "zero_offset: 0x%lx (%lu)", zero_offset_, zero_offset_);
}

bool I2CScale::power_up_() {
    uint8_t cmd = CMD_POWER_UP;
    return write(&cmd, 1) == i2c::ERROR_OK;
}

bool I2CScale::power_down_() {
    uint8_t cmd = CMD_POWER_DOWN;
    return write(&cmd, 1) == i2c::ERROR_OK;
}

bool I2CScale::get_value_(float &value, uint8_t samples) {
    uint8_t tx_conf[1] = {
        CMD_GET_CONFIG_BYTE
    };
    uint8_t config_byte;

    if (write(tx_conf, 1) != i2c::ERROR_OK)
        return false;
    if (read(&config_byte, 1) != i2c::ERROR_OK)
        return false;
    if ( !(config_byte & CONFIG_HAS_CALIBRATION)
         && calibration_number_)
        set_calibration(calibration_number_->get_calibration_value());
    if ( !(config_byte & CONFIG_HAS_ZERO_OFFSET)
          && zero_offset_loaded_)
        set_zero_offset(zero_offset_);


    uint8_t tx[1] = {
        CMD_GET_VALUE
    };

    if (write(tx, 1) != i2c::ERROR_OK)
        return false;

    uint8_t rx[4];

    if (read(rx, 4) != i2c::ERROR_OK)
        return false;

    union {
        float f;
        uint8_t b[4];
    } conv;

    conv.b[3] = rx[3];
    conv.b[2] = rx[2];
    conv.b[1] = rx[1];
    conv.b[0] = rx[0];

    value = conv.f;
    publish_state(value);

    ESP_LOGI(TAG, "I²C Scale, read value=%f", value);
    return true;
}

bool I2CScale::tare() {
    if (!always_on_)
        power_up_();

    uint8_t tx[2] = {
        CMD_TARE,
        maintenance_samples_
    };

    bool ok = write(tx, 2) == i2c::ERROR_OK;

    // Wait 100 ms then try to read the new zero offset
    set_timeout("get_zero_offset_async", 100, [this]() {
            async_read_zero_offset_();
    });

    if (!always_on_)
        power_down_();

    return ok;
}

void I2CScale::async_read_zero_offset_()
{
    if (get_zero_offset(&zero_offset_)) {
        // On success, save the zero offset value in preferences
        zero_offset_pref_.save(&zero_offset_);
    } else {
        // On failure, retry in 100 ms
        set_timeout("get_zero_offset_async", 100, [this]() {
            async_read_zero_offset_();
        });
    }
}

bool I2CScale::set_zero_offset(uint32_t zero_offset) {
    ESP_LOGI(TAG, "I²C scale, set zero_offset to 0x%lx", zero_offset);

    if (!always_on_)
        power_up_();

    union {
        uint32_t v;
        uint8_t b[4];
    } conv;
    conv.v = zero_offset;

    uint8_t tx[4];

    tx[0] = CMD_SET_ZERO_OFFSET;
    tx[1] = conv.b[2];
    tx[2] = conv.b[1];
    tx[3] = conv.b[0];

    bool ok = write(tx, 4) == i2c::ERROR_OK;

    if (!always_on_)
        power_down_();

    return ok;
}

bool I2CScale::get_zero_offset(uint32_t *zero_offset) {
    uint8_t tx[1] = {
        CMD_GET_ZERO_OFFSET
    };

    if (write(tx, 1) != i2c::ERROR_OK)
        return false;

    uint8_t rx[3];

    if (read(rx, 3) != i2c::ERROR_OK)
        return false;

    union {
        uint32_t v;
        uint8_t b[4];
    } conv;

    conv.b[3] = 0;
    conv.b[2] = rx[0];
    conv.b[1] = rx[1];
    conv.b[0] = rx[2];

    *zero_offset = conv.v;
    ESP_LOGI(TAG, "I²C Scale, read zero_offset=0x%lx", *zero_offset);
    return true;
}

bool I2CScale::set_calibration(float calibration) {
    ESP_LOGI(TAG, "I²C scale, set calibration to %f", calibration);

    if (!always_on_)
        power_up_();

    union {
        float f;
        uint8_t b[4];
    } conv;

    conv.f = calibration;

    uint8_t tx[5];

    tx[0] = CMD_SET_CALIBRATION;
    tx[1] = conv.b[0];
    tx[2] = conv.b[1];
    tx[3] = conv.b[2];
    tx[4] = conv.b[3];

    bool ok = write(tx, 5) == i2c::ERROR_OK;

    if (!always_on_)
        power_down_();

    return ok;
}

}
}
