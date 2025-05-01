package karm.van.controller;

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

    @GetMapping("{id}/get")
    public FullCardDtoForOutput getCard(@PathVariable Long id, @RequestHeader("Authorization") String authorization) throws CardNotFoundException, SerializationException, TokenNotExistException, UsernameNotFoundException {
        return cardService.getCard(id,authorization);
    }

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

    @GetMapping("getAll/{pageNumber}/{limit}")
    public CardPageResponseDto getAllCards(@PathVariable int pageNumber,
                                           @PathVariable int limit,
                                           @RequestHeader("Authorization") String authorization) throws TokenNotExistException, SerializationException {
        return cardService.getAllCards(pageNumber,limit,authorization);
    }

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

    @PostMapping(value = "add", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public void addCard(@RequestPart("cardDto") CardDto cardDto,
                        @RequestPart("files") List<MultipartFile> files,
                        @RequestHeader("Authorization") String authorization) throws ImageNotSavedException, CardNotSavedException, ImageLimitException, TokenNotExistException, UsernameNotFoundException {
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

    @PatchMapping(value = "{id}/patch",consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public void patchCard(@PathVariable Long id,
                          @RequestPart(value = "cardDto",required = false) Optional<CardDto> cardDto,
                          @RequestPart(value = "files",required = false) Optional<List<MultipartFile>> files,
                          @RequestHeader("Authorization") String authorization) throws CardNotFoundException, CardNotSavedException, ImageNotSavedException, ImageLimitException, TokenNotExistException, NotEnoughPermissionsException {
        try {
            cardService.patchCard(id,cardDto,files,authorization);
        } catch (CardNotFoundException | CardNotSavedException | ImageNotSavedException | ImageLimitException | TokenNotExistException | NotEnoughPermissionsException e) {
            throw e;
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
    }

    @DeleteMapping("/del/{id}")
    public void delCard(@PathVariable Long id, @RequestHeader("Authorization") String authorization) throws TokenNotExistException, CardNotFoundException, NotEnoughPermissionsException, CardNotDeletedException {
        try {
            cardService.deleteCard(id,authorization);
        } catch (CommentNotDeletedException | ImageNotMovedException | CardNotDeletedException e) {
            throw new CardNotDeletedException("Due to an error, the ad was not deleted");
        } catch (TokenNotExistException | CardNotFoundException | NotEnoughPermissionsException e){
            throw e;
        } catch (Exception e) {
            log.error("class: "+e.getClass()+" message: "+e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
    }

    @DeleteMapping("/image/del/{cardId}/{imageId}")
    public void delOneImageFromCard(@PathVariable Long cardId,
                                    @PathVariable Long imageId,
                                    @RequestHeader("Authorization") String authorization) throws CardNotFoundException, TokenNotExistException, ImageNotDeletedException, UsernameNotFoundException, NotEnoughPermissionsException {
        cardService.delOneImageInCard(cardId,imageId,authorization);
    }
}



