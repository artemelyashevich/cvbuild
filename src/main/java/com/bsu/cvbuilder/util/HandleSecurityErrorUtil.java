package com.bsu.cvbuilder.util;

import com.bsu.cvbuilder.dto.exception.ExceptionBodyDto;
import com.bsu.cvbuilder.exception.AppException;
import jakarta.servlet.http.HttpServletResponse;
import lombok.experimental.UtilityClass;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.util.Objects;

@UtilityClass
public class HandleSecurityErrorUtil {

    public HttpServletResponse handleError(HttpServletResponse response, AppException e) throws IOException {
        response.setStatus(e.getStatusCode());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        var res = JsonHelper.toJson(new ExceptionBodyDto(e.getMessage()));
        response.getWriter().write(Objects.requireNonNull(res));
        return response;
    }
}