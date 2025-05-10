package karm.van.controller;

import io.swagger.v3.oas.annotations.Hidden;
import karm.van.exception.card.CardNotDeletedException;
import karm.van.exception.card.CardNotFoundException;
import karm.van.exception.card.CardNotSavedException;
import karm.van.exception.card.CardNotUnlinkException;
import karm.van.exception.comment.CommentNotDeletedException;
import karm.van.exception.image.*;
import karm.van.exception.other.SerializationException;
import karm.van.exception.other.ServerException;
import karm.van.exception.other.TokenNotExistException;
import karm.van.exception.user.NotEnoughPermissionsException;
import karm.van.exception.user.UsernameNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Hidden
@RestControllerAdvice
public class ExceptionHandlerController {


    @ExceptionHandler(CardNotSavedException.class)
    public ResponseEntity<String> cardNotSavedException(CardNotSavedException ex){
        return new ResponseEntity<>(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ImageLimitException.class)
    public ResponseEntity<String> imageLimitException(ImageLimitException ex){
        return new ResponseEntity<>(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(SerializationException.class)
    public ResponseEntity<String> serializationException(SerializationException ex){
        return new ResponseEntity<>(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<String> handleHttpMessageNotReadableException() {
        String message = "Data entry error. Check if the field values are correct";
        return ResponseEntity.badRequest().body(message);
    }

    @ExceptionHandler(TokenNotExistException.class)
    public ResponseEntity<String> tokenNotExistException(TokenNotExistException ex){
        return new ResponseEntity<>(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(NotEnoughPermissionsException.class)
    public ResponseEntity<String> notEnoughPermissionsException(NotEnoughPermissionsException ex){
        return new ResponseEntity<>(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<String> usernameNotFoundException(UsernameNotFoundException ex){
        return new ResponseEntity<>(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(CardNotFoundException.class)
    public ResponseEntity<String> cardNotFoundException(CardNotFoundException ex){
        return new ResponseEntity<>(ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(ImageNotFoundException.class)
    public ResponseEntity<String> imageNotFoundException(ImageNotFoundException ex){
        return new ResponseEntity<>(ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(ImageNotDeletedException.class)
    public ResponseEntity<String> imageDeleteException(ImageNotDeletedException ex){
        return new ResponseEntity<>(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(CardNotUnlinkException.class)
    public ResponseEntity<String> cardNotUnlinkException(CardNotUnlinkException ex){
        return new ResponseEntity<>(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(ImageUrlNotGetted.class)
    public ResponseEntity<String> imageUrlNotGetted(ImageUrlNotGetted ex){
        return new ResponseEntity<>(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
    }


    @ExceptionHandler(CommentNotDeletedException.class)
    public ResponseEntity<String> commentNotDeletedException(CommentNotDeletedException ex){
        return new ResponseEntity<>(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(ImageNotSavedException.class)
    public ResponseEntity<String> imageLoadException(ImageNotSavedException ex){
        return new ResponseEntity<>(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(ServerException.class)
    public ResponseEntity<String> serverException(ServerException ex){
        return new ResponseEntity<>(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(CardNotDeletedException.class)
    public ResponseEntity<String> cardNotDeletedException(CardNotDeletedException ex){
        return new ResponseEntity<>(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(ImageNotMovedException.class)
    public ResponseEntity<String> imageNotMovedException(ImageNotMovedException ex){
        return new ResponseEntity<>(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

}
