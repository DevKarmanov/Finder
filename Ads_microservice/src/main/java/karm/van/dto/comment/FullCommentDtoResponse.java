package karm.van.dto.comment;

import java.time.LocalDateTime;
import java.util.List;

public record FullCommentDtoResponse(Long id, String text, LocalDateTime createdAt, Long cardId, FullCommentDtoResponse parentComment,
                                     List<FullCommentDtoResponse> replyComments, Long userId) {
}
