package karm.van.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.servlet.http.HttpServletRequest;
import karm.van.dto.request.RecoveryRequest;
import karm.van.dto.request.UserPatchRequest;
import karm.van.dto.response.FullUserDtoResponse;
import karm.van.dto.response.SubscribersPageResponse;
import karm.van.dto.response.UserPageResponse;
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

    @Hidden
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

    @Operation(
            summary = "Search user profiles",
            description = "Searches for user profiles based on optional filters like username, country, description, etc. Supports pagination."
    )
    @Parameter(
            name = "Authorization",
            in = ParameterIn.HEADER,
            required = true,
            description = "JWT token in the format: Bearer &lt;token&gt;"
    )
    @ApiResponse(
            responseCode = "200",
            description = "Search results",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = UserPageResponse.class)
            )
    )
    @ApiResponse(
            responseCode = "400",
            description = "Invalid request",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(example = "{\"error\": \"Invalid request\"}")
            )
    )
    @GetMapping("/profile/search")
    public ResponseEntity<?> searchProfile(
            @RequestParam(required = false, defaultValue = "0") @Parameter(description = "Page number for pagination") int pageNumber,
            @RequestParam(required = false, defaultValue = "5") @Parameter(description = "Number of results per page") int limit,
            @RequestParam(required = false, name = "username") @Parameter(description = "Search by login") Optional<String> usernameOpt,
            @RequestParam(required = false, name = "country") @Parameter(description = "Search by country") Optional<String> countryOpt,
            @RequestParam(required = false, name = "description") @Parameter(description = "Search by description") Optional<String> descriptionOpt,
            @RequestParam(required = false, name = "firstname") @Parameter(description = "Search by first name") Optional<String> firstnameOpt,
            @RequestParam(required = false, name = "lastname") @Parameter(description = "Search by last name") Optional<String> lastnameOpt,
            @RequestParam(required = false, name = "skills") @Parameter(description = "Search by skills") Optional<String> skillsOpt,
            @RequestParam(required = false, name = "roleInCommand") @Parameter(description = "Search by role in command") Optional<String> roleInCommandOpt
    ) {
        try {
            return ResponseEntity.ok(myUserService.searchUser(
                    usernameOpt, countryOpt, descriptionOpt, firstnameOpt, lastnameOpt, skillsOpt, roleInCommandOpt, pageNumber, limit
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }


    @Operation(
            summary = "Retrieve user details",
            description = "Fetches the full details of the user based on the provided username."
    )
    @Parameter(
            name = "Authorization",
            in = ParameterIn.HEADER,
            required = true,
            description = "JWT token in the format: Bearer &lt;token&gt;"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Successfully retrieved user details",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = FullUserDtoResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "An error occurred on the server side",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(example = "{\"error\": \"An error occurred on the server side\"}")
                    )
            )
    })
    @GetMapping("/profile/{userName}")
    public ResponseEntity<?> getFullUserData(HttpServletRequest request,
                                             @PathVariable String userName) {
        try {
            return ResponseEntity.ok(myUserService.getFullUserData(SecurityContextHolder.getContext().getAuthentication(), request, userName));
        } catch (CardsNotGetedException | ImageNotGetedException | JsonProcessingException e) {
            log.error("class: "+e.getClass()+" message: "+e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred on the server side");
        }
    }

    @Operation(
            summary = "Patch user information",
            description = "Updates the user information based on the provided fields. Only the fields passed in the request will be updated."
    )
    @Parameter(
            name = "Authorization",
            in = ParameterIn.HEADER,
            required = true,
            description = "JWT token in the format: Bearer &lt;token&gt;"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "User successfully updated",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(example = "\"User successfully changed\"")
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad Request if the input data is invalid or if required fields are missing",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(example = "\"Invalid argument\"")
                    )
            )
    })
    @PatchMapping("/patch")
    public ResponseEntity<?> patchUser(
            @Parameter(
                    description = "User information to be updated",
                    required = true,
                    schema = @Schema(implementation = UserPatchRequest.class)
            )
            @RequestBody UserPatchRequest userPatchRequest
    ) {
        try {
            myUserService.patchUser(
                    SecurityContextHolder.getContext().getAuthentication(),
                    userPatchRequest
            );
            return ResponseEntity.ok("User successfully changed");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @Operation(
            summary = "Toggle a card as favorite",
            description = "Adds a card to your favorites the first time you access it and removes it from there the second time you access it."
    )
    @Parameter(
            name = "Authorization",
            in = ParameterIn.HEADER,
            required = true,
            description = "JWT token in the format: Bearer &lt;token&gt;"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Successfully added or removed the card from favorites.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = String.class, example = "\"Card successfully added\"")
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad Request if the user is not found.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = String.class, example = "\"User with this name doesn't exist\"")
                    )
            )
    })
    @PostMapping("/toggle/favoriteCard/{cardId}")
    public ResponseEntity<?> toggleFavoriteCard(
            @Parameter(
                    description = "Unique identifier of the card",
                    required = true
            ) @PathVariable Long cardId
    ) {
        try {
            String message = myUserService.toggleFavoriteCard(SecurityContextHolder.getContext().getAuthentication(), cardId);
            return ResponseEntity.ok(message);
        } catch (UsernameNotFoundException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @Operation(
            summary = "Subscribe or unsubscribe from a user",
            description = "Subscribes to a user if not already following, or unsubscribes if already following the user."
    )
    @Parameter(
            name = "Authorization",
            in = ParameterIn.HEADER,
            required = true,
            description = "JWT token in the format: Bearer &lt;token&gt;"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Successfully subscribed or unsubscribed from the user.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = String.class, example = "\"You have successfully subscribed to this user.\"")
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad Request if the user doesn't exist or tries to subscribe to themselves.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = String.class, example = "\"User with this name doesn't exist\"")
                    )
            )
    })
    @PostMapping("/toggle/following/{userName}")
    public ResponseEntity<?> toggleFollowing(
            @Parameter(
                    description = "Username of the user to subscribe or unsubscribe from.",
                    required = true
            ) @PathVariable String userName
    ) {
        try {
            String action = myUserService.toggleFollowing(SecurityContextHolder.getContext().getAuthentication(), userName);
            return ResponseEntity.ok("You have successfully " + action + " to this user.");
        } catch (UsernameNotFoundException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @Operation(
            summary = "Get all subscriptions of a user",
            description = "Retrieves all users that a given user is following. Supports pagination."
    )
    @Parameter(
            name = "Authorization",
            in = ParameterIn.HEADER,
            required = true,
            description = "JWT token in the format: Bearer &lt;token&gt;"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Successfully retrieved user subscriptions",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = SubscribersPageResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad Request if the user does not exist",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = String.class, example = "\"User with this name doesn't exist\"")
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal Server Error if there is a server-side error",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = String.class, example = "\"Internal server error\"")
                    )
            )
    })
    @GetMapping("/following/{userName}")
    public ResponseEntity<?> getUserSubscriptions(
            @Parameter(
                    description = "Username of the user whose subscriptions you want to retrieve",
                    required = true
            ) @PathVariable String userName,

            @Parameter(
                    description = "Page number for pagination, defaults to 0"
            ) @RequestParam(required = false, defaultValue = "0") int pageNumber,

            @Parameter(
                    description = "Number of subscriptions to retrieve per page, defaults to 5"
            ) @RequestParam(required = false, defaultValue = "5") int limit,
            HttpServletRequest  request){
        try {
            return ResponseEntity.ok(myUserService.getUserSubscriptions(userName,pageNumber,limit,request));
        }catch (UsernameNotFoundException e){
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (SerializationException | ImageNotGetedException e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    @Operation(
            summary = "Get user's subscribers",
            description = "Retrieve a list of users who are following the given user.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successfully retrieved the list of subscribers", content = @Content(mediaType = "application/json", schema = @Schema(implementation = SubscribersPageResponse.class))),
                    @ApiResponse(responseCode = "400", description = "User not found", content = @Content(mediaType = "application/json")),
                    @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(mediaType = "application/json"))
            }
    )
    @Parameter(
            name = "Authorization",
            in = ParameterIn.HEADER,
            required = true,
            description = "JWT token in the format: Bearer &lt;token&gt;"
    )
    @GetMapping("/followers/{userName}")
    public ResponseEntity<?> getUserSubscribers(HttpServletRequest request,
                                                @PathVariable String userName,
                                                  @RequestParam(required = false,defaultValue = "0") int pageNumber,
                                                  @RequestParam(required = false,defaultValue = "5") int limit){
        try {
            return ResponseEntity.ok(myUserService.getUserSubscribers(userName,pageNumber,limit,request));
        }catch (UsernameNotFoundException e){
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (SerializationException | ImageNotGetedException e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    @Operation(
            summary = "Get user's favorite cards",
            description = "Returns a list of advertisement (card) IDs that the currently authenticated user has marked as favorites.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successfully retrieved list of favorite card IDs",
                            content = @Content(
                                    mediaType = "application/json",
                                    array = @ArraySchema(schema = @Schema(type = "integer", format = "int64"))
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "User not found",
                            content = @Content(mediaType = "application/json")
                    ),
                    @ApiResponse(
                            responseCode = "500",
                            description = "Internal server error (e.g., serialization failure)",
                            content = @Content(mediaType = "application/json")
                    )
            }
    )
    @Parameter(
            name = "Authorization",
            in = ParameterIn.HEADER,
            required = true,
            description = "JWT token in the format: Bearer &lt;token&gt;"
    )
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

    @Operation(
            summary = "Send password recovery email",
            description = "Generates and sends an email with a link to change your password."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "The letter was successfully sent.",
                    content = @Content(schema = @Schema(type = "string"))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "If there is a server error.",
                    content = @Content(schema = @Schema(type = "string"))
            )
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = RecoveryRequest.class),
                    examples = @ExampleObject(value = """
        {
          "email": "email@example.com",
          "password": "new_password"
        }
        """)
            )
    )
    @PostMapping("/recovery/mail")
    public ResponseEntity<?> recoveryMail(
            @RequestBody RecoveryRequest request
    ) {
        try {
            myUserService.getRecoveryMail(request);
            return ResponseEntity.ok("The message has been sent to your email: " + request.email());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @Hidden
    @PostMapping("/addCard/{cardId}/{userId}")
    public ResponseEntity<?> addCardToUser(@PathVariable("cardId") Long cardId,
                                           @PathVariable("userId") Long userId,
                                           @RequestHeader("x-api-key") String apiKey){
        try {
            if(myUserService.checkApiKeyNotEquals(apiKey)){
                throw new InvalidApiKeyException("Access denied");
            }
            myUserService.addCardToUser(userId,cardId);
            return ResponseEntity.ok("Card added successfully");
        }catch (UsernameNotFoundException | InvalidApiKeyException e){
            return ResponseEntity.badRequest().body(Map.of("error",e.getMessage()));
        }catch (BadCredentialsException e){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", e.getMessage()));
        }
    }

    @Operation(
            summary = "Block a user",
            description = "Blocks the user with the specified username until the given date and time.",
            parameters = {
                    @Parameter(name = "userName", description = "The username of the user to be blocked", required = true),
                    @Parameter(name = "year", description = "Year when the user will be unlocked", required = true),
                    @Parameter(name = "month", description = "Month when the user will be unlocked (1–12)", required = true),
                    @Parameter(name = "dayOfMonth", description = "Day of the month when the user will be unlocked", required = true),
                    @Parameter(name = "hours", description = "Hour of the day (0–23)", required = true),
                    @Parameter(name = "minutes", description = "Minute (0–59)", required = true),
                    @Parameter(name = "seconds", description = "Second (0–59)", required = true),
                    @Parameter(name = "reason", description = "Reason for blocking the user", required = true)
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "User successfully blocked"),
                    @ApiResponse(responseCode = "400", description = "User not found or invalid input")
            }
    )
    @Parameter(
            name = "Authorization",
            in = ParameterIn.HEADER,
            required = true,
            description = "JWT token in the format: Bearer &lt;token&gt;"
    )
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

    @Operation(
            summary = "Unblock a user",
            description = "Unblocks the user with the specified username.",
            parameters = {
                    @Parameter(name = "userName", description = "The username of the user to be unblocked", required = true)
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "User successfully unblocked"),
                    @ApiResponse(responseCode = "400", description = "User not found")
            }
    )
    @Parameter(
            name = "Authorization",
            in = ParameterIn.HEADER,
            required = true,
            description = "JWT token in the format: Bearer &lt;token&gt;"
    )
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

    @Operation(
            summary = "Toggle ADMIN role for a user",
            description = "Adds the ADMIN role to a user if not present; removes it if already assigned.",
            parameters = {
                    @Parameter(name = "userName", description = "The username for which to change roles", required = true)
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successfully toggled the user's roles"),
                    @ApiResponse(responseCode = "400", description = "User not found or insufficient permissions")
            }
    )
    @Parameter(
            name = "Authorization",
            in = ParameterIn.HEADER,
            required = true,
            description = "JWT token in the format: Bearer &lt;token&gt;"
    )
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

    @Hidden
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

    @Hidden
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

    @Hidden
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

    @Hidden
    @DeleteMapping("/card/favorite/unlink/{cardId}")
    public ResponseEntity<?> unlinkFavoriteCardFromAllUsers(@PathVariable("cardId") Long cardId,
                                               @RequestHeader("x-api-key") String apiKey){
        try {
            if(myUserService.checkApiKeyNotEquals(apiKey)){
                throw new InvalidApiKeyException("Access denied");
            }
            myUserService.unlinkFavoriteCardFromAllUsers(cardId);
            return ResponseEntity.ok("Cards deleted from favorite successfully");
        }catch (UsernameNotFoundException | InvalidApiKeyException e){
            return ResponseEntity.badRequest().body(Map.of("error",e.getMessage()));
        }catch (BadCredentialsException e){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", e.getMessage()));
        }
    }

    @Operation(
            summary = "Delete the current user",
            description = "Deletes the authenticated user and all related data (images, complaints, comments).",
            responses = {
                    @ApiResponse(responseCode = "200", description = "User deleted successfully"),
                    @ApiResponse(responseCode = "400", description = "Invalid credentials or token"),
                    @ApiResponse(responseCode = "500", description = "Internal error while deleting the user")
            }
    )
    @Parameter(
            name = "Authorization",
            in = ParameterIn.HEADER,
            required = true,
            description = "JWT token in the format: Bearer &lt;token&gt;"
    )
    @DeleteMapping("/del")
    public ResponseEntity<?> delUser(HttpServletRequest request) throws BadCredentialsException{

        try {
            myUserService.delUser(SecurityContextHolder.getContext().getAuthentication(),request);
            return ResponseEntity.ok("User deleted successfully");
        } catch (BadCredentialsException e){
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (ImageNotMovedException | ComplaintsNotDeletedException | CommentNotDeletedException e){
            log.error("class: "+e.getClass()+" message: "+e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Due to an internal error, your account was not deleted");
        }
    }

    @Hidden
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
