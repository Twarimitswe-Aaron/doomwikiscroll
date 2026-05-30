package com.doomscroll.wik.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventDto {
    private UUID id;
    private String title;
    private String summary;
    private String detailedContent;
    private String eventDate;
    private Integer eventYear;
    private String era;
    private String location;
    private Double latitude;
    private Double longitude;
    private String wikipediaUrl;
    private String imageUrl;
    private String thumbnailUrl;
    private String source;
    private Long viewCount;
    private Long likeCount;
    private Long commentCount;
    private Long shareCount;
    private Boolean isTrending;
    private Boolean isFeatured;
    private Set<String> categories;
}
