package com.bsu.cvbuilder.util;

import lombok.experimental.UtilityClass;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

@UtilityClass
public final class CommonUtil {

    public static Date toDate(LocalDate localDate) {
        return Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }
}
