package com.example.auth;

import com.example.role.RoleService;
import com.example.user.User;
import com.example.user.UserRepository;
import com.example.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final RoleService roleService;
    private final PasswordEncoder passwordEncoder;
    public void activateUser(User user) {
        // hash password
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        // default role
        user.setRoles(List.of(roleService.findByName("STUDENT")));
        userRepository.save(user);
    }
}
