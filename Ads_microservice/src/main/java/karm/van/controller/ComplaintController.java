package karm.van.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import karm.van.dto.complaint.ComplaintDtoRequest;
import karm.van.dto.complaint.ComplaintPageResponseDto;
import karm.van.exception.card.CardNotFoundException;
import karm.van.exception.other.SerializationException;
import karm.van.exception.other.TokenNotExistException;
import karm.van.exception.user.NotEnoughPermissionsException;
import karm.van.exception.user.UsernameNotFoundException;
import karm.van.service.ComplaintService;
import lombok.RequiredArgsConstructor;
import org.apache.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin
@RequestMapping("complaint")
@RequiredArgsConstructor
public class ComplaintController {
    private final ComplaintService complaintService;

    @Operation(
            summary = "Create a new complaint",
            description = "This endpoint allows users to create a new complaint about an ad or user. The user must be authenticated.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Complaint details",
                    content = @Content(
                            mediaType = "multipart/form-data",
                            schema = @Schema(implementation = ComplaintDtoRequest.class)
                    )
            ),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Complaint successfully sent"),
                    @ApiResponse(responseCode = "400", description = "Invalid token or request parameters"),
                    @ApiResponse(responseCode = "404", description = "User or card not found")
            }
    )
    @Parameter(
            name = "Authorization",
            in = ParameterIn.HEADER,
            required = true,
            description = "JWT token in the format: Bearer &lt;token&gt;"
    )
    @PostMapping(value = "/create", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> createComplaint(@RequestPart ComplaintDtoRequest complaintDto,
                                             @RequestHeader("Authorization") String authorization) {
        try {
            complaintService.createComplaint(authorization, complaintDto);
            return ResponseEntity.ok("Complaint successfully sent");
        } catch (TokenNotExistException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (UsernameNotFoundException | CardNotFoundException e) {
            return ResponseEntity.status(HttpStatus.SC_NOT_FOUND).body(e.getMessage());
        }
    }

    @Operation(
            summary = "Retrieve a list of complaints with optional filtering",
            description = "This endpoint retrieves a paginated list of complaints, with the option to filter by type (card or user).",
            parameters = {
                    @Parameter(name = "limit", description = "The maximum number of complaints to return", example = "5"),
                    @Parameter(name = "page", description = "The page number for pagination", example = "0"),
                    @Parameter(name = "complaintType", description = "The type of complaint to filter by (card/user/all)", example = "all")
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successfully retrieved the list of complaints",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ComplaintPageResponseDto.class))),
                    @ApiResponse(responseCode = "400", description = "Bad Request - Invalid or missing token"),
                    @ApiResponse(responseCode = "403", description = "Forbidden - Insufficient permissions"),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error - Failure during retrieval")
            }
    )
    @Parameter(
            name = "Authorization",
            in = ParameterIn.HEADER,
            required = true,
            description = "JWT token in the format: Bearer &lt;token&gt;"
    )
    @GetMapping("/get")
    public ResponseEntity<?> getComplaintList(
            @RequestParam(required = false, defaultValue = "5") int limit,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "all") String complaintType,
            @RequestHeader("Authorization") String authorization
    ) {
        try {
            return ResponseEntity.ok(complaintService.getComplaints(authorization, limit, page, complaintType));
        } catch (TokenNotExistException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (SerializationException | JsonProcessingException | UsernameNotFoundException e) {
            return ResponseEntity.status(HttpStatus.SC_INTERNAL_SERVER_ERROR).body(e.getMessage());
        } catch (NotEnoughPermissionsException e) {
            return ResponseEntity.status(HttpStatus.SC_FORBIDDEN).body(e.getMessage());
        }
    }

    @Hidden
    @DeleteMapping("/dellAllByUser/{userId}")
    public ResponseEntity<?> dellAllComplaintByUser(
                                        @RequestHeader("Authorization") String authorization,
                                        @RequestHeader("x-api-key") String apiKey,
                                        @PathVariable("userId") Long userId){
        try {
            complaintService.dellAllComplaintByUser(authorization,apiKey,userId);
            return ResponseEntity.ok("All complaints have been deleted");
        }catch (TokenNotExistException e){
            return ResponseEntity.badRequest().body(e.getMessage());
        }catch (Exception e){
            return ResponseEntity.status(HttpStatus.SC_INTERNAL_SERVER_ERROR).body(e.getMessage());
        }

    }

    @Operation(
            summary = "Delete a specific complaint",
            description = "Deletes a complaint by its unique ID. Only users with the required permissions can delete complaints.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successfully deleted the complaint"),
                    @ApiResponse(responseCode = "400", description = "Invalid or missing token"),
                    @ApiResponse(responseCode = "403", description = "Not enough permissions"),
                    @ApiResponse(responseCode = "404", description = "User not found")
            }
    )
    @Parameter(
            name = "Authorization",
            in = ParameterIn.HEADER,
            required = true,
            description = "JWT token in the format: Bearer &lt;token&gt;"
    )
    @DeleteMapping("delOne/{complaintId}")
    public ResponseEntity<?> dellOneComplaint(
            @RequestHeader("Authorization") String authorization,

            @Parameter(description = "The unique ID of the complaint to delete", required = true)
            @PathVariable("complaintId") Long complaintId
    ) {
        try {
            complaintService.delOneComplaint(authorization, complaintId);
            return ResponseEntity.ok("Complaint have been deleted");
        } catch (TokenNotExistException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (UsernameNotFoundException e) {
            return ResponseEntity.status(HttpStatus.SC_NOT_FOUND).body(e.getMessage());
        } catch (NotEnoughPermissionsException e) {
            return ResponseEntity.status(HttpStatus.SC_FORBIDDEN).body(e.getMessage());
        }
    }

}
