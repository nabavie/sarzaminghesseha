package com.example.myapp.service;

import com.example.myapp.dto.RegistrationForm;
import com.example.myapp.model.Role;
import com.example.myapp.model.User;
import com.example.myapp.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public boolean usernameExists(String username) {
        return userRepository.existsByUsername(username);
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    public List<User> searchStorytellers(String q) {
        String query = q == null ? "" : q.trim();
        return userRepository.searchStorytellers(Role.STORYTELLER, query);
    }

    @Transactional
    public User register(RegistrationForm form) {
        User user = new User(
                form.getUsername().trim(),
                passwordEncoder.encode(form.getPassword()),
                form.getDisplayName().trim());
        user.getRoles().add(Role.LISTENER);
        if (form.isStoryteller()) {
            user.getRoles().add(Role.STORYTELLER);
        }
        return userRepository.save(user);
    }

    @Transactional
    public void updateProfile(User user, String displayName, String avatarPath) {
        user.setDisplayName(displayName.trim());
        if (avatarPath != null) {
            user.setAvatarPath(avatarPath);
        }
        userRepository.save(user);
    }

    @Transactional
    public void grantRole(User user, Role role) {
        user.getRoles().add(role);
        userRepository.save(user);
    }

    public boolean passwordMatches(User user, String rawPassword) {
        return passwordEncoder.matches(rawPassword, user.getPasswordHash());
    }

    @Transactional
    public void changePassword(User user, String rawNewPassword) {
        user.setPasswordHash(passwordEncoder.encode(rawNewPassword));
        userRepository.save(user);
    }
}
