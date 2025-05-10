package karm.van.dto.complaint;

import io.swagger.v3.oas.annotations.media.Schema;

public record ComplaintDtoRequest(

        @Schema(description = "The type of the complaint target, e.g., User or Ad.")
        ComplaintType targetType,

        @Schema(description = "The reason for filing the complaint.")
        String reason,

        @Schema(description = "The ID of the user or ad being complained about.")
        Long complaintTargetId
) {
}
