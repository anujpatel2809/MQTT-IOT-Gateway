package com.makethone.outliers;

import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class IotLocalGatewayApplication {

	public static void main(String[] args) throws MqttException {
		ConfigurableApplicationContext context= SpringApplication.run(IotLocalGatewayApplication.class, args);

		IMqttClient mqttLocalClient = (IMqttClient) context.getBean("mqttLocalClient");
		IMqttClient mqttTBClient = (IMqttClient) context.getBean("mqttTBClient");

		mqttLocalClient.subscribe("v1/devices/me/telemetry",(topic,msg)->{
			System.out.println(msg.getId() + " -> " + new String(msg.getPayload()));
			MqttMessage mqttMessage = new MqttMessage();
			mqttMessage.setPayload(msg.getPayload());
			mqttMessage.setQos(2);
			mqttMessage.setRetained(true);
			mqttTBClient.publish("v1/devices/me/telemetry",mqttMessage);
		});

	}

}
