package gg.agit.konect.domain.user.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import gg.agit.konect.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserActivityService {

    private final UserRepository userRepository;

    @Transactional
    public void updateLastLoginAt(Integer userId) {
        if (userId == null) {
            return;
        }

        userRepository.getById(userId).updateLastLoginAt(LocalDateTime.now());
    }

    @Transactional
    public void updateLastActivityAt(Integer userId) {
        if (userId == null) {
            return;
        }

        userRepository.getById(userId).updateLastActivityAt(LocalDateTime.now());
    }
}
