package karm.van.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Paginated response containing comments")
public record CommentsPageResponse(

        @Schema(description = "List of comments on the card")
        List<CommentDtoResponse> comments,

        @Schema(description = "Is this the last page?")
        boolean last,

        @Schema(description = "Total number of pages")
        int totalPages,

        @Schema(description = "Total number of comment elements")
        long totalElements,

        @Schema(description = "Is this the first page?")
        boolean first,

        @Schema(description = "Number of comments on the current page")
        int numberOfElements

) {}