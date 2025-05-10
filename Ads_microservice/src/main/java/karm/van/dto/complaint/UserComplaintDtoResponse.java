package karm.van.dto.complaint;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
@Schema(description = "Complaint against a user", allOf = AbstractComplaint.class)
@Getter
@Setter
public class UserComplaintDtoResponse extends AbstractComplaint {
    private String userName;
    public UserComplaintDtoResponse(String userName, String reason, String complaintAuthorName, Long complaintId) {
        super(complaintId, reason, complaintAuthorName);
        this.userName = userName;
    }
}
