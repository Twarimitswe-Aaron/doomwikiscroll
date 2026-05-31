package com.doomscroll.wik.service.user;

import com.doomscroll.wik.dto.EventDto;
import com.doomscroll.wik.dto.UserProfileDto;
import com.doomscroll.wik.dto.request.user.UpdateProfileRequest;
import com.doomscroll.wik.entity.Bookmark;
import com.doomscroll.wik.entity.User;
import com.doomscroll.wik.mapper.EventMapper;
import com.doomscroll.wik.repository.BookmarkRepository;
import com.doomscroll.wik.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final BookmarkRepository bookmarkRepository;
    private final EventMapper eventMapper;

    @Transactional(readOnly = true)
    public UserProfileDto getCurrentUserProfile(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return toDto(user);
    }

    @Transactional
    public UserProfileDto updateUserProfile(UUID userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (request.getFirstName() != null) {
            user.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            user.setLastName(request.getLastName());
        }
        if (request.getBio() != null) {
            user.setBio(request.getBio());
        }

        user = userRepository.save(user);
        return toDto(user);
    }

    @Transactional(readOnly = true)
    public Page<EventDto> getBookmarks(UUID userId, Pageable pageable) {
        return bookmarkRepository.findByUserId(userId, pageable)
                .map(Bookmark::getEvent)
                .map(eventMapper::toDto);
    }

    public UserProfileDto toDto(User user) {
        if (user == null) return null;
        return UserProfileDto.builder()
                .id(user.getId())
                .username(user.getDisplayUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .bio(user.getBio())
                .role(user.getRole().name())
                .status(user.getStatus().name())
                .emailVerified(user.getEmailVerified())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
