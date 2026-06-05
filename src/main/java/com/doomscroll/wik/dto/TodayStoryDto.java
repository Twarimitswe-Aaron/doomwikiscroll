package com.doomscroll.wik.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * A lightweight story item for the "Today in History" stories bar.
 * Maps to a single Wikipedia "On This Day" event.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TodayStoryDto implements Serializable {
    /** Unique identifier (Wikipedia page ID or UUID) */
    private String id;
    /** Short headline/title */
    private String title;
    /** One-sentence extract */
    private String extract;
    /** Year the event occurred */
    private Integer year;
    /** Full-size image URL (for the story viewer) */
    private String imageUrl;
    /** Thumbnail URL (for the story bubble in the bar) */
    private String thumbnailUrl;
    /** Link back to Wikipedia */
    private String wikipediaUrl;
}
