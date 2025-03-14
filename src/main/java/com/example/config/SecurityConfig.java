package com.example.config;

import com.example.role.Role;
import com.example.role.RoleRepository;
import com.example.user.User;
import com.example.user.UserRepository;
import com.example.user.UserService;
import lombok.RequiredArgsConstructor;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import java.util.List;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final CustomUserDetailsService customUserDetailsService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.headers().frameOptions().sameOrigin();
        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(
                                "/login",
                                "/register",
                                "/otp",
                                "/verify-otp",
                                "/materials/**",
                                "assessments/expired-link", // Allow expired link page
                                "assessments/invite/**",   // Allow all invite-related links (Take Assessment)
                                "/already-assessed",
                                "assessments/create/**",
                                "assessments/detail/{id}",
                                "assessments/export",
                                "assessments/import",
                                "assessments/duplicate/{id}",
                                "assessments/edit/",
                                "assessments/{id}/preview"
                                // Allow all invite-related links (Take Assessment)
                        ).permitAll()
                        .requestMatchers("exercises/profile/**").authenticated()
                        .anyRequest().authenticated()

                )
                .formLogin(form -> form.loginPage("/login").permitAll())
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login")
                        .permitAll()
                )
                .rememberMe(rememberMe -> rememberMe
                        .key("uniqueAndSecret")
                        .tokenValiditySeconds(7 * 86400)
                        .userDetailsService(customUserDetailsService)
                );

        return http.build();
    }


    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public CommandLineRunner initData(RoleRepository roleRepository, UserService userService, UserRepository userRepository) {
        return args -> {
            if (!roleRepository.existsByName("SUPERADMIN")) {
                roleRepository.save(Role.builder().name("SUPERADMIN").build());
            }

            if (!userRepository.existsByUsername("superadmin")) {
                // create user with role SUPERADMIN
                User user = User.builder()
                        .username("superadmin")
                        .password(passwordEncoder().encode("123456"))
                        .email("spadmin@gmail.com")
                        .roles(List.of(roleRepository.findByName("SUPERADMIN").get()))
                        .firstName("Super")
                        .lastName("Admin")
                        .isLocked(false)
                        .is2faEnabled(false)
                        .build();
                userService.createUser(user);
            }
        };
    }
}
