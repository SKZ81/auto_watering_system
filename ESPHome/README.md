# AutoWatS_ESPHome
ESPHome script and components for the AutoWatS automatic watering system

## Building

To install the build system, as per ESPHome documentation, install `esphome` in a virtual environment.
```bash
python -m venv_esphome
. venv_esphome/bin/activate
pip install esphome
```

Then, to build : 
```bash
esphome run AutoWatS.yaml
```

First build will download and install ESP-IDF.
