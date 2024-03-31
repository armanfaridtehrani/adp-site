package com.adp.site.exceptionHandlers;

import com.adp.site.exceptions.AuthenticationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class AuthenticationExceptionHandler {
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(AuthenticationException.class)
    public Map<String,String> handleAuthenticationException(AuthenticationException authenticationException){
        Map<String,String> response= new HashMap<>();
        response.put("error Message",authenticationException.getMessage());
        return response;
    }
}
