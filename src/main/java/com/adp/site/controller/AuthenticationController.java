package com.adp.site.controller;

import com.adp.site.DTO.AuthenticationRequest;
import com.adp.site.DTO.AuthenticationResponse;
import com.adp.site.DTO.RegisterRequest;
import com.adp.site.exceptions.AuthenticationException;
import com.adp.site.service.AuthenticationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import redis.clients.jedis.JedisPooled;

import java.io.IOException;
import java.util.Random;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthenticationController {

  private final AuthenticationService service;
  private final JedisPooled jedisPooled;
  private final JavaMailSender javaMailSender;

  @PostMapping("/register")
  public ResponseEntity<AuthenticationResponse> register(
      @RequestBody RegisterRequest request
  ) throws AuthenticationException {
    try {
      return ResponseEntity.ok(service.register(request));
    } catch (AuthenticationException e) {
      throw new AuthenticationException(e.getMessage());
    }
  }
  @PostMapping("/authenticate")
  public ResponseEntity<AuthenticationResponse> authenticate(
      @RequestBody AuthenticationRequest request
  ) {
    return ResponseEntity.ok(service.authenticate(request));
  }

  @PostMapping("/refresh-token")
  public void refreshToken(
      HttpServletRequest request,
      HttpServletResponse response
  ) throws IOException {
    service.refreshToken(request, response);
  }
  @PostMapping("/sendMail")
  public void sendMail(
          HttpServletResponse response,@RequestBody AuthenticationRequest request
  ) throws IOException {
    SimpleMailMessage message=new SimpleMailMessage();
    message.setTo(request.getEmail());
    message.setSubject("auth otp");
    Random random=new Random();
    int generatedOtp = random.nextInt(999999);
    String otp = String.format("%06d", generatedOtp);
    jedisPooled.setex(request.getEmail(),120,otp);
    message.setText("your otp is:  "+ otp );
    javaMailSender.send(message);

  }



}
