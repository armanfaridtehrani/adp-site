package com.adp.site.service;

import com.adp.site.DTO.AuthenticationRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MailService {
    private final JavaMailSender javaMailSender;
    private final OtpService otpService;

    public void sendMail(AuthenticationRequest request) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(request.getEmail());
        message.setSubject("auth otp");
        String otp = otpService.generateOTP(request);
        message.setText("your otp is:  " + otp);
        javaMailSender.send(message);
    }
}
