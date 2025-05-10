package karm.van.service;

import jakarta.annotation.PostConstruct;
import karm.van.dto.comment.FullCommentDtoResponse;
import karm.van.dto.image.ImageDto;
import karm.van.dto.user.UserDtoRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;


@Component
@Slf4j
public class ApiService {
    private WebClient webClient;

    @PostConstruct
    public void init() {
        webClient = WebClient.create();
    }

    private <T> T sendPostRequest(String url, Object body, String token, String apiKey, ParameterizedTypeReference<T> responseType) {
        return webClient
                .post()
                .uri(url)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .headers(headers -> {
                    headers.setBearerAuth(token);
                    headers.set("x-api-key", apiKey);
                })
                .bodyValue(body)
                .retrieve()
                .bodyToMono(responseType)
                .block();
    }

    private HttpStatusCode sendPostRequest(String url, String apiKey) {
        return Objects.requireNonNull(
                webClient
                        .post()
                        .uri(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .headers(headers -> headers.set("x-api-key", apiKey))
                        .retrieve()
                        .toBodilessEntity()
                        .block()
        ).getStatusCode();
    }

    private HttpStatusCode sendDeleteRequest(String url,String apiKey) {
        return Objects.requireNonNull(
                    webClient
                        .delete()
                        .uri(url)
                        .headers(headers -> headers.set("x-api-key", apiKey))
                        .retrieve()
                        .toBodilessEntity()
                        .block())
                .getStatusCode();
    }

    private HttpStatusCode sendDeleteRequest(String url,String token, String apiKey) {
        return Objects.requireNonNull(
                        webClient
                                .delete()
                                .uri(url)
                                .headers(headers -> {
                                    headers.setBearerAuth(token);
                                    headers.set("x-api-key", apiKey);
                                })
                                .retrieve()
                                .toBodilessEntity()
                                .block())
                .getStatusCode();
    }

    private <T> T sendDeleteRequest(String url, String token, String apiKey, ParameterizedTypeReference<T> responseType) {
        return webClient
                .delete()
                .uri(url)
                .headers(headers -> {
                    headers.setBearerAuth(token);
                    headers.set("x-api-key", apiKey);
                })
                .retrieve()
                .bodyToMono(responseType)
                .block();
    }


    private <T> T sendGetResponse(String uri, String token, ParameterizedTypeReference<T> responseType, String apiKey){
        return webClient
                .get()
                .uri(uri)
                .headers(httpHeaders -> {
                    httpHeaders.set("x-api-key",apiKey);
                    httpHeaders.setBearerAuth(token);
                })
                .retrieve()
                .bodyToMono(responseType)
                .block();
    }

    private <T> T sendGetResponse(String uri, String token, Class<T> responseType, String apiKey) {
        try {
            return webClient
                    .get()
                    .uri(uri)
                    .headers(httpHeaders -> {
                        httpHeaders.setBearerAuth(token);
                        if (apiKey != null && !apiKey.trim().isEmpty()) {
                            httpHeaders.set("x-api-key", apiKey);
                        }
                    })
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, clientResponse -> {
                        throw new RuntimeException();
                    })
                    .bodyToMono(responseType)
                    .block();
        } catch (Exception e) {
            return null;
        }
    }

    private MultiValueMap<String, HttpEntity<?>> buildMultipartBody(List<MultipartFile> files, int currentCardImagesCount) {
        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        files.forEach(file -> builder.part("files", file.getResource()));
        builder.part("currentCardImagesCount", currentCardImagesCount);
        return builder.build();
    }


    public List<Long> postRequestToAddCardImage(List<MultipartFile> files, String url, int currentCardImagesCount, String token, String apiKey) {
        return sendPostRequest(
                url,
                buildMultipartBody(files, currentCardImagesCount),
                token,
                apiKey,
                new ParameterizedTypeReference<>() {}
        );
    }


    public List<ImageDto> getCardImagesRequest(List<Long> imagesId, String url, String token, String apikey) {

        String uri = UriComponentsBuilder.fromHttpUrl(url)
                .queryParam("imagesId", imagesId)
                .toUriString();

        return sendGetResponse(
                uri,
                token,
                new ParameterizedTypeReference<>() {
                },
                apikey
        );
    }

    private UserDtoRequest fetchUserData(String uri, String token, String apiKey) {
        return sendGetResponse(uri,token, UserDtoRequest.class,apiKey);
    }

    public UserDtoRequest getUserByToken(String url, String token, String apiKey) {
        return fetchUserData(url, token, apiKey);
    }

    public UserDtoRequest getUserById(String url, String token, Long userId, String apiKey) {
        String uri = UriComponentsBuilder.fromHttpUrl(url)
                .queryParam("userId", userId)
                .toUriString();
        return fetchUserData(uri, token, apiKey);
    }

    public Boolean validateToken(String token, String url) {
        Map<?, ?> responseMap = sendGetResponse(url, token, Map.class,null);
        return responseMap != null && responseMap.containsKey("valid") && (Boolean) responseMap.get("valid");
    }


    public List<FullCommentDtoResponse> requestToDelAllCommentsByCard(String url, String token, String apiKey) {
        return sendDeleteRequest(url,token,apiKey,new ParameterizedTypeReference<>(){});
    }

    public HttpStatusCode sendDeleteImagesFromMinioRequest(String url, List<Long> imagesId, String apiKey) {
        String ids = getIds(imagesId);

        String fullUrl = UriComponentsBuilder.fromHttpUrl(url)
                .queryParam("ids", ids)
                .toUriString();

        return sendDeleteRequest(fullUrl,apiKey);
    }

    public HttpStatusCode requestToUnlinkFavoriteCardAndUser(String url, String token, String apiKey){
        return sendDeleteRequest(url,token,apiKey);
    }

    public HttpStatusCode requestToDeleteOneImageFromDB(String url, String apiKey) {
        return sendDeleteRequest(url,apiKey);
    }

    public HttpStatusCode requestToUnlinkCardFromUser(String url, String token, String apiKey) {
        return sendDeleteRequest(url,token,apiKey);
    }

    private HttpStatusCode sendMoveRequest(String url,String apiKey){
        return sendPostRequest(url,apiKey);
    }

    private String getIds(List<?> imagesId){
        return imagesId.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
    }

    public HttpStatusCode moveImagesToImagePackage(String url, List<Long> imagesId, String apiKey) {
        String ids = getIds(imagesId);

        String fullUrl = UriComponentsBuilder.fromHttpUrl(url)
                .queryParam("ids", ids)
                .toUriString();

        return sendMoveRequest(fullUrl,apiKey);
    }

    public HttpStatusCode moveImagesToTrashPackage(String url, List<Long> imagesId, String apiKey) {
        String ids = getIds(imagesId);

        String fullUrl = UriComponentsBuilder.fromHttpUrl(url)
                .queryParam("ids", ids)
                .queryParam("toTrash",true)
                .toUriString();

        return sendMoveRequest(fullUrl,apiKey);
    }

    public HttpStatusCode addCardToUser(String url,String apiKey) throws NullPointerException{
        return sendPostRequest(url,apiKey);
    }

    public String buildUrl(String prefix, String host, String port, String endpoint, Long... ids) {
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(prefix + host + ":" + port + endpoint);

        if (ids.length > 0) {
            uriBuilder.pathSegment(ids[0].toString());
            for (int i = 1; i < ids.length; i++) {
                uriBuilder.pathSegment(ids[i].toString());
            }
        }

        return uriBuilder.toUriString();
    }

    public String buildUrl(String prefix, String host, String port, String endpoint) {
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(prefix + host + ":" + port + endpoint);
        return uriBuilder.toUriString();
    }

    private HttpStatusCode sendPostRequest(String url, String apiKey, List<?> requestBody) {
        return Objects.requireNonNull(
                webClient
                        .post()
                        .uri(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .headers(headers -> headers.set("x-api-key", apiKey))
                        .bodyValue(requestBody)
                        .retrieve()
                        .toBodilessEntity()
                        .block()
        ).getStatusCode();
    }

    public HttpStatusCode requestToRollBackDeletedComments(String url,String apiKey,List<FullCommentDtoResponse> commentDtos){
        return sendPostRequest(url,apiKey,commentDtos);
    }

}
