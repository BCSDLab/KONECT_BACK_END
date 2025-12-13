package gg.agit.konect.user.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import gg.agit.konect.global.code.ApiResponseCode;
import gg.agit.konect.global.exception.CustomException;
import gg.agit.konect.security.enums.Provider;
import gg.agit.konect.university.model.University;
import gg.agit.konect.university.repository.UniversityRepository;
import gg.agit.konect.user.dto.SignupRequest;
import gg.agit.konect.user.dto.UserInfoResponse;
import gg.agit.konect.user.dto.UserUpdateRequest;
import gg.agit.konect.user.model.UnRegisteredUser;
import gg.agit.konect.user.model.User;
import gg.agit.konect.user.repository.UnRegisteredUserRepository;
import gg.agit.konect.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final UnRegisteredUserRepository unRegisteredUserRepository;
    private final UniversityRepository universityRepository;

    @Transactional
    public User signup(String email, Provider provider, SignupRequest request) {
        userRepository.findByEmailAndProvider(email, provider)
            .ifPresent(u -> {
                throw CustomException.of(ApiResponseCode.ALREADY_REGISTERED_USER);
            });

        UnRegisteredUser tempUser = unRegisteredUserRepository
            .findByEmailAndProvider(email, provider)
            .orElseThrow(() -> CustomException.of(ApiResponseCode.NOT_FOUND_UNREGISTERED_USER));

        University university = universityRepository.findById(request.universityId())
            .orElseThrow(() -> CustomException.of(ApiResponseCode.UNIVERSITY_NOT_FOUND));

        User newUser = User.builder()
            .university(university)
            .email(tempUser.getEmail())
            .name(request.name())
            .studentNumber(request.studentNumber())
            .provider(tempUser.getProvider())
            .isMarketingAgreement(request.isMarketingAgreement())
            .build();

        User savedUser = userRepository.save(newUser);

        unRegisteredUserRepository.delete(tempUser);

        return savedUser;
    }

    public UserInfoResponse getUserInfo(Integer userId) {
        User user = userRepository.getById(userId);

        return UserInfoResponse.from(user);
    }

    @Transactional
    public void updateUserInfo(Integer userId, UserUpdateRequest request) {
        User user = userRepository.getById(userId);

        user.updateInfo(request.name(), request.studentNumber(), request.phoneNumber());
    }
}
