package com.doomscroll.wik.dto.response;

import com.doomscroll.wik.dto.EventDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedResponseDto {
    private List<EventDto> events;
    private int pageNumber;
    private int pageSize;
    private boolean hasNext;
    private long totalElements;
}
