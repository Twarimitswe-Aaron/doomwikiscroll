package com.doomscroll.wik.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InteractionRequest {

    @NotNull(message = "Event ID cannot be null")
    private UUID eventId;

    @NotNull(message = "Interaction type cannot be null")
    @Pattern(regexp = "^(VIEW|LIKE|SHARE)$", message = "Interaction type must be VIEW, LIKE, or SHARE")
    private String interactionType;
}
