package com.bsu.cvbuilder.web.controller.rest;

import com.bsu.cvbuilder.dto.question.ChatTemplateDto;
import com.bsu.cvbuilder.entity.chat.Difficulty;
import com.bsu.cvbuilder.service.QuestionTemplateService;
import com.bsu.cvbuilder.web.mapper.impl.QuestionTemplateMapper;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.mapstruct.factory.Mappers;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Templates")
@RestController
@RequestMapping("/api/v1/templates")
@RequiredArgsConstructor
public class QuestionTemplateController {

    private static final QuestionTemplateMapper mapper = Mappers.getMapper(QuestionTemplateMapper.class);

    private final QuestionTemplateService questionTemplateService;

    @GetMapping
    public Page<ChatTemplateDto> findAll(
            @RequestParam(required = false, defaultValue = "0") Integer page,
            @RequestParam(required = false, defaultValue = "10") Integer size,
            @RequestParam(required = false, defaultValue = "asc") Sort.Direction sort
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(sort));
        return questionTemplateService.findAll(pageable)
                .map(mapper::toDto);
    }

    @GetMapping("/{id}")
    public ChatTemplateDto findById(@PathVariable String id) {
        return mapper.toDto(questionTemplateService.findById(id));
    }

    @GetMapping("/difficulty/{difficulty}")
    public List<ChatTemplateDto> findByDifficulty(@PathVariable Difficulty difficulty) {
        return mapper.toDto(questionTemplateService.findByDifficulty(difficulty));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ChatTemplateDto create(@RequestBody ChatTemplateDto chatTemplateDto) {
        return mapper.toDto(questionTemplateService.create(mapper.toEntity(chatTemplateDto)));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id) {
        questionTemplateService.delete(id);
    }
}
