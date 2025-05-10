package karm.van.dto.complaint;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Schema(
        description = "Base complaint model",
        discriminatorProperty = "type",
        oneOf = {UserComplaintDtoResponse.class, CardComplaintDtoResponse.class}
)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = UserComplaintDtoResponse.class, name = "user"),
        @JsonSubTypes.Type(value = CardComplaintDtoResponse.class, name = "card")
})
@Getter
@Setter
@AllArgsConstructor
public abstract class AbstractComplaint {
    @Schema(description = "Complaint ID", example = "5")
    private Long complaintId;

    @Schema(description = "Reason for the complaint", example = "Spam or abusive content")
    private String reason;

    @Schema(description = "Author's username", example = "venik6")
    private String complaintAuthorName;
}