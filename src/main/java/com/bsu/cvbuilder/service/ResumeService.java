package com.bsu.cvbuilder.service;

import com.bsu.cvbuilder.entity.resume.Resume;

import java.util.List;

public interface ResumeService {

    List<Resume> findAll();

    Resume findById(String id);

    Resume create(Resume resume);

    Resume update(Resume resume);
}
