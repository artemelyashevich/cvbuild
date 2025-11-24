package com.bsu.cvbuilder.service;

public interface RedisService {
    void putOtp(String keyUnique, String otp);

    String getOtp(String keyUnique);

    void putLocation(String email, String location);

    String getLocation(String email);
}
