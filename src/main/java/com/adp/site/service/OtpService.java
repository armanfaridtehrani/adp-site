package com.adp.site.service;

import com.adp.site.DTO.AuthenticationRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import redis.clients.jedis.JedisPooled;

import java.util.Random;

@Service
@RequiredArgsConstructor
public class OtpService {
    private final JedisPooled jedisPooled;

    public String generateOTP(AuthenticationRequest request) {
        Random random = new Random();
        int generatedOtp = random.nextInt(999999);
        String otp = String.format("%06d", generatedOtp);
        jedisPooled.setex(request.getEmail(), 120, otp);
        return otp;
    }

}
