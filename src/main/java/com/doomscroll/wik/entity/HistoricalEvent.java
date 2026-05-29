package com.doomscroll.wik.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "historical_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HistoricalEvent extends BaseEntity {

    @Column(nullable = false, length = 500)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(name = "detailed_content", columnDefinition = "TEXT")
    private String detailedContent;

    @Column(name = "event_date", length = 50)
    private String eventDate;

    @Column(name = "event_year")
    private Integer eventYear;

    @Column(length = 50)
    private String era;

    @Column(length = 255)
    private String location;

    private Double latitude;
    private Double longitude;

    @Column(name = "wikipedia_url", length = 500)
    private String wikipediaUrl;

    @Column(name = "wikipedia_page_id", unique = true)
    private Long wikipediaPageId;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;

    @Column(length = 100)
    @Builder.Default
    private String source = "WIKIPEDIA";

    @Column(name = "view_count")
    @Builder.Default
    private Long viewCount = 0L;

    @Column(name = "like_count")
    @Builder.Default
    private Long likeCount = 0L;

    @Column(name = "comment_count")
    @Builder.Default
    private Long commentCount = 0L;

    @Column(name = "share_count")
    @Builder.Default
    private Long shareCount = 0L;

    @Column(name = "is_trending")
    @Builder.Default
    private Boolean isTrending = false;

    @Column(name = "is_featured")
    @Builder.Default
    private Boolean isFeatured = false;

    @Column(length = 20, nullable = false)
    @Builder.Default
    private String status = "PUBLISHED";

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "event_categories",
            joinColumns = @JoinColumn(name = "event_id"),
            inverseJoinColumns = @JoinColumn(name = "category_id")
    )
    @Builder.Default
    private Set<Category> categories = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "related_events",
            joinColumns = @JoinColumn(name = "event_id"),
            inverseJoinColumns = @JoinColumn(name = "related_event_id")
    )
    @Builder.Default
    private Set<HistoricalEvent> relatedEvents = new HashSet<>();
}
