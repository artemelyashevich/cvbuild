package com.bsu.cvbuilder.web.controller.rest;

import com.bsu.cvbuilder.service.ImageService;
import com.mongodb.client.gridfs.model.GridFSFile;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/v1/images")
@RequiredArgsConstructor
public class ImageController {

    private final ImageService imageService;

    @GetMapping
    public ResponseEntity<List<GridFSFile>> findAll() {
        var images = new ArrayList<GridFSFile>();
        var data = this.imageService.findAll();
        data.forEach(images::add);
        return ResponseEntity.ok(images);
    }

    @GetMapping(value = "/{id}", produces = MediaType.IMAGE_JPEG_VALUE)
    public ResponseEntity<byte[]> findById(final @PathVariable("id") String id) {
        var image = this.imageService.findById(id);
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_JPEG);
        headers.setContentLength(image.length);
        return new ResponseEntity<>(image, headers, HttpStatus.OK);
    }

    @GetMapping("/owner/{ownerId}")
    public ResponseEntity<List<GridFSFile>> findAllByOwnerId(final @PathVariable("ownerId") String userId) {
        var data = this.imageService.findByOwnerId(userId);
        return ResponseEntity.ok(data);
    }

    @PostMapping("/{userId}")
    public ResponseEntity<String> create(
            final @PathVariable("userId") String userId,
            final @RequestParam("file") MultipartFile file,
            final UriComponentsBuilder uriComponentsBuilder
    ) {
        var metadata = this.imageService.create(file, userId);
        return ResponseEntity
                .created(
                        uriComponentsBuilder.path("/api/v1/images/{id}")
                                .build(metadata.getId())
                )
                .body(metadata.getId());
    }

    @PostMapping
    public ResponseEntity<?> save(
            final @RequestParam("file") MultipartFile file,
            final UriComponentsBuilder uriComponentsBuilder

    ) {
        var id = this.imageService.upload(file);
        return ResponseEntity.created(
                uriComponentsBuilder.path("/api/v1/images/{id}")
                        .build(id)
        )
                .body(id);
    }
}