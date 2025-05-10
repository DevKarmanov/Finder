package karm.van.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Setter
@Getter
@Builder
public class FullCommentDtoResponse {
    private Long id;
    private String text;
    private LocalDateTime createdAt;
    private Long cardId;
    private FullCommentDtoResponse parentComment;
    private List<FullCommentDtoResponse> replyComments;
    private Long userId;

    public FullCommentDtoResponse(Long id, String text, LocalDateTime createdAt, Long cardId,
                                  FullCommentDtoResponse parentComment, List<FullCommentDtoResponse> replyComments, Long userId) {
        this.id = id;
        this.text = text;
        this.createdAt = createdAt;
        this.cardId = cardId;
        this.parentComment = parentComment;
        this.replyComments = replyComments;
        this.userId = userId;
    }

}

