package com.nafis.groupchat.service;

import com.nafis.groupchat.dto.LoginRequest;
import com.nafis.groupchat.entity.User;
import com.nafis.groupchat.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private final JwtService jwtService;



    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }


    public User register(User user) {

        if(userRepository.findByEmail(user.getEmail()).isPresent()) {
            throw new RuntimeException("Email already exists");
        }

        user.setPassword(
                passwordEncoder.encode(user.getPassword())
        );

        return userRepository.save(user);
    }

    public String login(LoginRequest request) {

        User user = userRepository
                .findByEmail(request.getEmail())
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        if (passwordEncoder.matches(
                request.getPassword(),
                user.getPassword())) {

            return jwtService.generateToken(user.getEmail());
        }

        throw new RuntimeException("Invalid Password");
//        return "Invalid Password";
    }
}