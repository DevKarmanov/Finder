package karm.van.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.BAD_REQUEST,reason = "User not found")
public class UsernameNotFoundException extends Exception{

    public UsernameNotFoundException(String message){
        super(message);
    }

}
