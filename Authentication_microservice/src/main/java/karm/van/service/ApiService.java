package karm.van.service;

import jakarta.annotation.PostConstruct;
import karm.van.dto.response.CommentDto;
import karm.van.dto.response.ProfileImageDtoResponse;
import karm.van.dto.response.UserCardResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@RequiredArgsConstructor
@Component
@Slf4j
public class ApiService {
    private WebClient webClient;

    @PostConstruct
    public void init(){
        webClient = WebClient.create();
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

    public String buildUrl(String prefix, String host, String port, String endpoint, String... pathSegments) {
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(prefix + host + ":" + port + endpoint);

        for (Object segment : pathSegments) {
            uriBuilder.pathSegment(segment.toString());
        }

        return uriBuilder.toUriString();
    }

    public String buildUrl(String prefix, String host, String port, String endpoint) {
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(prefix + host + ":" + port + endpoint);
        return uriBuilder.toUriString();
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

    public HttpStatusCode moveProfileImage(String url, String apiKey, Boolean toTrash) {

        String fullUrl = UriComponentsBuilder.fromHttpUrl(url)
                .queryParam("toTrash",toTrash)
                .toUriString();

        return sendMoveRequest(fullUrl,apiKey);
    }

    public HttpStatusCode deleteImageFromMinioRequest(String url, String token, String apiKey) {
        return sendDeleteRequest(url,token,apiKey);
    }

    private HttpStatusCode sendDeleteRequest(String url, String token, String apiKey) {
        return sendDeleteRequest(url, token, apiKey != null ? Optional.of(apiKey) : Optional.empty());
    }

    private HttpStatusCode sendDeleteRequest(String url, String token) {
        return sendDeleteRequest(url, token, Optional.empty());
    }

    private HttpStatusCode sendDeleteRequest(String url, String token, Optional<String> apiKey) {
        return Objects.requireNonNull(
                        webClient
                                .delete()
                                .uri(url)
                                .headers(headers -> {
                                    headers.setBearerAuth(token);
                                    apiKey.ifPresent(key -> headers.set("x-api-key", key));
                                })
                                .retrieve()
                                .toBodilessEntity()
                                .block())
                .getStatusCode();
    }

    public <T> T sendDeleteRequestForList(String url, String token, String apiKey,ParameterizedTypeReference<T> responseType) {

        return Objects.requireNonNull(
                webClient
                        .delete()
                        .uri(url)
                        .headers(headers -> {
                            headers.setBearerAuth(token);
                            headers.set("x-api-key", apiKey);
                        })
                        .retrieve()
                        .bodyToMono(responseType)
                        .block()
        );
    }

    private <T> T sendGetResponse(String uri, String token, ParameterizedTypeReference<T> responseType, String apiKey){
        try {
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
        }catch (Exception e){
            return null;
        }

    }

    private <T> T sendGetResponse(String uri, String token, String apiKey, Class<T> responseType){
        try {
            return webClient
                    .get()
                    .uri(uri)
                    .headers(httpHeaders -> {
                        httpHeaders.set("x-api-key",apiKey);
                        httpHeaders.setBearerAuth(token);
                    })
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, clientResponse -> {
                        throw new RuntimeException();
                    })
                    .bodyToMono(responseType)
                    .block();
        }catch (Exception e){
            return null;
        }
    }

    public HttpStatusCode requestToDelCard(String url, String token) {
        return sendDeleteRequest(url,token);
    }

    private HttpStatusCode sendMoveRequest(String url,String apiKey){
        return sendPostRequest(url,apiKey);
    }

    public List<UserCardResponse> getCardImagesRequest(String uri, String token, String apiKey) {

        return sendGetResponse(
                uri,
                token,
                new ParameterizedTypeReference<>() {
                },
                apiKey
        );
    }

    public ProfileImageDtoResponse requestToGetProfileImage(String uri, String token, String apiKey){
        return sendGetResponse(uri,token,apiKey, ProfileImageDtoResponse.class);
    }

    public HttpStatusCode requestToDeleteAllComplaintByUserId(String uri, String token, String apiKey){
        return sendDeleteRequest(uri,token,apiKey);
    }

    public List<CommentDto> requestToDeleteAllUserComments(String uri, String token, String apiKey){
        return sendDeleteRequestForList(uri,token,apiKey,new ParameterizedTypeReference<>() {});
    }

    public HttpStatusCode requestToRollBackDeletedComments(String url,String apiKey,List<CommentDto> commentDtos){
        return sendPostRequest(url,apiKey,commentDtos);
    }

}
