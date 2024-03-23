# AUTOWATS -- ESP32 core control

This directory has been kickstarted using the esp32 simple HTTPd server example.

## HTTP Methods

  * (GET)  `/index.html` : the server replies with the content of `./files/index.html`,
  * (GET)  `/index` and `/`: "redirect" to `/index.html` (same C function handler),
  * (GET)  `/favicon.ico`: allow to display favicon in the client browser (reply with content of `./files/favicon.ico`),
  * (POST) `/endpoint_update`: Request payload must match `"endpoint=%4s\nstate=%u"` format. `endpoint` is a 4 char string
reprensenting one of the actionner in the system. Actual endpoint list depends on hardware setup (pumps, valves, lights & so on). 
`state` is either 0 (OFF) or 1 (ON).
  * (GET)  `/plants/search_id`: takes one URL parameter `id` which is an UUID (basically any string accepted as for now). 
Replies with a JSON contening know info for that ID, or an error.
 
## Project options

### HSPI

Pin configurable.

Used for LCD display (RA8865) control, ideally without CS (screen always asserted).

### VSPI

Pin TBD configurable.

Used for communication with SDcard (and maybe other device).

### I²C

Pin not configurable (but good to have).

Port 0 is used for communication with IO expander, with controls actuator relays.
