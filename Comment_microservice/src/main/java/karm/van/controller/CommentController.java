package karm.van.controller;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import karm.van.dto.CommentDto;
import karm.van.dto.CommentsPageResponse;
import karm.van.dto.FullCommentDtoResponse;
import karm.van.exception.card.CardNotFoundException;
import karm.van.exception.comment.CommentNotFoundException;
import karm.van.exception.comment.CommentNotUnlinkException;
import karm.van.exception.other.InvalidDataException;
import karm.van.exception.other.SerializationException;
import karm.van.exception.token.InvalidApiKeyException;
import karm.van.exception.token.TokenNotExistException;
import karm.van.exception.user.NotEnoughPermissionsException;
import karm.van.exception.user.UsernameNotFoundException;
import karm.van.service.CommentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Log4j2
@RequiredArgsConstructor
@RestController
@CrossOrigin
@RequestMapping("/comment")
public class CommentController {
    private final CommentService commentService;

    @Operation(
            summary = "Add a new comment to a card",
            description = "Adds a new comment to the card identified by the given card ID.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = "multipart/form-data",
                            schema = @Schema(implementation = CommentDto.class),
                            examples = @ExampleObject(value = """
                {
                  "text": "This is a comment"
                }
            """)
                    )
            ),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Comment added successfully"),
                    @ApiResponse(responseCode = "401", description = "Invalid or missing token"),
                    @ApiResponse(responseCode = "400", description = "Invalid card ID, bad input data, or unknown error")
            }
    )
    @Parameter(
            name = "Authorization",
            in = ParameterIn.HEADER,
            required = true,
            description = "JWT token in the format: Bearer &lt;token&gt;"
    )
    @PostMapping("/add/{cardId}")
    public ResponseEntity<?> addComment(@PathVariable Long cardId,
                                        @RequestPart("commentDto") CommentDto commentDto,
                                        @RequestHeader("Authorization") String authorization) {
        try {
            commentService.addComment(cardId, commentDto, authorization);
            return ResponseEntity.ok("Comment added successfully");
        } catch (TokenNotExistException e){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        } catch (InvalidDataException | CardNotFoundException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            log.error("class: "+e.getClass()+" message: "+e.getMessage());
            return ResponseEntity.badRequest().body("An unknown error occurred while adding comment");
        }
    }

    @Hidden
    @PostMapping("/add/all")
    public ResponseEntity<?> addComments(@RequestBody List<FullCommentDtoResponse> deletedComments,
                                         @RequestHeader("x-api-key") String key) {
        try {
            if (commentService.checkNoneEqualsApiKey(key)){
                throw new InvalidApiKeyException("Invalid api-key");
            }
            commentService.addComments(deletedComments);
            return ResponseEntity.ok("Comment added successfully");
        } catch (Exception e) {
            log.error("class: "+e.getClass()+" message: "+e.getMessage());
            return ResponseEntity.badRequest().body("An unknown error occurred while adding comment");
        }
    }

    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = CommentDto.class),
                    examples = @ExampleObject(value = """
            {
                "text": "This is a reply"
            }
            """)
            )
    )
    @Operation(
            summary = "Reply to a comment",
            description = "Adds a reply to a specific comment by its ID.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Comment added successfully"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized: If the token does not exist or is invalid"),
                    @ApiResponse(responseCode = "400", description = "Bad Request: If the data entered is invalid"),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error: If an unknown error occurs while adding the reply")
            }
    )
    @Parameter(
            name = "Authorization",
            in = ParameterIn.HEADER,
            required = true,
            description = "JWT token in the format: Bearer &lt;token&gt;"
    )
    @PostMapping("/reply/{commentId}")
    public ResponseEntity<?> replyComment(@Parameter(description = "The ID of the comment to which the reply will be added")  @PathVariable Long commentId,
                                        @RequestPart("commentDto") CommentDto commentDto,
                                        @RequestHeader("Authorization") String authorization) {
        try {
            commentService.replyComment(commentId, commentDto, authorization);
            return ResponseEntity.ok("Comment added successfully");
        } catch (TokenNotExistException e){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        } catch (InvalidDataException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            log.error("class: "+e.getClass()+" message: "+e.getMessage());
            return ResponseEntity.internalServerError().body("An unknown error occurred while adding comment");
        }
    }

    @GetMapping("/get/{cardId}")
    @Operation(
            summary = "Get paginated comments for a card",
            description = "Returns a paginated list of comments for a specific card by its ID."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "If the reply is added successfully",
                    content = @Content(schema = @Schema(implementation = CommentsPageResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing token"),
            @ApiResponse(responseCode = "500", description = "If the comment is not found, if the entered data is incorrect, or if an unknown error occurs")
    })
    @Parameter(
            name = "Authorization",
            in = ParameterIn.HEADER,
            required = true,
            description = "JWT token in the format: Bearer &lt;token&gt;"
    )
    public ResponseEntity<?> getComments(
            @Parameter(description = "ID of the card for which to retrieve comments")
            @PathVariable Long cardId,

            @Parameter(description = "Page number of the comments pagination", example = "0")
            @RequestParam(required = false, defaultValue = "0") int page,

            @Parameter(description = "Number of comments per page", example = "10")
            @RequestParam(required = false, defaultValue = "10") int limit,

            @RequestHeader("Authorization") String authorization
    ) {
        try {
            return ResponseEntity.ok(commentService.getComments(cardId,limit,page,authorization));
        } catch (TokenNotExistException e){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        } catch (CardNotFoundException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (SerializationException e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }catch (Exception e){
            return ResponseEntity.badRequest().body("An unknown error occurred while deleting one comment: "+e.getMessage()+" - "+e.getClass());
        }
    }

    @GetMapping("/reply/get/{commentId}")
    @Operation(
            summary = "Get reply comments for a specific comment",
            description = "Returns the list of replies for the specified comment, paginated."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Replies successfully retrieved"),
            @ApiResponse(responseCode = "401", description = "Unauthorized: Invalid or missing token"),
            @ApiResponse(responseCode = "400", description = "Bad request: Comment not found"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error: An unknown error occurred")
    })
    @Parameter(
            name = "Authorization",
            in = ParameterIn.HEADER,
            required = true,
            description = "JWT token in the format: Bearer &lt;token&gt;"
    )
    public ResponseEntity<?> getReplyComments(
            @Parameter(description = "The ID of the comment to get replies for")
            @PathVariable Long commentId,

            @Parameter(description = "The page number for pagination (default is 0)")
            @RequestParam(required = false, defaultValue = "0") int page,

            @Parameter(description = "The number of replies per page (default is 10)")
            @RequestParam(required = false, defaultValue = "10") int limit,

            @RequestHeader("Authorization") String authorization) {
        try {
            return ResponseEntity.ok(commentService.getReplyComments(commentId,limit,page,authorization));
        } catch (TokenNotExistException e){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        } catch (CommentNotFoundException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (SerializationException e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }catch (Exception e){
            return ResponseEntity.internalServerError().body("An unknown error occurred while deleting one comment: "+e.getMessage()+" - "+e.getClass());
        }
    }

    @Hidden
    @DeleteMapping("/delAll/{cardId}")
    public ResponseEntity<?> deleteCommentsByCard(@PathVariable Long cardId,
                                     @RequestHeader("Authorization") String authorization,
                                     @RequestHeader("x-api-key") String key) {
        try {
            if (commentService.checkNoneEqualsApiKey(key)){
                throw new InvalidApiKeyException("Invalid api-key");
            }
            return ResponseEntity.ok(commentService.deleteAllCommentsByCard(cardId, authorization));
        } catch (TokenNotExistException | InvalidApiKeyException e){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        }catch (Exception e){
            log.error("An unknown error occurred while deleting comments by card: "+e.getMessage()+" - "+e.getClass());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @Hidden
    @DeleteMapping("/delAll/byUser/{userId}")
    public ResponseEntity<?> deleteCommentsByUser(@PathVariable Long userId,
                                     @RequestHeader("Authorization") String authorization,
                                     @RequestHeader("x-api-key") String key) throws TokenNotExistException, InvalidApiKeyException {
        try {
            if (commentService.checkNoneEqualsApiKey(key)){
                throw new InvalidApiKeyException("Invalid api-key");
            }
            return ResponseEntity.ok(commentService.deleteAllCommentsByUser(userId, authorization));
        } catch (TokenNotExistException | InvalidApiKeyException e){
            throw e;
        }catch (Exception e){
            log.error("An unknown error occurred while deleting comments by card: "+e.getMessage()+" - "+e.getClass());
            throw new RuntimeException("Unexpected error occurred", e);
        }
    }

    @Operation(
            summary = "Delete a specific comment by its unique ID",
            description = "Deletes the comment with the given ID if the user has the necessary permissions."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully deleted the comment"),
            @ApiResponse(responseCode = "400", description = "Bad request: Comment not found or insufficient permissions"),
            @ApiResponse(responseCode = "401", description = "Unauthorized: Invalid or missing token"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error: An error occurred while deleting the comment")
    })
    @Parameter(
            name = "Authorization",
            in = ParameterIn.HEADER,
            required = true,
            description = "JWT token in the format: Bearer &lt;token&gt;"
    )
    @DeleteMapping("/del/{commentId}")
    public ResponseEntity<?> deleteOneComment(@Parameter(description = "The ID of the comment to delete") @PathVariable Long commentId,
                                              @RequestHeader("Authorization") String authorization){
        try {
            commentService.deleteOneComment(commentId, authorization);
            return ResponseEntity.ok("Comment deleted successfully");
        } catch (CommentNotFoundException | UsernameNotFoundException |
                 NotEnoughPermissionsException e){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (TokenNotExistException e){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        } catch (CommentNotUnlinkException e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        } catch (Exception e){
            log.error("An unknown error occurred while deleting comments by card: "+e.getMessage()+" - "+e.getClass());
            return ResponseEntity.badRequest().body("An unknown error occurred while deleting one comment: "+e.getMessage()+" - "+e.getClass());
        }
    }

    @Operation(
            summary = "Patch (update) an existing comment",
            description = "Updates the comment with the given ID using the provided data."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully patched the comment"),
            @ApiResponse(responseCode = "400", description = "Bad request: Comment not found or invalid data"),
            @ApiResponse(responseCode = "401", description = "Unauthorized: Invalid or missing token"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error: An error occurred during the patching process")
    })
    @Parameter(
            name = "Authorization",
            in = ParameterIn.HEADER,
            required = true,
            description = "JWT token in the format: Bearer &lt;token&gt;"
    )
    @PatchMapping("/{commentId}/patch")
    public ResponseEntity<?> patchComment(
            @Parameter(description = "The ID of the comment to patch")
            @PathVariable Long commentId,

            @Parameter(description = "The data to update the comment with")
            @RequestBody CommentDto commentDto,

            @RequestHeader("Authorization") String authorization) {
        try {
            commentService.patchComment(commentId, commentDto, authorization);
            return ResponseEntity.ok("Comment patched successfully");
        } catch (TokenNotExistException e){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        }catch (InvalidDataException | CommentNotFoundException e){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }catch (Exception e){
            return ResponseEntity.badRequest().body("An unknown error occurred while deleting one comment: "+e.getMessage()+" - "+e.getClass());
        }
    }
}
