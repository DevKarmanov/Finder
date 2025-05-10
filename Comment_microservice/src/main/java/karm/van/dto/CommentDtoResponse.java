package karm.van.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Response DTO representing a comment")
public record CommentDtoResponse(

        @Schema(description = "ID of the comment")
        Long commentId,

        @Schema(description = "Text of the comment")
        String text,

        @Schema(description = "Date and time when the comment was created")
        LocalDateTime createdAt,

        @Schema(description = "Author of the comment")
        CommentAuthorDto commentAuthorDto,

        @Schema(description = "Number of replies to the comment")
        int replyQuantity

) {}
