package com.bsu.cvbuilder.service;

import jakarta.servlet.http.HttpServletRequest;

public interface IpAddressService {

    public String getIpAddress(HttpServletRequest request);
}
