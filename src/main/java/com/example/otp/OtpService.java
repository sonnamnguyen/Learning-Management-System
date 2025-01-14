package com.example.otp;

import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class OtpService {
    private final Map<String, String> otpStorage = new HashMap<>();

    public String generateOtp(String email) {
        String otp = String.valueOf(new Random().nextInt(999999));
        otpStorage.put(email, otp);
        // Mã OTP hết hạn sau 2 phút
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                otpStorage.remove(email);
            }
        }, 2 * 60 * 1000);
        return otp;
    }

    public boolean validateOtp(String email, String otp) {
        return otp.equals(otpStorage.get(email));
    }
}
