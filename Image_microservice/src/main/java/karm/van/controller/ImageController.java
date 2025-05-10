package karm.van.controller;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import karm.van.dto.ImageDto;
import karm.van.exception.*;
import karm.van.service.ImageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Log4j2
@RequiredArgsConstructor
@RestController
@CrossOrigin
@RequestMapping("/image")
public class ImageController {
    private final ImageService imageService;

    @Value("${minio.bucketNames.image-bucket}")
    private String minioImageBucket;

    @Value("${minio.bucketNames.profile-image-bucket}")
    private String minioProfileImageBucket;

    @Value("${minio.bucketNames.trash-bucket}")
    private String minioTrashBucket;

    @Hidden
    @GetMapping("/get")
    public List<ImageDto> getCardImages(@RequestParam List<Long> imagesId,
                                        @RequestHeader("x-api-key") String key,
                                        @RequestHeader("Authorization") String authorization) throws TokenNotExistException, InvalidApiKeyException {
        if(imageService.checkNoneEqualsApiKey(key)){
            throw new InvalidApiKeyException("Invalid api-key");
        }
        return imageService.getImages(imagesId,authorization);
    }

    @Hidden
    @GetMapping("/get-one/{imageId}")
    public ResponseEntity<?> getImage(@PathVariable Long imageId,
                                      @RequestHeader("Authorization") String authorization,
                                      @RequestHeader("x-api-key") String key){
        try {
            if(imageService.checkNoneEqualsApiKey(key)){
                throw new InvalidApiKeyException("Invalid api-key");
            }

            return ResponseEntity.ok(imageService.getImage(imageId,authorization));
        } catch (ImageNotFoundException | TokenNotExistException | InvalidApiKeyException e) {
            log.error("Error deleting image: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @Hidden
    @DeleteMapping("/del/{imageId}")
    public ResponseEntity<?> deleteOneImage(@PathVariable Long imageId,
                               @RequestHeader("x-api-key") String key){

        try {
            if(imageService.checkNoneEqualsApiKey(key)){
                throw new InvalidApiKeyException("Invalid api-key");
            }
            imageService.deleteImage(imageId);
            return ResponseEntity.ok("Success delete");
        } catch (ImageNotFoundException | TokenNotExistException | InvalidApiKeyException e) {
            log.error("Error deleting image: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (ImageNotDeletedException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @Hidden
    @PostMapping(value = "/addCardImages", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public List<Long> addCardImages(@RequestPart("files") List<MultipartFile> files,
                                    @RequestPart("currentCardImagesCount") int currentCardImagesCount,
                                    @RequestHeader("Authorization") String authorization,
                                    @RequestHeader("x-api-key") String key) throws ImageNotSavedException, ImageLimitException, TokenNotExistException, InvalidApiKeyException {
        if(imageService.checkNoneEqualsApiKey(key)){
            throw new InvalidApiKeyException("Invalid api-key");
        }
        try {
            return imageService.addCardImages(files, currentCardImagesCount,authorization,minioImageBucket);
        } catch (ImageNotSavedException | ImageLimitException | TokenNotExistException e) {
            log.error("Error adding card images: {}", e.getMessage(), e);
            throw e;
        }
    }

    @PostMapping(value = "/addProfileImage", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Add a profile image",
            description = "Adds an image as the user's profile picture."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Profile image added successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized: Invalid or missing token"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error: Error while saving the profile image")
    })
    @Parameter(
            name = "Authorization",
            in = ParameterIn.HEADER,
            required = true,
            description = "JWT token in the format: Bearer &lt;token&gt;"
    )
    public ResponseEntity<?> addProfileImage(
            @Parameter(description = "The profile image to be uploaded")
            @RequestPart("profileImage") MultipartFile profileImage,

            @RequestHeader("Authorization") String authorization) {

        try {
            imageService.addProfileImage(profileImage, authorization, minioProfileImageBucket);
            return ResponseEntity.ok("The profile picture has been successfully added");
        } catch (TokenNotExistException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid token");
        } catch (ImageNotSavedException e) {
            log.error("Error adding profile image: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        } catch (Exception e) {
            log.error("Error adding images: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error adding images");
        }
    }

    @Hidden
    @PostMapping(value = "/move")
    public ResponseEntity<?> moveImagesBetweenBuckets(@RequestParam List<Long> ids,
                                                      @RequestParam(value = "toTrash",required = false,defaultValue = "false") Boolean toTrash,
                                                      @RequestHeader("x-api-key") String key) {



        try {
            if(imageService.checkNoneEqualsApiKey(key)){
                throw new InvalidApiKeyException("Invalid api-key");
            }

            String bucketToMove = toTrash? minioTrashBucket:minioImageBucket;

            imageService.moveAllImagesBetweenBuckets(ids,bucketToMove);
            return ResponseEntity.ok("ok");
        } catch (TokenNotExistException | InvalidApiKeyException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Error moving images: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error moving images");
        }
    }

    @Hidden
    @PostMapping(value = "/profile/move/{imageId}")
    public ResponseEntity<?> moveProfileImagesBetweenBuckets(@PathVariable Long imageId,
                                                             @RequestParam(value = "toTrash",required = false,defaultValue = "false") Boolean toTrash,
                                                             @RequestHeader("x-api-key") String key) {



        try {
            if(imageService.checkNoneEqualsApiKey(key)){
                throw new InvalidApiKeyException("Invalid api-key");
            }
            String bucketToMove = toTrash? minioTrashBucket:minioProfileImageBucket;

            imageService.moveImageBetweenBuckets(imageId,bucketToMove);
            return ResponseEntity.ok("ok");
        } catch (TokenNotExistException | InvalidApiKeyException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        } catch (Exception e) {
            log.error("Error moving images: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error moving images");
        }
    }

    @Hidden
    @DeleteMapping("/minio/del")
    public ResponseEntity<?> delImagesFromMinio(@RequestParam List<Long> ids,
                                                @RequestHeader("x-api-key") String key) {


        try {
            if(imageService.checkNoneEqualsApiKey(key)){
                throw new InvalidApiKeyException("Invalid api-key");
            }

            imageService.deleteAllImages(ids);
            return ResponseEntity.ok("Images deleted");
        } catch (TokenNotExistException | InvalidApiKeyException e){
            return ResponseEntity.badRequest().body(e.getMessage());
        }catch (Exception e) {
            log.error("Error deleting images from Minio: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to delete images from Minio");
        }
    }
}