package karm.van.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import karm.van.exception.EmailNotFoundException;
import karm.van.service.MyUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("${microservices.auth.endpoints.recovery-password}")
@RequiredArgsConstructor
@CrossOrigin
public class PasswordRecoveryController {
    private final MyUserService myUserService;

    @Operation(
            summary = "Update password using recovery key",
            description = "This endpoint is used to update the password using a recovery key sent to the user's email as a link."
    )
    @Parameter(
            name = "recoveryKey",
            description = "Recovery key received by email, embedded in the link.",
            required = true
    )
    @ApiResponse(
            responseCode = "200",
            description = "Password successfully changed",
            content = @Content(
                    mediaType = "text/plain",
                    schema = @Schema(example = "The password has been successfully changed, you can close this window")
            )
    )
    @ApiResponse(
            responseCode = "404",
            description = "Invalid or expired recovery key",
            content = @Content(
                    mediaType = "text/plain",
                    schema = @Schema(example = "Email not found")
            )
    )
    @GetMapping("/{recoveryKey}")
    public ResponseEntity<?> updatePassword(@PathVariable String recoveryKey) {
        try {
            myUserService.updatePassword(recoveryKey);
            return ResponseEntity.ok("The password has been successfully changed, you can close this window");
        } catch (EmailNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }
}
