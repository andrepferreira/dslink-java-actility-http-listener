# Actility LoRaWAN Network Server dslink for Cisco Kinetic EFM
This link is an implementation of an http listener compatible with receiving sensor data from the Actility NS. 

## Usage

* Install the dslink from the EFM Manager interface as usual
* Start the dslink
* Configure the listening port by adding a socket number to the dslink
* Set the logging level if required

* Configure the Actility NS destination to be the Kinetic EFM Server IP:port

Inbound data will appear on the dslink node and can be used in a dataflow. Sensor data is passed through as raw JSON and can be decoded with a JSON Parser widget

## Note
The repo contains both the source and the binaries. The source is in the directory **dslink-java** and the binaries (for Linux) are in the **binaries** directory. Normally, you would only need to clone the source directory for your own dev work. 

## Credits and references

1. dslink-java-template was used as the starting point
2. All code written by Simon Ball (Britehouse South Africa, a division of NTT Inc)
