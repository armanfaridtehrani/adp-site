package com.adp.site.service;


import com.adp.site.DTO.AuthenticationRequest;
import com.adp.site.DTO.AuthenticationResponse;
import com.adp.site.DTO.PreAuthenticateResponse;
import com.adp.site.DTO.RegisterRequest;
import com.adp.site.entity.Token;
import com.adp.site.entity.User;
import com.adp.site.enums.TokenType;
import com.adp.site.exceptions.AuthenticationException;
import com.adp.site.repository.TokenRepository;
import com.adp.site.repository.UserRepository;
import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Example;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import redis.clients.jedis.JedisPooled;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final UserRepository repository;
    private final TokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final MailService mailService;
    private final JedisPooled jedisPooled;


    public AuthenticationResponse register(RegisterRequest request) throws AuthenticationException {

        if (repository.exists(Example.of(User.builder().email(request.getEmail()).build()))) {
            throw new AuthenticationException("user already registered");
        }
        passwordValidator(request.getPassword());
        emailValidator(request.getEmail());

        var user = User.builder()
                .firstname(request.getFirstname())
                .lastname(request.getLastname())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .build();
        var savedUser = repository.save(user);
        var jwtToken = jwtService.generateToken(user);
        var refreshToken = jwtService.generateRefreshToken(user);
        DecodedJWT decodedJWT = JWT.decode(jwtToken);
        saveUserToken(savedUser, jwtToken);
        return AuthenticationResponse.builder()
                .accessToken(jwtToken)
                .refreshToken(refreshToken)
                .expireTime(String.valueOf(decodedJWT.getExpiresAt()))
                .build();
    }

    public PreAuthenticateResponse preAuthenticate(AuthenticationRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );
        mailService.sendMail(request);
        return PreAuthenticateResponse.builder()
                .resultMessage("mail send successfully!")
                .build();
    }

    public AuthenticationResponse authenticate(AuthenticationRequest request) throws AuthenticationException {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        var user = repository.findByEmail(request.getEmail())
                .orElseThrow();
        otpValidator(user,request.getOtp());
        var jwtToken = jwtService.generateToken(user);
        var refreshToken = jwtService.generateRefreshToken(user);
        revokeAllUserTokens(user);
        saveUserToken(user, jwtToken);
        DecodedJWT decodedJWT = JWT.decode(jwtToken);
        return AuthenticationResponse.builder()
                .accessToken(jwtToken)
                .refreshToken(refreshToken)
                .expireTime(decodedJWT.getExpiresAt().toString())
                .build();
    }

    private void saveUserToken(User user, String jwtToken) {
        DecodedJWT decodedJWT = JWT.decode(jwtToken);
        var token = Token.builder()
                .user(user)
                .token(jwtToken)
                .tokenType(TokenType.BEARER)
                .expired(false)
                .revoked(false)
                .expireTime(new Timestamp(decodedJWT.getExpiresAt().getTime()))
                .build();
        tokenRepository.save(token);
    }

    private void revokeAllUserTokens(User user) {
        var validUserTokens = tokenRepository.findAllValidTokenByUser(user.getId());
        if (validUserTokens.isEmpty())
            return;
        validUserTokens.forEach(token -> {
            token.setExpired(true);
            token.setRevoked(true);
        });
        tokenRepository.saveAll(validUserTokens);
    }

    public void refreshToken(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        final String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        final String refreshToken;
        final String userEmail;
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return;
        }
        refreshToken = authHeader.substring(7);
        userEmail = jwtService.extractUsername(refreshToken);
        if (userEmail != null) {
            var user = this.repository.findByEmail(userEmail)
                    .orElseThrow();
            if (jwtService.isTokenValid(refreshToken, user)) {
                var accessToken = jwtService.generateToken(user);
                revokeAllUserTokens(user);
                saveUserToken(user, accessToken);
                DecodedJWT decodedJWT = JWT.decode(accessToken);
                var authResponse = AuthenticationResponse.builder()
                        .accessToken(accessToken)
                        .refreshToken(refreshToken)
                        .expireTime(decodedJWT.getExpiresAt().toString())
                        .build();
                new ObjectMapper().writeValue(response.getOutputStream(), authResponse);
            }
        }
    }

    private void emailValidator(String email) throws AuthenticationException {
        boolean isValid= Pattern.compile("^[\\w.!#$%&'*+/=?`{|}~^-]+(?:\\\\.[\\w!#$%&'*+/=?`{|}~^-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,6}",Pattern.CASE_INSENSITIVE).matcher(email).matches();
        if (!isValid){
            throw new AuthenticationException("invalid email!");

        }
    }

    private void passwordValidator(String password) throws AuthenticationException {
        boolean isValid = Pattern.compile("^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[!@#&()–[{}]:;',?/*~$^+=<>]).{8,20}$")
                .matcher(password).matches();
        if (!isValid) {
            throw new AuthenticationException("password does not meet security recommendation!");

        }
    }
    private void otpValidator(User user,String otp) throws AuthenticationException {

        if (otp==null || otp.isEmpty()){
            throw new AuthenticationException("otp is invalid");
        }
        if (user!=null){
           String savedOtp=jedisPooled.get(user.getEmail());
           if (savedOtp==null||savedOtp.isEmpty()){
               throw new AuthenticationException("otp not found");
           }
           if (!otp.equals(savedOtp)){
               throw new AuthenticationException("otp is invalid");
           }else {
               jedisPooled.del(user.getEmail());
           }

        }
    }
}
