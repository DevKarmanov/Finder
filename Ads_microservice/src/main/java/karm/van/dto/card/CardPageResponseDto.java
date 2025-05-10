package karm.van.dto.card;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Response containing a list of cards and pagination information")
public record CardPageResponseDto(
        @Schema(description = "List of cards matching the search query")
        List<FullCardDtoForOutput> cards,

        @Schema(description = "Indicates if this is the last page")
        boolean last,

        @Schema(description = "Total number of pages")
        int totalPages,

        @Schema(description = "Total number of elements")
        long totalElements,

        @Schema(description = "Indicates if this is the first page")
        boolean first,

        @Schema(description = "Number of elements on the current page")
        int numberOfElements
) {}
