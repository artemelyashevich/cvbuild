package com.bsu.cvbuilder.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResumeData implements Serializable {

    @JsonProperty("personal_info")
    private PersonalInfo personalInfo;

    private String summary;

    @JsonProperty("technical_skills")
    private List<String> technicalSkills;

    private List<Experience> experience;

    private List<Education> education;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PersonalInfo {
        @JsonProperty("full_name")
        private String fullName;

        @JsonProperty("current_role")
        private String currentRole;

        private Contacts contacts;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Contacts {
        private String email;
        private String phone;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Experience {
        private String company;
        private String position;
        private String duration;

        @JsonProperty("key_achievements")
        private List<String> keyAchievements;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Education {
        private String institution;
        private String degree;
        private Integer year;
    }
}
