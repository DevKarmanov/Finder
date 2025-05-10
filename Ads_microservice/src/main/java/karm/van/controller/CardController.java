package karm.van.controller;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import karm.van.dto.card.CardDto;
import karm.van.dto.card.CardPageResponseDto;
import karm.van.dto.card.FullCardDtoForOutput;
import karm.van.exception.card.CardNotDeletedException;
import karm.van.exception.card.CardNotFoundException;
import karm.van.exception.card.CardNotSavedException;
import karm.van.exception.comment.CommentNotDeletedException;
import karm.van.exception.image.ImageLimitException;
import karm.van.exception.image.ImageNotDeletedException;
import karm.van.exception.image.ImageNotMovedException;
import karm.van.exception.image.ImageNotSavedException;
import karm.van.exception.other.SerializationException;
import karm.van.exception.other.TokenNotExistException;
import karm.van.exception.user.NotEnoughPermissionsException;
import karm.van.exception.user.UsernameNotFoundException;
import karm.van.service.CardService;
import karm.van.service.ElasticService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Log4j2
@RequiredArgsConstructor
@RestController
@CrossOrigin
@RequestMapping("/card/")
public class CardController {
    private final CardService cardService;
    private final ElasticService elasticService;

    @Operation(
            summary = "Get card by ID",
            description = "Retrieves the full details of a card using its ID.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successful retrieval of the card",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = FullCardDtoForOutput.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Card not found",
                            content = @Content(schema = @Schema(type = "string"))
                    )
            }
    )
    @Parameter(
            name = "Authorization",
            in = ParameterIn.HEADER,
            required = true,
            description = "JWT token in the format: Bearer &lt;token&gt;"
    )
    @GetMapping("{id}/get")
    public FullCardDtoForOutput getCard(@PathVariable Long id,  @RequestHeader("Authorization") String authorization) throws CardNotFoundException, SerializationException, TokenNotExistException, UsernameNotFoundException {
        return cardService.getCard(id,authorization);
    }

    @Operation(
            summary = "Search cards with filters",
            description = "Search for cards based on the provided query and optional filters for creation time and tags.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successful search",
                            content = @Content(schema = @Schema(implementation = CardPageResponseDto.class))
                    ),
                    @ApiResponse(
                            responseCode = "500",
                            description = "Internal server error",
                            content = @Content(schema = @Schema(type = "string"))
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Unauthorized",
                            content = @Content(schema = @Schema(type = "string"))
                    )
            }
    )
    @Parameter(
            name = "Authorization",
            in = ParameterIn.HEADER,
            required = true,
            description = "JWT token in the format: Bearer &lt;token&gt;"
    )
    @GetMapping("/search")
    public ResponseEntity<?> searchCards(@RequestParam(required = false,defaultValue = "0") int pageNumber,
                                         @RequestParam(required = false,defaultValue = "5") int limit,
                                         @RequestParam String query,
                                         @RequestParam(required = false) Optional<LocalDate> createTime,
                                         @RequestParam(required = false) Optional<List<String>> tags,
                                         @RequestHeader("Authorization") String authorization){
        try {
            return ResponseEntity.ok(elasticService.search(query,pageNumber,limit,authorization,createTime,tags));
        }catch (SerializationException e){
            return ResponseEntity.internalServerError().body(e.getMessage());
        }catch (TokenNotExistException e){
            return ResponseEntity.status(HttpStatus.SC_UNAUTHORIZED).body(e.getMessage());
        }
    }

    @Operation(
            summary = "Retrieve all ads with pagination support",
            description = "Fetch all ads with pagination support. Returns a paginated list of ads.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successful retrieval of paginated results",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = CardPageResponseDto.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Bad Request due to invalid pagination parameters",
                            content = @Content(schema = @Schema(type = "string"))
                    )
            }
    )
    @Parameter(
            name = "Authorization",
            in = ParameterIn.HEADER,
            required = true,
            description = "JWT token in the format: Bearer &lt;token&gt;"
    )
    @GetMapping("getAll/{pageNumber}/{limit}")
    public CardPageResponseDto getAllCards(
            @Parameter(description = "The page number to retrieve") @PathVariable int pageNumber,
            @Parameter(description = "The number of ads per page") @PathVariable int limit,
            @RequestHeader("Authorization") String authorization
    ) throws TokenNotExistException, SerializationException {
        return cardService.getAllCards(pageNumber, limit, authorization);
    }

    @Hidden
    @GetMapping("getUserCards/{userId}")
    public List<CardDto> getUserCards(@RequestHeader("Authorization") String authorization,
                                      @RequestHeader("x-api-key") String apiKey,
                                      @PathVariable("userId") Long userId) throws TokenNotExistException {
        try {
            return cardService.getAllUserCards(authorization,apiKey,userId);
        } catch (TokenNotExistException e) {
            log.error("class: "+e.getClass()+", message: "+e.getMessage());
            throw e;
        }
    }

    @Operation(
            summary = "Add a new ad",
            description = "Adds a new ad along with associated images. Requires a JWT token for authentication.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successfully created a new ad"
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Bad Request - Invalid input data (e.g., too many images, missing fields)",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(type = "string")
                            )
                    ),
                    @ApiResponse(
                            responseCode = "500",
                            description = "Internal Server Error - Issue during the ad creation process",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(type = "string")
                            )
                    )
            }
    )
    @Parameter(
            name = "Authorization",
            in = ParameterIn.HEADER,
            required = true,
            description = "JWT token in the format: Bearer &lt;token&gt;"
    )
    @PostMapping(value = "add", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public void addCard(
            @Parameter(description = "Card details including title and text") @RequestPart("cardDto") CardDto cardDto,
            @Parameter(description = "List of images to be uploaded and attached to the ad") @RequestPart("files") List<MultipartFile> files,
            @RequestHeader("Authorization") String authorization
    ) throws ImageNotSavedException, CardNotSavedException, ImageLimitException, TokenNotExistException, UsernameNotFoundException {
        try {
            cardService.addCard(files, cardDto, authorization);
        } catch (ImageNotSavedException | CardNotSavedException | ImageLimitException | TokenNotExistException |
                 UsernameNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("class: "+e.getClass()+", message: "+e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
    }

    @Operation(
            summary = "Update an existing ad",
            description = "This endpoint allows you to update an existing ad. You can update the ad's details (title, text) and attach new images.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successfully updated the ad"),
                    @ApiResponse(responseCode = "404", description = "Ad not found"),
                    @ApiResponse(responseCode = "403", description = "Forbidden, insufficient permissions"),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error")
            }
    )
    @Parameter(
            name = "Authorization",
            in = ParameterIn.HEADER,
            required = true,
            description = "JWT token in the format: Bearer &lt;token&gt;"
    )
    @PatchMapping(value = "{id}/patch", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public void patchCard(@PathVariable Long id,
                          @Parameter(description = "Card details including title and text") @RequestPart(value = "cardDto", required = false) Optional<CardDto> cardDto,
                          @Parameter(description = "List of images to be uploaded and attached to the ad") @RequestPart(value = "files", required = false) Optional<List<MultipartFile>> files,
                          @RequestHeader("Authorization") String authorization)
            throws CardNotFoundException, CardNotSavedException, ImageNotSavedException, ImageLimitException, TokenNotExistException, NotEnoughPermissionsException {

        try {
            cardService.patchCard(id, cardDto, files, authorization);
        } catch (CardNotFoundException | CardNotSavedException | ImageNotSavedException | ImageLimitException | TokenNotExistException | NotEnoughPermissionsException e) {
            throw e;
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
    }

    @Operation(
            summary = "Delete an ad by its unique ID",
            description = "This endpoint allows the deletion of an ad based on its unique ID. The user must have appropriate permissions and be authenticated.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successfully deleted the ad"),
                    @ApiResponse(responseCode = "404", description = "Ad not found"),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error, if deletion fails")
            }
    )
    @Parameter(
            name = "Authorization",
            in = ParameterIn.HEADER,
            required = true,
            description = "JWT token in the format: Bearer &lt;token&gt;"
    )
    @DeleteMapping("/del/{id}")
    public void delCard(@PathVariable Long id,
                        @RequestHeader("Authorization") String authorization)
            throws TokenNotExistException, CardNotFoundException, NotEnoughPermissionsException, CardNotDeletedException {

        try {
            cardService.deleteCard(id, authorization);
        } catch (CommentNotDeletedException | ImageNotMovedException | CardNotDeletedException e) {
            throw new CardNotDeletedException("Due to an error, the ad was not deleted");
        } catch (TokenNotExistException | CardNotFoundException | NotEnoughPermissionsException e) {
            throw e;
        } catch (Exception e) {
            log.error("class: " + e.getClass() + ", message: " + e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
    }

    @Operation(
            summary = "Delete a specific image from an ad",
            description = "This endpoint allows the deletion of a specific image from an ad based on the given card ID and image ID. The user must be authenticated and have the necessary permissions.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successfully deleted the image from the ad"),
                    @ApiResponse(responseCode = "404", description = "Ad or image not found"),
                    @ApiResponse(responseCode = "500", description = "Internal Server Error, if deletion fails")
            }
    )
    @Parameter(
            name = "Authorization",
            in = ParameterIn.HEADER,
            required = true,
            description = "JWT token in the format: Bearer &lt;token&gt;"
    )
    @DeleteMapping("/image/del/{cardId}/{imageId}")
    public void delOneImageFromCard(@PathVariable Long cardId,
                                    @PathVariable Long imageId,
                                    @RequestHeader("Authorization") String authorization)
            throws CardNotFoundException, TokenNotExistException, ImageNotDeletedException, UsernameNotFoundException, NotEnoughPermissionsException {
        cardService.delOneImageInCard(cardId, imageId, authorization);
    }
}



