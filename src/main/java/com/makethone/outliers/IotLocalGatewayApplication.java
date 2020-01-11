package com.makethone.outliers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@SpringBootApplication
public class IotLocalGatewayApplication {

    static Map<String, DeviceData> deviceCache = new HashMap<>();

    public static void main(String[] args) throws MqttException {
        ConfigurableApplicationContext context = SpringApplication.run(IotLocalGatewayApplication.class, args);

        IMqttClient mqttLocalClient = (IMqttClient) context.getBean("mqttLocalClient");
        IMqttClient mqttTBClient = (IMqttClient) context.getBean("mqttTBClient");
        RestTemplate restTemplate = context.getBean(RestTemplate.class);
        context.getEnvironment().getProperty("hr-ms.url");
        ObjectMapper objectMapper = new ObjectMapper();

        mqttLocalClient.subscribe("v1/devices/me/telemetry", (topic, msg) -> {
            System.out.println("Received -> " + new String(msg.getPayload()));

            SensorData sensorData = objectMapper.readValue(msg.getPayload(), SensorData.class);

            if (deviceCache.containsKey(sensorData.getDeviceId())) {
                System.out.println("DeviceId present in Cache");
                if (deviceCache.get(sensorData.getDeviceId()).getAuthorized() == true) {
                    System.out.println("Authorized device");
                    MqttMessage mqttMessage = new MqttMessage();
                    mqttMessage.setPayload(msg.getPayload());
                    mqttMessage.setQos(2);
                    mqttMessage.setRetained(true);
                    mqttTBClient.publish("v1/devices/me/telemetry", mqttMessage);
                }
            } else {
                System.out.println("New Device");
                deviceCache.put(sensorData.getDeviceId(), new DeviceData(sensorData.getDeviceId(), null, false));

                HttpHeaders httpHeaders = new HttpHeaders();
                httpHeaders.set("source", "GATEWAY");
                HttpEntity httpEntity = new HttpEntity(httpHeaders);

                try {
                    ResponseEntity<DeviceData> responseEntity = restTemplate.exchange(context.getEnvironment().getProperty("hr-ms.url") + sensorData.getDeviceId(), HttpMethod.GET, httpEntity, DeviceData.class);
                    System.out.println(responseEntity);
                    DeviceData deviceData = new DeviceData(sensorData.getDeviceId(), responseEntity.getBody().getAccessToken(), true);
                    deviceCache.put(sensorData.getDeviceId(), deviceData);

                    MqttMessage mqttMessage = new MqttMessage();
                    mqttMessage.setPayload(objectMapper.writeValueAsString(sensorData).getBytes());
                    mqttMessage.setQos(2);
                    mqttMessage.setRetained(true);
                    mqttTBClient.publish("v1/devices/me/telemetry", mqttMessage);

                } catch (HttpStatusCodeException e) {
                    System.out.println(e.getMessage());
                    System.out.println(e);
                    DeviceData deviceData = new DeviceData(sensorData.getDeviceId(), null, false);
                    deviceCache.put(sensorData.getDeviceId(), deviceData);
                }
            }
        });

    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

}
