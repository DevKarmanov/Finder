package karm.van.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Paginated response for users search")
public record UserPageResponse(
        @Schema(description = "List of users matching the search criteria") List<UserDtoForSearchResponse> users,
        @Schema(description = "Indicates if this is the last page", example = "false") boolean last,
        @Schema(description = "Total number of pages available", example = "5") int totalPages,
        @Schema(description = "Total number of elements available", example = "50") long totalElements,
        @Schema(description = "Indicates if this is the first page", example = "true") boolean first,
        @Schema(description = "Number of elements on the current page", example = "5") int numberOfElements
) {}
