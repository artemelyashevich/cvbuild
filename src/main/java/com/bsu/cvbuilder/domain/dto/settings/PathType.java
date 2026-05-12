package com.bsu.cvbuilder.domain.dto.settings;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PathType {
    SECURED("/secured/"),
    FILES("/files/"),
    TMP("/tmo/");

    private final String pathFragment;
}