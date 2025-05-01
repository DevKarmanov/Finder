package karm.van.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.servlet.http.HttpServletRequest;
import karm.van.dto.request.RecoveryRequest;
import karm.van.dto.request.UserPatchRequest;
import karm.van.exception.*;
import karm.van.service.MyUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.nio.file.AccessDeniedException;
import java.util.Map;
import java.util.Optional;

@Log4j2
@RequiredArgsConstructor
@RestController
@CrossOrigin
@RequestMapping("/user")
public class UserController {
    private final MyUserService myUserService;

    @GetMapping("/get")
    public ResponseEntity<?> getUserDto(@RequestParam(name = "userId",required = false) Optional<Long> userId,
                                        @RequestHeader(name = "x-api-key") String apiKey){
        try {
            if(myUserService.checkApiKeyNotEquals(apiKey)){
                throw new InvalidApiKeyException("Access denied");
            }
            return ResponseEntity.ok(myUserService.getUser(SecurityContextHolder.getContext().getAuthentication(),userId));
        }catch (UsernameNotFoundException | InvalidApiKeyException e){
            return ResponseEntity.badRequest().body(e.getMessage());
        }catch (BadCredentialsException e){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        }
    }


    @GetMapping("/profile/{userName}")
    public ResponseEntity<?> getFullUserData(HttpServletRequest request,
                                             @PathVariable String userName) {
        try {
            return ResponseEntity.ok(myUserService.getFullUserData(request,userName));
        }catch (CardsNotGetedException | ImageNotGetedException | JsonProcessingException e){
            log.error("class: "+e.getClass()+" message: "+e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred on the server side");
        }
    }

    @PatchMapping("/patch")
    public ResponseEntity<?> patchUser(@RequestBody UserPatchRequest userPatchRequest){
        try {
            myUserService.patchUser(
                    SecurityContextHolder.getContext().getAuthentication(),
                    userPatchRequest
            );
            return ResponseEntity.ok("User successfully changed");
        }catch (IllegalArgumentException e){
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/toggle/favoriteCard/{cardId}")
    public ResponseEntity<?> toggleFavoriteCard(@PathVariable Long cardId){
        try {
            String message = myUserService.toggleFavoriteCard(SecurityContextHolder.getContext().getAuthentication(),cardId);
            return ResponseEntity.ok(message);
        }catch (UsernameNotFoundException e){
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/favoriteCard/get")
    public ResponseEntity<?> getUserFavoriteCards(){
        try {
            return ResponseEntity.ok(myUserService.getUserFavoriteCards(SecurityContextHolder.
                    getContext().getAuthentication()));
        }catch (UsernameNotFoundException e){
            return ResponseEntity.badRequest().body(e.getMessage());
        }catch (JsonProcessingException e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @GetMapping("/recovery/mail")
    public ResponseEntity<?> recoveryMail(@RequestBody RecoveryRequest request){
        try {
            myUserService.getRecoveryMail(request);
            return ResponseEntity.ok("The message has been sent to your email: "+request.email());
        }catch (Exception e){
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/addCard/{cardId}")
    public ResponseEntity<?> addCardToUser(@PathVariable("cardId") Long cardId,
                                           @RequestHeader("x-api-key") String apiKey){
        try {
            if(myUserService.checkApiKeyNotEquals(apiKey)){
                throw new InvalidApiKeyException("Access denied");
            }
            myUserService.addCardToUser(SecurityContextHolder.getContext().getAuthentication(),cardId);
            return ResponseEntity.ok("Card added successfully");
        }catch (UsernameNotFoundException | InvalidApiKeyException e){
            return ResponseEntity.badRequest().body(Map.of("error",e.getMessage()));
        }catch (BadCredentialsException e){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", e.getMessage()));
        }
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @PatchMapping("/block/{userName}")
    public ResponseEntity<?> blockUser(@PathVariable String userName,
                                       @RequestParam int year,
                                       @RequestParam int month,
                                       @RequestParam int dayOfMonth,
                                       @RequestParam int hours,
                                       @RequestParam int minutes,
                                       @RequestParam int seconds,
                                       @RequestParam String reason){
        try {

            myUserService.blockUser(userName,year,month,dayOfMonth,hours,minutes,seconds,reason);
            return ResponseEntity.ok("user successfully blocked");
        }catch (UsernameNotFoundException e){
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @PatchMapping("/unblock/{userName}")
    public ResponseEntity<?> unblockUser(@PathVariable String userName){
        try {
            myUserService.unblockUser(userName);
            return ResponseEntity.ok("user successfully unblocked");
        }catch (UsernameNotFoundException e){
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @PatchMapping("/toggle/authorities/{userName}")
    public ResponseEntity<?> toggleUserAuthorities(@PathVariable String userName){
        try {
            String message = myUserService.toggleUserAuthorities(userName);
            return ResponseEntity.ok(message);
        }catch (UsernameNotFoundException | AccessDeniedException e){
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PatchMapping("/addProfileImage/{profileImageId}")
    public ResponseEntity<?> addProfileImage(@PathVariable("profileImageId") Long profileImageId,
                                             @RequestHeader("x-api-key") String apiKey){
        try {
            if(myUserService.checkApiKeyNotEquals(apiKey)){
                throw new InvalidApiKeyException("Access denied");
            }
            return ResponseEntity.ok(myUserService.addProfileImage(SecurityContextHolder.getContext().getAuthentication(),profileImageId));
        }catch (UsernameNotFoundException | InvalidApiKeyException e){
            return ResponseEntity.badRequest().body(Map.of("error",e.getMessage()));
        }catch (BadCredentialsException e){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/addComment/{commentId}")
    public ResponseEntity<?> addCommentToUser(@PathVariable("commentId") Long commentId,
                                              @RequestHeader("x-api-key") String apiKey){
        try {
            if(myUserService.checkApiKeyNotEquals(apiKey)){
                throw new InvalidApiKeyException("Access denied");
            }
            myUserService.addCommentToUser(SecurityContextHolder.getContext().getAuthentication(),commentId);
            return ResponseEntity.ok("Comment added successfully");
        }catch (UsernameNotFoundException | InvalidApiKeyException e){
            return ResponseEntity.badRequest().body(Map.of("error",e.getMessage()));
        }catch (BadCredentialsException e){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/comment/del/{commentId}/{authorId}")
    public ResponseEntity<?> unlinkCommentAndUser(@PathVariable("commentId") Long commentId,
                                                  @PathVariable("authorId") Long authorId,
                                                  @RequestHeader("x-api-key") String apiKey){
        try {
            if(myUserService.checkApiKeyNotEquals(apiKey)){
                throw new InvalidApiKeyException("Access denied");
            }
            myUserService.unlinkCommentAndUser(commentId,authorId);
            return ResponseEntity.ok("Comment deleted successfully");
        }catch (UsernameNotFoundException | InvalidApiKeyException e){
            return ResponseEntity.badRequest().body(Map.of("error",e.getMessage()));
        }catch (BadCredentialsException e){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/del")
    public ResponseEntity<?> delUser(HttpServletRequest request) throws BadCredentialsException{

        try {
            myUserService.delUser(SecurityContextHolder.getContext().getAuthentication(),request);
            return ResponseEntity.ok("User deleted successfully");
        } catch (BadCredentialsException e){
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (CardNotDeletedException | ImageNotMovedException | ImageNotDeletedException | ComplaintsNotDeletedException e){
            log.error("class: "+e.getClass()+" message: "+e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Due to an internal error, your account was not deleted");
        }
    }

    @DeleteMapping("/card/del/{cardId}")
    public ResponseEntity<?> delUserCard(@PathVariable("cardId") Long cardId,
                                         @RequestHeader("x-api-key") String apiKey){
        try {
            if(myUserService.checkApiKeyNotEquals(apiKey)){
                throw new InvalidApiKeyException("Access denied");
            }

            myUserService.delUserCard(SecurityContextHolder.getContext().getAuthentication(),cardId);
            return ResponseEntity.ok("Card deleted successfully");
        } catch (InvalidApiKeyException e){
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", e.getMessage()));
        }
    }

}
