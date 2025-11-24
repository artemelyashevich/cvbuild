package com.bsu.cvbuilder.web.mapper;

import java.util.List;

public interface Mappable<E, D> {
    E toEntity(D dto);
    D toDto(E dto);
    List<D> toDtoList(List<E> dtoList);
}
