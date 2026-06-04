package com.doomscroll.wik.mapper;

import com.doomscroll.wik.dto.EventDto;
import com.doomscroll.wik.entity.Category;
import com.doomscroll.wik.entity.HistoricalEvent;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class EventMapper {

    public EventDto toDto(HistoricalEvent event) {
        if (event == null) {
            return null;
        }
        
        return EventDto.builder()
                .id(event.getId())
                .title(event.getTitle())
                .summary(event.getSummary())
                .detailedContent(event.getDetailedContent())
                .eventDate(event.getEventDate())
                .eventYear(event.getEventYear())
                .era(event.getEra())
                .location(event.getLocation())
                .latitude(event.getLatitude())
                .longitude(event.getLongitude())
                .wikipediaPageId(event.getWikipediaPageId())
                .wikipediaUrl(event.getWikipediaUrl())
                .imageUrl(event.getImageUrl())
                .thumbnailUrl(event.getThumbnailUrl())
                .source(event.getSource())
                .viewCount(event.getViewCount())
                .likeCount(event.getLikeCount())
                .commentCount(event.getCommentCount())
                .shareCount(event.getShareCount())
                .isTrending(event.getIsTrending())
                .isFeatured(event.getIsFeatured())
                .categories(event.getCategories() != null 
                        ? event.getCategories().stream().map(Category::getName).collect(Collectors.toSet()) 
                        : null)
                .build();
    }
}
