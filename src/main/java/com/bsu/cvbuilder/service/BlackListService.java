package com.bsu.cvbuilder.service;

public interface BlackListService {

    void banToken(String token);

    Boolean validate(String token);
}
