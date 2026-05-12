package com.bsu.cvbuilder.service.impl;

import com.bsu.cvbuilder.domain.entity.ImageMetadata;
import com.bsu.cvbuilder.repository.ImageMetadataRepository;
import com.bsu.cvbuilder.repository.UserProfileRepository;
import com.bsu.cvbuilder.service.ImageService;
import com.bsu.cvbuilder.service.SecurityService;
import com.bsu.cvbuilder.service.UserProfileService;
import com.bsu.cvbuilder.util.CommonUtil;
import com.bsu.cvbuilder.util.MinioUtil;
import com.mongodb.client.gridfs.model.GridFSFile;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.bson.BsonDocument;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@Primary
@RequiredArgsConstructor
public class S3ImageService implements ImageService {

    public static final String BUCKET_NAME = "images";
    private final MinioUtil minioUtil;
    private final ImageMetadataRepository imageMetadataRepository;

    @Override
    @SneakyThrows
    public List<GridFSFile> findAll() {

        List<GridFSFile> result = new ArrayList<>();

        List<Item> objects = minioUtil.getAllObjectsByPrefix(
                BUCKET_NAME,
                "",
                true
        );

        for (Item item : objects) {

            GridFSFile file = new GridFSFile(
                    new BsonDocument(),
                    item.objectName(),
                    item.size(),
                    0,
                    CommonUtil.toDate(item.lastModified().toLocalDate()),
                    null
            );

            result.add(file);
        }

        return result;
    }

    @Override
    @SneakyThrows
    public List<GridFSFile> findByOwnerId(String userId) {

        List<GridFSFile> result = new ArrayList<>();

        String prefix = userId + "/";

        List<Item> objects = minioUtil.getAllObjectsByPrefix(
                BUCKET_NAME,
                prefix,
                true
        );

        for (Item item : objects) {

            GridFSFile file = new GridFSFile(
                    new BsonDocument(),
                    item.objectName(),
                    item.size(),
                    0,
                    CommonUtil.toDate(item.lastModified().toLocalDate()),
                    null
            );

            result.add(file);
        }

        return result;
    }

    @Override
    @SneakyThrows
    public byte[] findById(String id) {

        try (InputStream inputStream = minioUtil.getObject(BUCKET_NAME, id)) {
            return inputStream.readAllBytes();
        }
    }

    @Override
    @SneakyThrows
    public ImageMetadata create(MultipartFile file, String userId) {

        minioUtil.createBucketIfNotExists(BUCKET_NAME);

        String fileName = buildObjectName(userId, file.getOriginalFilename());

        minioUtil.uploadFile(
                BUCKET_NAME,
                file,
                fileName,
                file.getContentType()
        );

        ImageMetadata metadata = ImageMetadata.builder()
                .filename(fileName)
                .contentType(file.getContentType())
                .size(file.getSize())
                .ownerId(userId)
                .url(minioUtil.getPresignedObjectUrl(BUCKET_NAME, fileName))
                .build();

        return imageMetadataRepository.save(metadata);
    }

    @Override
    @SneakyThrows
    public String upload(MultipartFile file) {

        minioUtil.createBucketIfNotExists(BUCKET_NAME);

        String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();

        minioUtil.uploadFile(
                BUCKET_NAME,
                file,
                fileName,
                file.getContentType()
        );

        return minioUtil.getPresignedObjectUrl(BUCKET_NAME, fileName);
    }

    private String buildObjectName(String userId, String originalFilename) {

        return userId
                + "/"
                + UUID.randomUUID()
                + "_"
                + originalFilename;
    }
}
