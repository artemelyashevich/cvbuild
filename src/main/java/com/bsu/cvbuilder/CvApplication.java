package com.bsu.cvbuilder;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.data.web.config.EnableSpringDataWebSupport;

import static org.springframework.data.web.config.EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO;

@SpringBootApplication
@EnableMongoRepositories(basePackages = "com.bsu.cvbuilder.repository")
@EnableSpringDataWebSupport(pageSerializationMode = VIA_DTO)
public class CvApplication {

    public static void main(String[] args) {
        SpringApplication.run(CvApplication.class, args);
    }

}
