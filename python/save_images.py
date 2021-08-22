"""Image Saver"""

import os
import sys
from threading import Thread
from datetime import datetime
from mqtt_base import MQTTBase

class ImageSaver(MQTTBase):
    def __init__(self, config_file):
        MQTTBase.__init__(self, config_file=config_file)

    def on_connect(self, client, userdata, flags, conn_result):
        self.mqtt.subscribe(self.mqtt_config['site'] + '/camera/#')
        self.mqtt.publish('saver/status', 'connected', 0, True)
        print("Connected. Listening for images on camera/#/image")

    def on_message(self, client, userdata, message):
        parts = message.topic.split('/')
        if len(parts) == 4 and parts[3] == 'image':
            site = parts[0]
            uuid = parts[2]
            self.on_image(site, uuid, message.payload)

    def get_ouput_dir(self, uuid):
        return os.path.join(self.mqtt_config['output_dir'], uuid)

    def get_filename(self):
        return "{:%Y-%m-%dT%H%M%S_%f}.jpg".format(datetime.now())

    def on_image(self, site, uuid, data):
        print("Got image from site {} with UUID {}".format(site, uuid))

        output_dir = self.get_ouput_dir(os.path.join(site, uuid))
        if not os.path.exists(output_dir):
            os.makedirs(output_dir)

        filename = os.path.join(output_dir, self.get_filename())

        with open(filename, 'wb') as img_out:
            img_out.write(data)

def main():
    if len(sys.argv) < 2:
        print("Usage: {} config.json".format(sys.argv[0]))
        sys.exit(1)

    saver = ImageSaver(sys.argv[1])
    saver.mqtt.will_set('saver/status', 'disconnected', 0, True)
    saver.connect()
    saver.loop()

if __name__ == '__main__':
    main()
