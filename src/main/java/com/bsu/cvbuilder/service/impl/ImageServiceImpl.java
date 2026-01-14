package com.bsu.cvbuilder.service.impl;

import com.bsu.cvbuilder.domain.entity.image.ImageMetadata;
import com.bsu.cvbuilder.exception.AppException;
import com.bsu.cvbuilder.repository.ImageMetadataRepository;
import com.bsu.cvbuilder.service.ImageService;
import com.mongodb.client.gridfs.GridFSFindIterable;
import com.mongodb.client.gridfs.model.GridFSFile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


@Slf4j
@Service
@RequiredArgsConstructor
public class ImageServiceImpl implements ImageService {

    private final GridFsTemplate gridFsTemplate;
    private final ImageMetadataRepository imageMetadataRepository;

    @Override
    public GridFSFindIterable findAll() {
        return this.gridFsTemplate.find(null);
    }

    @Transactional
    @Override
    public List<GridFSFile> findByOwnerId(final String userId) {
        log.debug("Finding images by user id {}", userId);
        var imageMetadata = imageMetadataRepository.findByOwnerId(userId);
        var data = new ArrayList<GridFSFile>();
        imageMetadata.forEach(image -> data.add(this.gridFsTemplate.findOne(Query.query(Criteria.where("_id").is(image.getId())))));
        log.info("Images found: {}", data.size());
        return data;
    }

    @Override
    public byte[] findById(final String id) {
        log.debug("Finding image by id {}", id);
        var gridFsFile = gridFsTemplate.findOne(Query.query(Criteria.where("_id").is(id)));
        byte[] bytes = null;
        GridFsResource resource = gridFsTemplate.getResource(gridFsFile);
        try (var inputStream = resource.getInputStream()) {
            bytes = inputStream.readAllBytes();
        } catch (IOException e) {
            throw new AppException(e, 500);
        }
        log.info("Images found: {}", bytes.length);
        return bytes;
    }

    @Transactional
    @Override
    public ImageMetadata create(final MultipartFile file, final String userId) {
        log.debug("Creating image by user id {}", userId);
        try {
            var id = gridFsTemplate.store(file.getInputStream(), file.getOriginalFilename(), file.getContentType());
            var image = ImageMetadata.builder()
                    .id(id.toString())
                    .filename(file.getOriginalFilename())
                    .contentType(file.getContentType())
                    .ownerId(userId)
                    .build();

            var result = this.imageMetadataRepository.save(image);
            log.info("Image created: {}", image);
            return result;
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            throw new AppException("Error uploading file", e, 500);
        }
    }

    @Override
    public String upload(final MultipartFile file) {
        log.debug("Uploading image by user id {}", file.getOriginalFilename());
        String id = null;
        try {
            var imageId = gridFsTemplate.store(file.getInputStream(), file.getOriginalFilename(), file.getContentType());
            id = imageId.toString();
            log.info("Image uploaded: {}", imageId);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return id;
    }
}