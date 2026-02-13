package com.bsu.cvbuilder.service;

import java.util.Date;

public interface BlackListService {

    void banToken(String token, Date expiration);

    Boolean validate(String token);
}
