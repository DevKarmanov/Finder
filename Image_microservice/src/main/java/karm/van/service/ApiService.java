package karm.van.service;

import jakarta.annotation.PostConstruct;
import karm.van.dto.UserDtoRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

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

    private <T> T sendPatchRequest(String uri, String token, String apikey, Class<T> responseType){
        try {
            return webClient
                    .patch()
                    .uri(uri)
                    .headers(headers->{
                        headers.setBearerAuth(token);
                        headers.set("x-api-key",apikey);
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

    public Boolean validateToken(String token, String url) {
        Map<?, ?> responseMap = sendGetResponse(url, token, Map.class,null);
        return responseMap != null && responseMap.containsKey("valid") && (Boolean) responseMap.get("valid");
    }


    public Long requestToLinkImageAndUser(String uri, String token, String apiKey) {
        return sendPatchRequest(uri,token,apiKey, Long.class);
    }

    public UserDtoRequest getUserByToken(String url, String token, String apiKey) {
        return fetchUserData(url, token, apiKey);
    }

    private UserDtoRequest fetchUserData(String uri, String token, String apiKey) {
        return sendGetResponse(uri,token, UserDtoRequest.class,apiKey);
    }
}
