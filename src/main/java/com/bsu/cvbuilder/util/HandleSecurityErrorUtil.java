package com.bsu.cvbuilder.util;

import com.bsu.cvbuilder.domain.dto.exception.ExceptionBodyDto;
import jakarta.servlet.http.HttpServletResponse;
import lombok.experimental.UtilityClass;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.util.Objects;

@UtilityClass
public class HandleSecurityErrorUtil {

    public HttpServletResponse handleError(HttpServletResponse response, Exception e) throws IOException {
        response.setStatus(401);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        var res = JsonHelper.toJson(new ExceptionBodyDto(e.getMessage()));
        response.getWriter().write(Objects.requireNonNull(res));
        return response;
    }
}