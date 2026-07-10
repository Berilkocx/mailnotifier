package com.beril.mailnotifier.service;

import com.beril.mailnotifier.domain.entity.AuthProvider;
import com.beril.mailnotifier.domain.entity.User;
import com.beril.mailnotifier.domain.repository.UserRepository;
import com.beril.mailnotifier.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional
    public User saveOrUpdateUser(String email, String name, String profilePictureUrl,
                                  String providerId, AuthProvider provider,
                                  String accessToken, String refreshToken, Instant tokenExpiresAt) {
        return userRepository.findByEmail(email)
                .map(existing -> {
                    existing.setName(name);
                    existing.setProfilePictureUrl(profilePictureUrl);
                    existing.setAccessToken(accessToken);
                    // refresh token yalnızca geldiğinde güncellenir (Google bazen göndermez)
                    if (refreshToken != null) {
                        existing.setRefreshToken(refreshToken);
                    }
                    existing.setTokenExpiresAt(tokenExpiresAt);
                    return userRepository.save(existing);
                })
                .orElseGet(() -> userRepository.save(User.builder()
                        .email(email)
                        .name(name)
                        .profilePictureUrl(profilePictureUrl)
                        .providerId(providerId)
                        .provider(provider)
                        .accessToken(accessToken)
                        .refreshToken(refreshToken)
                        .tokenExpiresAt(tokenExpiresAt)
                        .build()));
    }

    @Transactional
    public void updateTokens(UUID userId, String accessToken, Instant tokenExpiresAt) {
        User user = findById(userId);
        user.setAccessToken(accessToken);
        user.setTokenExpiresAt(tokenExpiresAt);
        userRepository.save(user);
    }

    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }

    public User findById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
    }
}
