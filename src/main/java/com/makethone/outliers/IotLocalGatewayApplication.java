package com.makethone.outliers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
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
import java.util.UUID;

@SpringBootApplication
public class IotLocalGatewayApplication {

    static Map<String, DeviceData> deviceCache = new HashMap<>();

    public static void main(String[] args) throws MqttException, JsonProcessingException {
        ConfigurableApplicationContext context = SpringApplication.run(IotLocalGatewayApplication.class, args);

        IMqttClient mqttLocalClient = (IMqttClient) context.getBean("mqttLocalClient");
//        IMqttClient mqttTBClient = (IMqttClient) context.getBean("mqttTBClient");
        RestTemplate restTemplate = context.getBean(RestTemplate.class);

        ObjectMapper objectMapper = new ObjectMapper();

        mqttLocalClient.subscribe("v1/devices/me/telemetry", (topic, msg) -> {
            System.out.println("Received -> " + new String(msg.getPayload()));

            SensorData sensorData = objectMapper.readValue(new String(msg.getPayload()), SensorData.class);

            if (deviceCache.containsKey(sensorData.getDeviceId())) {
                System.out.println("DeviceId present in Cache");
                if (deviceCache.get(sensorData.getDeviceId()).getAuthorized() == true) {
                    System.out.println("Authorized device");

                    IMqttClient mqttClient = new MqttClient("tcp://" + context.getEnvironment().getProperty("mqtt.tb.hostname") + ":" + 1883, "tb-mqtt");
                    MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
                    mqttConnectOptions.setUserName(deviceCache.get(sensorData.getDeviceId()).getAccessToken());
                    mqttConnectOptions.setAutomaticReconnect(true);
                    mqttConnectOptions.setCleanSession(true);
                    mqttConnectOptions.setConnectionTimeout(10);
                    mqttClient.connect(mqttConnectOptions);

                    MqttMessage mqttMessage = new MqttMessage();
                    mqttMessage.setPayload(msg.getPayload());
                    mqttMessage.setQos(2);
                    mqttMessage.setRetained(true);
                    mqttClient.publish("v1/devices/me/telemetry", mqttMessage);

                    mqttClient.disconnect();

                }
            } else {
                System.out.println("New Device");
                deviceCache.put(sensorData.getDeviceId(), new DeviceData(sensorData.getDeviceId(), null, false));

                HttpHeaders httpHeaders = new HttpHeaders();
                httpHeaders.set("source", "GATEWAY");
                HttpEntity httpEntity = new HttpEntity(httpHeaders);

                try {
                    System.out.println(context.getEnvironment().getProperty("hr-ms.url") + sensorData.getDeviceId());
                    ResponseEntity<DeviceData> responseEntity = restTemplate.exchange(context.getEnvironment().getProperty("hr-ms.url") + sensorData.getDeviceId(), HttpMethod.GET, httpEntity, DeviceData.class);
                    System.out.println(responseEntity);
                    DeviceData deviceData = new DeviceData(sensorData.getDeviceId(), responseEntity.getBody().getAccessToken(), true);
                    System.out.println(deviceData);
                    deviceCache.put(sensorData.getDeviceId(), deviceData);

                    MqttClient mqttClient = new MqttClient("tcp://" + context.getEnvironment().getProperty("mqtt.tb.hostname") + ":" + 1883, "tb-mqtt");
                    MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
                    mqttConnectOptions.setUserName(deviceCache.get(sensorData.getDeviceId()).getAccessToken());
                    mqttConnectOptions.setAutomaticReconnect(true);
                    mqttConnectOptions.setCleanSession(true);
//                    mqttConnectOptions.setConnectionTimeout(1);
                    mqttClient.connect(mqttConnectOptions);

                    MqttMessage mqttMessage = new MqttMessage();
                    mqttMessage.setPayload(objectMapper.writeValueAsString(sensorData).getBytes());
                    mqttMessage.setQos(2);
                    mqttMessage.setRetained(true);
                    mqttClient.publish("v1/devices/me/telemetry", mqttMessage);

                    mqttClient.disconnect();

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
