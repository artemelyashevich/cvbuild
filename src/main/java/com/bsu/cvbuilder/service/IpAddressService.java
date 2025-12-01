package com.bsu.cvbuilder.service;

import jakarta.servlet.http.HttpServletRequest;

public interface IpAddressService {

    String getIpAddress(HttpServletRequest request);
}
