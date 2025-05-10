package karm.van.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.BAD_REQUEST,reason = "an error occurred during serialization")
public class SerializationException extends Exception{

    public SerializationException(String message){
        super(message);
    }

}
