/* Simple HTTP Server Example

   This example code is in the Public Domain (or CC0 licensed, at your option.)

   Unless required by applicable law or agreed to in writing, this
   software is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
   CONDITIONS OF ANY KIND, either express or implied.
*/

#include <esp_wifi.h>
#include <esp_event.h>
#include <esp_log.h>
#include <esp_system.h>
#include <nvs_flash.h>
#include <stdint.h>
#include <sys/param.h>
#include "driver/pcnt.h"
#include "nvs_flash.h"
#include "esp_netif.h"
#include "esp_eth.h"
#include "protocol_examples_common.h"
#include "esp_tls_crypto.h"
#include <cJSON.h>
#include <esp_http_server.h>
#include "MCP23017.h"
#include "flowcounter.h"

static const char *TAG = "AutoWatS-proto";

pcnt_unit_t counters[2];

mcp23017_t mcp23017 = {
    .i2c_addr = MCP23017_DEFAULT_ADDR + CONFIG_AUTOWATS_I2C_MCP23017_ADDR_OFFSET,
    .port = CONFIG_AUTOWATS_I2C_PORT,
    .sda_pin = CONFIG_AUTOWATS_I2C_SDA_GPIO,
    .scl_pin = CONFIG_AUTOWATS_I2C_SCL_GPIO,
    /* soldered HW pullups */
    .sda_pullup_en = GPIO_PULLUP_DISABLE,
    .scl_pullup_en = GPIO_PULLUP_DISABLE,
};

typedef struct {
    char* name;
    uint8_t pin_number;
    mcp23017_iogrp_t group;
} endpoint_entry_t;

endpoint_entry_t endpoints[] = {
    {"VLV1", 4, GRP_A},
    {"VLV2", 5, GRP_A},
    {"VLV3", 6, GRP_A},
    {"VLV4", 7, GRP_A},
    {"PFTW", 0, GRP_B},
    {"PCLW", 1, GRP_B},
    {"PRP1", 2, GRP_B},
    {"PRP2", 3, GRP_B},
    {"PRP3", 4, GRP_B},
    {NULL, 0xFF, NO_GRP}
};

/* Handler to respond with an icon file embedded in flash.
 * Browsers expect to GET website icon at URI /favicon.ico.
 * This can be overridden by uploading file with same name */
static esp_err_t favicon_get_handler(httpd_req_t *req)
{
    extern const unsigned char favicon_ico_start[] asm("_binary_favicon_ico_start");
    extern const unsigned char favicon_ico_end[]   asm("_binary_favicon_ico_end");
    const size_t favicon_ico_size = (favicon_ico_end - favicon_ico_start);
    httpd_resp_set_type(req, "image/x-icon");
    httpd_resp_send(req, (const char *)favicon_ico_start, favicon_ico_size);
    return ESP_OK;
}

static const httpd_uri_t uri_favicon_ico = {
    .uri       = "/favicon.ico",
    .method    = HTTP_GET,
    .handler   = favicon_get_handler,
    .user_ctx  = NULL
};

/* main page GET handler */

static esp_err_t index_get_handler(httpd_req_t *req)
{
    // Get handle to embedded color selection script
    extern const unsigned char index_start[] asm("_binary_index_html_start");
    extern const unsigned char index_end[]   asm("_binary_index_html_end");
    const size_t index_size = (index_end - index_start);
    ESP_LOGI(TAG, "GET /index.html");
    // Add file upload form and script which on execution sends a POST request to /upload
    httpd_resp_send(req, (const char *)index_start, index_size);

    return ESP_OK;
}

static const httpd_uri_t uri_slash = {
    .uri       = "/",
    .method    = HTTP_GET,
    .handler   = index_get_handler,
    .user_ctx  = NULL
};
static const httpd_uri_t uri_index = {
    .uri       = "/index",
    .method    = HTTP_GET,
    .handler   = index_get_handler,
    .user_ctx  = NULL
};
static const httpd_uri_t uri_index_html = {
    .uri       = "/index.html",
    .method    = HTTP_GET,
    .handler   = index_get_handler,
    .user_ctx  = NULL
};



/* Handler for POST endpoint state update */
int autowats_set_endpoint(const char* endpoint_name, bool state) {
    esp_err_t r;
    int i;
    for (i = 0; endpoints[i].name != NULL; i++)
        if (!strncmp(endpoint_name, endpoints[i].name, 4))
            break;
    if (endpoints[i].name == NULL) {
        ESP_LOGE(TAG, "Endpoint %s does not exists...", endpoints[i].name);
        return ESP_ERR_NOT_FOUND;
    }
    // NOTE : relaysGPIO states are "inverted" (off on logic high), this inversion is managed here
    if (state) {
        r = mcp23017_clear_gpio(&mcp23017, endpoints[i].pin_number, endpoints[i].group);
    } else {
        r = mcp23017_set_gpio(&mcp23017, endpoints[i].pin_number, endpoints[i].group);
    }

    if (r) {
        ESP_LOGE(TAG, "Error while setting endpoint %s to state: %s", endpoints[i].name, state?"ON":"OFF");
    }
    return r;
}

static esp_err_t endpoint_update_post_handler(httpd_req_t *req)
{
    char buf[32];
    char endpoint_name[5];
    unsigned int state;
    int ret = HTTPD_SOCK_ERR_TIMEOUT;

    if (req->content_len > 30) {
        ESP_LOGE(TAG, "Got a request with an obviously too long payload (%d bytes)", req->content_len);
        httpd_resp_send_err(req, 413, NULL);
        return ESP_FAIL;
    }

    while ((ret = httpd_req_recv(req, buf, sizeof(buf))) == HTTPD_SOCK_ERR_TIMEOUT) {
        ESP_LOGI(TAG, "Request timeout, retrying...");
    }

    if (ret != req->content_len) {
        ESP_LOGE(TAG, "BAD REQUEST. content length is %d, but read %d bytes",
                 req->content_len, ret);
        httpd_resp_send_err(req, 400, NULL);
        return ESP_FAIL;
    }

    if (sscanf(buf, "endpoint=%4s\nstate=%u", endpoint_name, &state) != 2) {
        ESP_LOGE(TAG, "Parse error, rejecting request ('%s')", buf);
        httpd_resp_send_err(req, 400, NULL);
        return ESP_FAIL;
    }

    ESP_LOGI(TAG, "request parsed. endpoint: %s, state: %s", endpoint_name, state ? "ON":"OFF");
    if (autowats_set_endpoint(endpoint_name, state) != ESP_OK) {
        httpd_resp_send_err(req, 500, "Error while setting HW endpoint state");
    }

    httpd_resp_send_chunk(req, NULL, 0);

    return ESP_OK;
}

static const httpd_uri_t uri_endpoint_update = {
    .uri       = "/endpoint_update",
    .method    = HTTP_POST,
    .handler   = endpoint_update_post_handler,
    .user_ctx  = NULL
};

static esp_err_t counters_get_handler(httpd_req_t *req) {
    // Create a cJSON object
    cJSON *root = cJSON_CreateObject();
    if (root == NULL) {
        httpd_resp_send_500(req);
        return ESP_FAIL;
    }

    // Add the counter values to the JSON object
    cJSON_AddNumberToObject(root, "counter_0", flowcounter_get_volume_mL(counters[0]));
    cJSON_AddNumberToObject(root, "counter_1", flowcounter_get_volume_mL(counters[1]));

    // Convert JSON object to string
    const char *json_response = cJSON_Print(root);
    if (json_response == NULL) {
        cJSON_Delete(root);
        httpd_resp_send_500(req);
        return ESP_FAIL;
    }

    // Set HTTP response content type to JSON
    httpd_resp_set_type(req, "application/json");

    // Send the JSON response
    httpd_resp_sendstr(req, json_response);

    // Free the JSON object and string
    cJSON_Delete(root);
    free((void *)json_response);

    return ESP_OK;
}

static const httpd_uri_t uri_counters = {
    .uri       = "/counters",
    .method    = HTTP_GET,
    .handler   = counters_get_handler,
    .user_ctx  = NULL
};

static esp_err_t counters_reset_post_handler(httpd_req_t *req) {
    flowcounter_stop(counters[0]);
    flowcounter_stop(counters[1]);
    httpd_resp_set_type(req, "text/plain");
    httpd_resp_sendstr(req, "OK");

    return ESP_OK;
}

static const httpd_uri_t uri_counters_reset = {
    .uri       = "/counters_reset",
    .method    = HTTP_POST,
    .handler   = counters_reset_post_handler,
    .user_ctx  = NULL
};

static httpd_handle_t start_webserver(void)
{
    httpd_handle_t server = NULL;
    httpd_config_t config = HTTPD_DEFAULT_CONFIG();
    config.lru_purge_enable = true;

    // Start the httpd server
    ESP_LOGI(TAG, "Starting server on port: '%d'", config.server_port);
    if (httpd_start(&server, &config) == ESP_OK) {
        // Set URI handlers
        ESP_LOGI(TAG, "Registering URI handlers");
        httpd_register_uri_handler(server, &uri_favicon_ico);
        httpd_register_uri_handler(server, &uri_slash);
        httpd_register_uri_handler(server, &uri_index);
        httpd_register_uri_handler(server, &uri_index_html);
        httpd_register_uri_handler(server, &uri_endpoint_update);
        httpd_register_uri_handler(server, &uri_counters);
        httpd_register_uri_handler(server, &uri_counters_reset);
        return server;
    }

    ESP_LOGI(TAG, "Error starting server!");
    return NULL;
}

static void stop_webserver(httpd_handle_t server)
{
    // Stop the httpd server
    httpd_stop(server);
}

static void disconnect_handler(void* arg, esp_event_base_t event_base,
                               int32_t event_id, void* event_data)
{
    httpd_handle_t* server = (httpd_handle_t*) arg;
    if (*server) {
        ESP_LOGI(TAG, "Stopping webserver");
        stop_webserver(*server);
        *server = NULL;
    }
}

static void connect_handler(void* arg, esp_event_base_t event_base,
                            int32_t event_id, void* event_data)
{
    httpd_handle_t* server = (httpd_handle_t*) arg;
    if (*server == NULL) {
        ESP_LOGI(TAG, "Starting webserver");
        *server = start_webserver();
    }
}

#define GPIO_COUNTER_1 34
#define GPIO_COUNTER_2 35

void app_main(void)
{
    static httpd_handle_t server = NULL;

    ESP_ERROR_CHECK(nvs_flash_init());
    ESP_ERROR_CHECK(esp_netif_init());
    ESP_ERROR_CHECK(esp_event_loop_create_default());

    flowcounter_init();
    counters[0] = flowcounter_alloc(GPIO_COUNTER_1, 20, 1);
    counters[1] = flowcounter_alloc(GPIO_COUNTER_2, 20, 1);
    ESP_LOGI(TAG, "counter for GPIO %d => unit %d", GPIO_COUNTER_1, counters[0]);
    ESP_LOGI(TAG, "counter for GPIO %d => unit %d", GPIO_COUNTER_2, counters[1]);
    flowcounter_start(counters[0], INT32_MAX, NULL);
    flowcounter_start(counters[1], INT32_MAX, NULL);
    /* enable flowcounter supply voltage */
    gpio_config_t io_conf = {
        .intr_type = GPIO_INTR_DISABLE,
        .mode = GPIO_MODE_OUTPUT,
        .pin_bit_mask = GPIO_NUM_32,
        .pull_down_en = GPIO_PULLDOWN_DISABLE,
        .pull_up_en = GPIO_PULLUP_DISABLE
    };
    //configure GPIO with the given settings
    gpio_config(&io_conf);
    gpio_set_level(GPIO_NUM_32, 1);

    mcp23017_init(&mcp23017);
    /* This helper function configures Wi-Fi or Ethernet, as selected in menuconfig.
     * Read "Establishing Wi-Fi or Ethernet Connection" section in
     * examples/protocols/README.md for more information about this function.
     */
    ESP_ERROR_CHECK(example_connect());

    /* Register event handlers to stop the server when Wi-Fi or Ethernet is disconnected,
     * and re-start it upon connection.
     */
#ifdef CONFIG_EXAMPLE_CONNECT_WIFI
    ESP_ERROR_CHECK(esp_event_handler_register(IP_EVENT, IP_EVENT_STA_GOT_IP, &connect_handler, &server));
    ESP_ERROR_CHECK(esp_event_handler_register(WIFI_EVENT, WIFI_EVENT_STA_DISCONNECTED, &disconnect_handler, &server));
#endif // CONFIG_EXAMPLE_CONNECT_WIFI
#ifdef CONFIG_EXAMPLE_CONNECT_ETHERNET
    ESP_ERROR_CHECK(esp_event_handler_register(IP_EVENT, IP_EVENT_ETH_GOT_IP, &connect_handler, &server));
    ESP_ERROR_CHECK(esp_event_handler_register(ETH_EVENT, ETHERNET_EVENT_DISCONNECTED, &disconnect_handler, &server));
#endif // CONFIG_EXAMPLE_CONNECT_ETHERNET

    /* Start the server for the first time */
    server = start_webserver();
}
