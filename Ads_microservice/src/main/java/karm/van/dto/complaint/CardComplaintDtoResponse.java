package karm.van.dto.complaint;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Schema(description = "Complaint against a card", allOf = AbstractComplaint.class)
@Getter
@Setter
public class CardComplaintDtoResponse extends AbstractComplaint {
    private Long cardId;
    public CardComplaintDtoResponse(Long cardId, String reason, String complaintAuthorName, Long complaintId) {
        super(complaintId,reason,complaintAuthorName);
        this.cardId = cardId;
    }
}
