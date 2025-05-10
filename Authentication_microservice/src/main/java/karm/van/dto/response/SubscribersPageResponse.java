package karm.van.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Optional;

public record SubscribersPageResponse(
        @Schema(description = "List of subscribers (users who are followed by the specified user)")
        List<SubscriberDto> users,

        @Schema(description = "Indicates if this is the last page of results")
        boolean last,

        @Schema(description = "Total number of pages")
        int totalPages,

        @Schema(description = "Total number of subscribers")
        long totalElements,

        @Schema(description = "Indicates if this is the first page of results")
        boolean first,

        @Schema(description = "Number of elements in the current page")
        int numberOfElements
) {
}
