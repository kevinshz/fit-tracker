package com.fittracker.auth.service.interfaces;

import com.fittracker.auth.persistence.entity.User;

import java.util.Optional;

public interface UserService {

    void createDefaultRoles();

    Optional<User> findByEmail(String email);
}
