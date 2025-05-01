package karm.van.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.BAD_REQUEST,reason = "You don't have permission to do this")
public class NotEnoughPermissionsException extends Exception{

    public NotEnoughPermissionsException(String message){
        super(message);
    }

}
