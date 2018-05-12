# Import package
import paho.mqtt.client as mqtt
import ssl
import time
import sys

user_id = sys.argv[1]
user_name = sys.argv[2]
file_size = sys.argv[3]

# Define Variables
MQTT_PORT = 8883
MQTT_KEEPALIVE_INTERVAL = 45
MQTT_TOPIC = "CloudComputingStoreInDynamoDb"
MQTT_MSG = '{"UserId" : "'+user_id+'", "UserName": "'+user_name+'", "FileSize" : '+file_size+'}'

MQTT_HOST = "a2dixptvnp1o4e.iot.us-east-1.amazonaws.com"
CA_ROOT_CERT_FILE = "root-CA.crt"
THING_CERT_FILE = "CloudComptingAndroid.cert.pem"
THING_PRIVATE_KEY = "CloudComptingAndroid.private.key"


# Define on_publish event function
def on_publish(client, userdata, mid):
        print "Message Published..."


# Initiate MQTT Client
mqttc = mqtt.Client()

# Register publish callback function
mqttc.on_publish = on_publish

# Configure TLS Set
mqttc.tls_set(CA_ROOT_CERT_FILE, certfile=THING_CERT_FILE, keyfile=THING_PRIVATE_KEY, cert_reqs=ssl.CERT_REQUIRED, tls_version=ssl.PROTOCOL_TLSv1_2, ciphers=None)

# Connect with MQTT Broker
mqttc.connect(MQTT_HOST, MQTT_PORT, MQTT_KEEPALIVE_INTERVAL)
mqttc.loop_start()

mqttc.publish(MQTT_TOPIC,MQTT_MSG,qos=1)
time.sleep(1)
