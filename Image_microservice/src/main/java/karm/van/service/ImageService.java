package karm.van.service;

import io.lettuce.core.api.sync.RedisCommands;
import karm.van.config.AuthenticationMicroServiceProperties;
import karm.van.dto.ImageDto;
import karm.van.dto.ImageDtoResponse;
import karm.van.dto.UserDtoRequest;
import karm.van.exception.*;
import karm.van.model.ImageModel;
import karm.van.repository.ImageRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageService {
    private final RedisCommands<String,String> redisCommands;
    private final MinioService minioService;
    private final ImageRepo imageRepo;
    private final AuthenticationMicroServiceProperties authProperties;
    private final ApiService apiService;

    @Value("${microservices.x-api-key}")
    private String apiKey;


    @Value("${card.images.count}")
    private int allowedImagesCount;

    private void saveImage(MultipartFile file, String fileName, String bucketName) throws ImageNotSavedException {
        try {
            minioService.putObject(bucketName,file, fileName);
        }catch (Exception e){
            log.error("The image was not saved: "+e.getMessage()+" - "+e.getClass());
            throw new ImageNotSavedException(e.getMessage());
        }
    }

    private void delImage(String imageBucket, String imageName) throws ImageNotDeletedException {
        try {
            minioService.delObject(imageBucket,imageName);
        }catch (Exception e){
            log.error("The image was not deleted: "+e.getMessage()+" - "+e.getClass());
            throw new ImageNotDeletedException(e.getMessage());
        }
    }

    @Async
    protected void delImageAsync(String imageBucket, String imageName) throws ImageNotDeletedException {
        delImage(imageBucket,imageName);
    }

    private void checkToken(String token) throws TokenNotExistException {
        if (!apiService.validateToken(token,
                apiService.buildUrl(authProperties.getPrefix(),
                        authProperties.getHost(),
                        authProperties.getPort(),
                        authProperties.getEndpoints().getValidateToken()
                )
        )){
            throw new TokenNotExistException("Invalid token or expired");
        }
    }

    @Transactional
    public List<Long> addCardImages(List<MultipartFile> files, int currentCardImagesCount, String authorization, String bucketName) throws ImageNotSavedException, ImageLimitException, TokenNotExistException {
        checkToken(authorization.substring(7));
        if (currentCardImagesCount<allowedImagesCount){
            int freeMemory = allowedImagesCount - currentCardImagesCount;

            List<MultipartFile> filesToAdd = files.subList(0,Math.min(freeMemory,files.size()));

            String unique_uuid = UUID.randomUUID().toString();

            List<Long> imagesId = new ArrayList<>();

            for (MultipartFile file:filesToAdd){
                String fileName = unique_uuid+"-"+file.getOriginalFilename();

                ImageModel imageModel = ImageModel.builder()
                        .imageName(fileName)
                        .imageBucket(bucketName)
                        .build();
                try {
                    imageRepo.save(imageModel);
                    imagesId.add(imageModel.getId());
                }catch (Exception e){
                    throw new ImageNotSavedException("There was a problem while saving the image");
                }
            }

            filesToAdd.parallelStream().forEach(file -> {
                String fileName = unique_uuid+"-"+file.getOriginalFilename();

                try {
                    saveImage(file,fileName,bucketName);
                }catch (Exception e){
                    throw new RuntimeException(new ImageNotSavedException("There is a problem with image processing, so the article has not been published"));
                }
            });
            return imagesId;
        }else {
            throw new ImageLimitException("You have provided more than" + allowedImagesCount + "images");
        }
    }

    private void deleteImagesFromMinio(List<ImageModel> images){
        images.parallelStream().forEach(image->{
            try {
                delImage(image.getImageBucket(), image.getImageName());
            } catch (ImageNotDeletedException e) {
                throw new RuntimeException(new ImageNotDeletedException("An error occurred while deleting the card"));
            }
        });
    }

    @Transactional
    public void moveAllImagesBetweenBuckets(List<Long> imagesId,String targetBucket) throws TokenNotExistException{

        List<ImageModel> images = imageRepo.findAllById(imagesId);
        images.parallelStream().forEach(imageModel -> {
            try {
                minioService.moveObject(imageModel.getImageBucket(), targetBucket, imageModel.getImageName());
                imageModel.setImageBucket(targetBucket);
                imageRepo.save(imageModel);
            } catch (ImageNotFoundException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Transactional
    public void moveImageBetweenBuckets(Long imagesId, String targetBucket) throws TokenNotExistException, ImageNotFoundException {

        ImageModel image = imageRepo.findById(imagesId)
                .orElseThrow(() -> new ImageNotFoundException("Image with this id doesn't exist"));

        try {
            minioService.moveObject(image.getImageBucket(), targetBucket, image.getImageName());
            image.setImageBucket(targetBucket);
            imageRepo.save(image);
        } catch (ImageNotFoundException e) {
            throw new RuntimeException(e);
        }
    }


    @Transactional
    public void deleteAllImages(List<Long> imagesId) throws TokenNotExistException {
        List<ImageModel> images = imageRepo.findAllById(imagesId);
        images.parallelStream().forEach(imageModel -> {
            try {
                minioService.delObject(imageModel.getImageBucket(),imageModel.getImageName());
            } catch (ImageNotDeletedException e) {
                throw new RuntimeException(e);
            }
        });
        deleteImagesFromMinio(images);
        imageRepo.deleteAllById(imagesId);
    }

    @Transactional
    public void deleteImage(Long imageId) throws ImageNotFoundException, ImageNotDeletedException, TokenNotExistException {
        ImageModel imageModel = imageRepo.findById(imageId)
                .orElseThrow(() -> new ImageNotFoundException("Image with this id doesn't exist"));

        try {
            minioService.delObject(imageModel.getImageBucket(), imageModel.getImageName());
            imageRepo.delete(imageModel);
        } catch (Exception e){
            log.error("class: "+e.getClass()+" message: "+e.getMessage());
            throw new ImageNotDeletedException("Failed to delete image");
        }
    }

    private ImageDto imageModelToDto(ImageModel imageModel){
        return new ImageDto(imageModel.getId(),imageModel.getImageBucket(),imageModel.getImageName());
    }

    public List<ImageDto> getImages(List<Long> imagesId,String authorization) throws TokenNotExistException {
        checkToken(authorization.substring(7));
        return imageRepo.findAllById(imagesId)
                .stream().map(this::imageModelToDto).toList();
    }

    public boolean checkNoneEqualsApiKey(String key) {
        return !key.equals(apiKey);
    }

    private Long sendRequestToLinkImageAndUser(String token, Long profileImageId) throws ImageNotLinkException {
        String uri = apiService.buildUrl(
                authProperties.getPrefix(),
                authProperties.getHost(),
                authProperties.getPort(),
                authProperties.getEndpoints().getAddProfileImage(),
                profileImageId
        );

        try {
            Long result = apiService.requestToLinkImageAndUser(uri,token,apiKey);
            if (result == null){
                throw new RuntimeException();
            }

            return result;
        } catch (Exception e){
            log.error("class: "+e.getClass()+" message: "+e.getMessage());
            throw new ImageNotLinkException("There was a problem while linking the image");
        }
    }

    private UserDtoRequest requestToGetUserByToken(String token) throws UsernameNotFoundException {
        UserDtoRequest user = apiService.getUserByToken(apiService.buildUrl(
                authProperties.getPrefix(),
                authProperties.getHost(),
                authProperties.getPort(),
                authProperties.getEndpoints().getUser()
        ), token,apiKey);

        if (user == null) {
            throw new UsernameNotFoundException("User not found");
        }

        return user;
    }

    @Transactional
    public void addProfileImage(MultipartFile profileImage, String authorization, String minioProfileImageBucket) throws TokenNotExistException, ImageNotSavedException, ImageNotDeletedException, UsernameNotFoundException {
        String token = authorization.substring(7);
        checkToken(token);
        String fileName = UUID.randomUUID()+"-"+profileImage.getOriginalFilename();

        ImageModel imageModel = ImageModel.builder()
                .imageName(fileName)
                .imageBucket(minioProfileImageBucket)
                .build();

        imageRepo.save(imageModel);

        UserDtoRequest user = requestToGetUserByToken(token);

        String redisKey = "user_"+user.id();

        System.out.println("PHOTO REDIS KEY: "+redisKey);
        redisCommands.del(redisKey);

        try {
            saveImage(profileImage,fileName,minioProfileImageBucket);
            Long oldImageId = sendRequestToLinkImageAndUser(token,imageModel.getId());
            if (oldImageId>0){
                ImageModel oldProfileImage = imageRepo.getReferenceById(oldImageId);
                delImageAsync(oldProfileImage.getImageBucket(),oldProfileImage.getImageName());
            }
        } catch (ImageNotSavedException e) {
            throw new ImageNotSavedException("There is a problem with image processing");
        } catch (ImageNotLinkException e){
            log.error("class: "+e.getClass()+" message: "+e.getMessage());
            delImageAsync(minioProfileImageBucket,fileName);
            throw new ImageNotSavedException("There is a problem with image processing");
        }
    }

    public ImageDtoResponse getImage(Long imageId, String authorization) throws TokenNotExistException, ImageNotFoundException {
        checkToken(authorization.substring(7));

        return imageRepo.findById(imageId)
                .map(image->new ImageDtoResponse(image.getImageName(),image.getImageBucket()))
                .orElseThrow(()->new ImageNotFoundException(("Image with this id doesn't exist")));

    }
}
