package karm.van.dto.complaint;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Paginated complaint list response")
public record ComplaintPageResponseDto(
        @Schema(description = "List of complaints (can be of different types)")
        List<AbstractComplaint> complaints,

        @Schema(description = "Is this the last page?", example = "false")
        boolean last,

        @Schema(description = "Total number of pages", example = "3")
        int totalPages,

        @Schema(description = "Total number of complaints", example = "12")
        long totalElements,

        @Schema(description = "Is this the first page?", example = "true")
        boolean first,

        @Schema(description = "Number of elements on this page", example = "5")
        int numberOfElements
) { }
