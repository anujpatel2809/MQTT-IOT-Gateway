package com.makethone.outliers;

import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MQTTConfiguration {

    @Value("${mqtt.tb.auth-token}")
    private String token;

    @Bean
    public IMqttClient mqttLocalClient(@Value("${mqtt.local.clientId}") String clientId,
                                  @Value("${mqtt.local.hostname}") String hostname, @Value("${mqtt.local.port}") int port) throws MqttException {

        IMqttClient mqttClient = new MqttClient("tcp://" + hostname + ":" + port,clientId);

        mqttClient.connect(mqttLocalConnectOptions());

        return mqttClient;
    }

    @Bean
    public MqttConnectOptions mqttLocalConnectOptions() {
        MqttConnectOptions mqttConnectOptions=new MqttConnectOptions();
        mqttConnectOptions.setAutomaticReconnect(true);
        mqttConnectOptions.setCleanSession(true);
        mqttConnectOptions.setConnectionTimeout(10);
        return new MqttConnectOptions();
    }

    @Bean
    public IMqttClient mqttTBClient(@Value("${mqtt.tb.clientId}") String clientId,
                                       @Value("${mqtt.tb.hostname}") String hostname, @Value("${mqtt.tb.port}") int port) throws MqttException {

        IMqttClient mqttClient = new MqttClient("tcp://" + hostname + ":" + port,clientId);

        mqttClient.connect(mqttTBConnectOptions());

        return mqttClient;
    }

    @Bean
    public MqttConnectOptions mqttTBConnectOptions() {
        MqttConnectOptions mqttConnectOptions=new MqttConnectOptions();
        //mqttConnectOptions.setUserName("use-token-auth");
        mqttConnectOptions.setUserName(token);
        mqttConnectOptions.setAutomaticReconnect(true);
        mqttConnectOptions.setCleanSession(true);
        mqttConnectOptions.setConnectionTimeout(10);
        return mqttConnectOptions;
    }

}
