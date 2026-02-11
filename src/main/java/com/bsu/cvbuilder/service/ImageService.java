package com.bsu.cvbuilder.service;

import com.bsu.cvbuilder.domain.entity.ImageMetadata;
import com.mongodb.client.gridfs.model.GridFSFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ImageService {

    List<GridFSFile> findAll();

    List<GridFSFile> findByOwnerId(final String userId);

    byte[] findById(final String id);

    ImageMetadata create(final MultipartFile file, final String userId);

    String upload(final MultipartFile file);
}