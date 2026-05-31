package com.doomscroll.wik.dto.request.event;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventRequestDto {
    @NotBlank(message = "Title is required")
    @Size(max = 500, message = "Title cannot exceed 500 characters")
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
    private Set<String> categories;
}
