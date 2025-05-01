package karm.van.controller;

import karm.van.dto.CommentDto;
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

@Log4j2
@RequiredArgsConstructor
@RestController
@CrossOrigin
@RequestMapping("/comment")
public class CommentController {
    private final CommentService commentService;
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

    @PostMapping("/reply/{commentId}")
    public ResponseEntity<?> replyComment(@PathVariable Long commentId,
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
    public ResponseEntity<?> getComments(@PathVariable Long cardId,
                                         @RequestParam(required = false,defaultValue = "0") int page,
                                         @RequestParam(required = false,defaultValue = "10") int limit,
                                         @RequestHeader("Authorization") String authorization) {
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
    public ResponseEntity<?> getReplyComments(@PathVariable Long commentId,
                                         @RequestParam(required = false,defaultValue = "0") int page,
                                         @RequestParam(required = false,defaultValue = "10") int limit,
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

    @DeleteMapping("/delAll/{cardId}")
    public void deleteCommentsByCard(@PathVariable Long cardId,
                                     @RequestHeader("Authorization") String authorization,
                                     @RequestHeader("x-api-key") String key) throws TokenNotExistException, InvalidApiKeyException {
        try {
            if (commentService.checkNoneEqualsApiKey(key)){
                throw new InvalidApiKeyException("Invalid api-key");
            }
            commentService.deleteAllCommentsByCard(cardId, authorization);
        } catch (TokenNotExistException | InvalidApiKeyException e){
            throw e;
        }catch (Exception e){
            log.error("An unknown error occurred while deleting comments by card: "+e.getMessage()+" - "+e.getClass());
            throw new RuntimeException("Unexpected error occurred", e);
        }
    }

    @DeleteMapping("/del/{commentId}")
    public ResponseEntity<?> deleteOneComment(@PathVariable Long commentId,
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

    @PatchMapping("/{commentId}/patch")
    public ResponseEntity<?> patchComment(@PathVariable Long commentId,
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
