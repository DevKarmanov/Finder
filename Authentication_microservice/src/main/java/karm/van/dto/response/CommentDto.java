package karm.van.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record CommentDto(Long id, String text, LocalDateTime createdAt, Long cardId, CommentDto parentComment,
                         List<CommentDto> replyComments,Long userId) {
}
