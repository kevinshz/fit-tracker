package com.fittracker.auth.service.implementation;

import com.fittracker.auth.persistence.entity.Role;
import com.fittracker.auth.persistence.entity.User;
import com.fittracker.auth.persistence.repository.RoleRepository;
import com.fittracker.auth.persistence.repository.UserRepository;
import com.fittracker.auth.service.interfaces.UserService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;

    @PostConstruct
    @Override
    public void createDefaultRoles() {
        createRoleIfNotExists("ROLE_USER");
        createRoleIfNotExists("ROLE_ADMIN");
        log.info("Roles por defecto creados/verificados");
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    private void createRoleIfNotExists(String roleName) {
        if (roleRepository.findByName(roleName).isEmpty()) {
            Role role = Role.builder()
                    .name(roleName)
                    .build();
            roleRepository.save(role);
            log.info("Rol creado: {}", roleName);
        }
    }
}
