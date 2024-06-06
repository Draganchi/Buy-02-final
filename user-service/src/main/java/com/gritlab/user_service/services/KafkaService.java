package com.gritlab.user_service.services;

import com.gritlab.user_service.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class KafkaService {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private KafkaTemplate<String, byte[]> byteArrayKafkaTemplate;

    public void sendUserCreatedEvent(User user) {
        kafkaTemplate.send("user-events", "User created: " + user.getId());
    }

    public void sendUserUpdatedEvent(User user) {
        kafkaTemplate.send("user-events", "User updated: " + user.getId());
    }

    public void sendUserDeletedEvent(String userId) {
        kafkaTemplate.send("user-events", "User deleted: " + userId);
    }
    public void sendToTopic(String topic, String payload) {
        kafkaTemplate.send(topic, payload);
    }
    public void sendFileToTopic(String topic, byte[] fileContent) {
        byteArrayKafkaTemplate.send(topic, fileContent);
    }
}

