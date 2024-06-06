package com.gritlab.user_service.repositories;

import com.gritlab.user_service.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends MongoRepository<User, String> {
    Optional<User> findByName(String name);
    Optional<User> findByEmail(String email);
}
