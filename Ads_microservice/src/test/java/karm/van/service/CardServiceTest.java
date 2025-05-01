package karm.van.service;

import karm.van.dto.card.CardDto;
import karm.van.exception.card.CardNotSavedException;
import karm.van.exception.image.ImageLimitException;
import karm.van.exception.image.ImageNotSavedException;
import karm.van.exception.other.TokenNotExistException;
import karm.van.exception.user.UsernameNotFoundException;
import karm.van.model.CardDocument;
import karm.van.model.CardModel;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest
public class CardServiceTest {

    @Value("${card.images.count}")
    private int allowedImagesCount;

    @MockBean
    private ApiService apiService;

    @MockBean
    private BrokerProducer brokerProducer;

    @Autowired
    private CardService cardService;

    @Spy
    @InjectMocks
    private CardService cardServiceSpy;

    @Test
    void checkInvalidText() {
        assertThrows(CardNotSavedException.class,()->cardService.validateText("","",""));
    }

    @Test
    void checkInvalidToken(){
        when(apiService.validateToken(anyString(), anyString())).thenReturn(false);
        assertThrows(TokenNotExistException.class,()->cardService.checkToken("invalid token"));
    }

    @Test
    void checkUserNotFound(){
        when(apiService.getUserByToken(anyString(),anyString(),anyString())).thenReturn(null);
        when(apiService.getUserById(anyString(),anyString(),anyLong(),anyString())).thenReturn(null);

        assertThrows(UsernameNotFoundException.class,()->cardService.requestToGetUserByToken("invalid token"));
        assertThrows(UsernameNotFoundException.class,()->cardService.requestToGetUserById("invalid token",1L));
    }

    @Test
    void checkWhenImagesNotAddedIntoDB() {
        when(apiService.postRequestToAddCardImage(anyList(), anyString(), anyInt(), anyString(), anyString()))
                .thenReturn(null);

        assertThrows(ImageNotSavedException.class,
                () -> cardService.requestToAddCardImages(new ArrayList<>(), "token"));
    }

    @Test
    void checkBadRequestInRequestToLinkCardAndUser(){
        when(apiService.addCardToUser(anyString(),anyString(),anyString())).thenReturn(HttpStatus.BAD_REQUEST);

        assertThrows(CardNotSavedException.class,()->cardService.requestToLinkCardAndUser(new CardModel(),"token"));
    }

    @Test
    void checkAsyncExecutionAddCardIntoElastic() {

        CardModel cardModel = new CardModel();
        cardModel.setId(1L);
        cardModel.setTitle("Test Title");
        cardModel.setText("Test Text");
        cardModel.setCreateTime(LocalDate.now());

        doNothing().when(brokerProducer).saveInBroker(any(CardDocument.class));

        cardService.addCardIntoElastic(cardModel);

        verify(brokerProducer, timeout(1000)).saveInBroker(any(CardDocument.class));
    }

    @Test
    void checkValidErrorOfComparisonBetweenFileSizeAndAllowedImagesCount() throws TokenNotExistException {
        List<MultipartFile> files = new ArrayList<>();
        for (int i = 0; i <= allowedImagesCount; i++) {
            files.add(new MockMultipartFile("file",new byte[0]));
        }

        doNothing().when(cardServiceSpy).checkToken("token");

        assertThrows(ImageLimitException.class,()->cardServiceSpy.addCard(files,new CardDto(1L,"title","text","tags"),"Bearer token"));

    }
}
