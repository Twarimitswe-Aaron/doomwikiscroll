package com.doomscroll.wik.service.social;

import com.doomscroll.wik.dto.request.InteractionRequest;
import com.doomscroll.wik.entity.*;
import com.doomscroll.wik.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SocialService {

    private final HistoricalEventRepository eventRepository;
    private final UserReactionRepository reactionRepository;
    private final BookmarkRepository bookmarkRepository;
    private final ReadingHistoryRepository readingHistoryRepository;
    private final UserFollowRepository userFollowRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;

    @Transactional
    public void recordInteraction(UUID userId, InteractionRequest request) {
        HistoricalEvent event = eventRepository.findById(request.getEventId())
                .orElseThrow(() -> new RuntimeException("Event not found"));

        String type = request.getInteractionType().toUpperCase();

        if ("VIEW".equals(type)) {
            event.setViewCount(event.getViewCount() + 1);
            eventRepository.save(event);

            if (userId != null) {
                User user = userRepository.findById(userId).orElse(null);
                if (user != null) {
                    ReadingHistory history = readingHistoryRepository.findByUserIdAndEventId(userId, event.getId())
                            .orElse(ReadingHistory.builder()
                                    .user(user)
                                    .event(event)
                                    .build());
                    history.setProgressPercentage(100);
                    history.setCompleted(true);
                    history.setLastReadAt(LocalDateTime.now());
                    readingHistoryRepository.save(history);
                }
            }
        } else if ("LIKE".equals(type)) {
            if (userId != null) {
                User user = userRepository.findById(userId).orElse(null);
                if (user != null) {
                    Optional<UserReaction> existingReaction = reactionRepository.findByUserIdAndEventIdAndReactionType(userId, event.getId(), "LIKE");
                    if (existingReaction.isPresent()) {
                        // Toggle off
                        reactionRepository.delete(existingReaction.get());
                        if (event.getLikeCount() > 0) {
                            event.setLikeCount(event.getLikeCount() - 1);
                        }
                    } else {
                        // Save new reaction
                        UserReaction reaction = UserReaction.builder()
                                .user(user)
                                .event(event)
                                .reactionType("LIKE")
                                .build();
                        reactionRepository.save(reaction);
                        event.setLikeCount(event.getLikeCount() + 1);
                    }
                    eventRepository.save(event);
                }
            } else {
                // Anonymous like
                event.setLikeCount(event.getLikeCount() + 1);
                eventRepository.save(event);
            }
        } else if ("SHARE".equals(type)) {
            event.setShareCount(event.getShareCount() + 1);
            eventRepository.save(event);
        }
    }

    @Transactional
    public boolean toggleBookmark(UUID userId, UUID eventId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        HistoricalEvent event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));

        Optional<Bookmark> bookmarkOpt = bookmarkRepository.findByUserIdAndEventId(userId, eventId);
        if (bookmarkOpt.isPresent()) {
            bookmarkRepository.delete(bookmarkOpt.get());
            return false;
        } else {
            Bookmark bookmark = Bookmark.builder()
                    .user(user)
                    .event(event)
                    .build();
            bookmarkRepository.save(bookmark);
            return true;
        }
    }

    @Transactional
    public boolean toggleFollowCategory(UUID userId, UUID categoryId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Category not found"));

        Optional<UserFollow> followOpt = userFollowRepository.findByUserIdAndCategoryId(userId, categoryId);
        if (followOpt.isPresent()) {
            userFollowRepository.delete(followOpt.get());
            return false;
        } else {
            UserFollow follow = UserFollow.builder()
                    .user(user)
                    .category(category)
                    .build();
            userFollowRepository.save(follow);
            return true;
        }
    }
}
