package com.bsu.cvbuilder.service.impl;

import com.bsu.cvbuilder.annotation.metrics.Monitored;
import com.bsu.cvbuilder.domain.entity.ImageMetadata;
import com.bsu.cvbuilder.exception.AppException;
import com.bsu.cvbuilder.repository.ImageMetadataRepository;
import com.bsu.cvbuilder.service.ImageService;
import com.mongodb.client.gridfs.model.GridFSFile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
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
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageServiceImpl implements ImageService {

    private final GridFsTemplate gridFsTemplate;
    private final ImageMetadataRepository imageMetadataRepository;

    @Override
    public List<GridFSFile> findAll() {
        List<GridFSFile> files = new ArrayList<>();
        gridFsTemplate.find(new Query()).into(files);
        return files;
    }

    @Override
    @Transactional(readOnly = true)
    public List<GridFSFile> findByOwnerId(final String userId) {
        log.debug("Fetching images for user: {}", userId);

        List<ImageMetadata> metadataList = imageMetadataRepository.findByOwnerId(userId);
        if (metadataList.isEmpty()) {
            return List.of();
        }

        List<ObjectId> ids = metadataList.stream()
                .map(meta -> new ObjectId(meta.getId()))
                .toList();

        List<GridFSFile> files = new ArrayList<>();
        gridFsTemplate.find(Query.query(Criteria.where("_id").in(ids))).into(files);

        log.info("Found {} images for user: {}", files.size(), userId);
        return files;
    }

    @Override
    public byte[] findById(final String id) {
        log.debug("Downloading image: {}", id);

        GridFSFile gridFsFile = Optional.of(
                gridFsTemplate.findOne(Query.query(Criteria.where("_id").is(id)))
        ).orElseThrow(() -> new AppException("Image not found: " + id, 404));

        GridFsResource resource = gridFsTemplate.getResource(gridFsFile);

        if (!resource.exists()) {
            log.error("GridFS file metadata exists but content is missing for id: {}", id);
            throw new AppException("Image content not found", 404);
        }

        try (var inputStream = resource.getInputStream()) {
            return inputStream.readAllBytes();
        } catch (IOException e) {
            log.error("Failed to read image stream for id: {}", id, e);
            throw new AppException("Failed to read image data", e, 500);
        }
    }

    @Override
    @Transactional
    @Monitored(value = "uploading_image", context = "internal")
    public ImageMetadata create(final MultipartFile file, final String userId) {
        log.debug("Creating image for user {}: {}", userId, file.getOriginalFilename());

        String fileId = uploadToGridFs(file);

        ImageMetadata metadata = ImageMetadata.builder()
                .id(fileId)
                .filename(file.getOriginalFilename())
                .contentType(file.getContentType())
                .ownerId(userId)
                .build();

        return imageMetadataRepository.save(metadata);
    }

    @Override
    public String upload(final MultipartFile file) {
        return uploadToGridFs(file);
    }

    private String uploadToGridFs(MultipartFile file) {
        try {
            ObjectId objectId = gridFsTemplate.store(
                    file.getInputStream(),
                    file.getOriginalFilename(),
                    file.getContentType()
            );
            log.info("File uploaded to GridFS with id: {}", objectId);
            return objectId.toString();
        } catch (IOException e) {
            log.error("GridFS storage failure: {}", file.getOriginalFilename(), e);
            throw new AppException("Could not store file: " + file.getOriginalFilename(), e, 500);
        }
    }
}