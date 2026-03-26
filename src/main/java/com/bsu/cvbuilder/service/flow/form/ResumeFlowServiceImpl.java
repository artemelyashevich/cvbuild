package com.bsu.cvbuilder.service.flow.form;

import com.bsu.cvbuilder.domain.entity.Resume;
import com.bsu.cvbuilder.domain.entity.UserProfile;
import com.bsu.cvbuilder.service.*;
import com.bsu.cvbuilder.service.flow.form.domain.FollowUpQuestion;
import com.bsu.cvbuilder.service.flow.form.domain.ResumeField;
import com.bsu.cvbuilder.service.flow.form.domain.ResumeFlowStep;
import com.bsu.cvbuilder.service.flow.form.domain.ResumePayload;
import com.bsu.cvbuilder.util.JsonHelper;
import com.bsu.cvbuilder.util.MaskUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeFlowServiceImpl implements ResumeFlowService {

    private final SecurityService securityService;
    private final ResumeService resumeService;
    private final AiService aiService;
    private final JobParserService jobParserService;
    private final AnalyzerService analyzerService;

    @Override
    public Map<String, Object> getResumeFlowRoadmap() {
        UserProfile userProfile = securityService.findCurrentUser();

        return ResumeFlowStep.getResumeFlowRoadmap().stream()
                .collect(Collectors.toMap(
                        step -> step.name().toLowerCase(),
                        step -> Map.of(
                                "voiceInput", step.getVoiceInput(),
                                "questions", buildQuestions(step, userProfile)
                        ),
                        (a, b) -> b,
                        LinkedHashMap::new
                ));
    }

    @Override
    public Resume generateResume(ResumePayload payload, UserProfile user) {
        log.debug("[RESUME-FLOW] Generating resume for user: {}", user.getLogin());

        Resume resume = createEmptyResume(payload.name(), user);

        Map<String, Object> personalInfos = extractPersonalInformation(payload, user);
        Map<String, Object> links = extractLinks(payload);
        List<Map<String, String>> jobList = extractJobExperience(payload);
        List<Map<String, String>> educationList = extractEducation(payload);
        Map<String, Object> skills = Map.of("Skills", payload.skills());
        Map<String, Object> highlights = Map.of("Highlights", payload.highlights());
        Map<String, Object> goals = Map.of("Goals", payload.careerGoals());

        Map<String, Object> blocks = new LinkedHashMap<>();

        String userContent = JsonHelper.toJson(personalInfos)
                + JsonHelper.toJson(links)
                + JsonHelper.toJson(jobList)
                + JsonHelper.toJson(educationList)
                + JsonHelper.toJson(skills)
                + JsonHelper.toJson(goals)
                + JsonHelper.toJson(highlights);

        CompletableFuture<Object> skillsFuture = aiService.callFlow("skills", userContent);
        CompletableFuture<Object> jobFuture = aiService.callFlow("job", userContent);
        CompletableFuture<Object> goalFuture = aiService.callFlow("goals", userContent);

        CompletableFuture.allOf(skillsFuture, jobFuture, goalFuture).join();

        String skillsContent = (String) skillsFuture.join();
        String summary = (String) goalFuture.join();
        List<Object> job = (List<Object>) JsonHelper.fromJson(((String) jobFuture.join()), List.class);

        blocks.put("Personal Information", personalInfos);
        blocks.put("Professional Media", links);
        blocks.put("Summary", summary);
        blocks.put("Education", educationList);
        blocks.put("Skills", skillsContent);
        blocks.put("Key Highlights", highlights);
        blocks.put("Job Experience", job);

        resume.setBlocks(blocks);

        log.info("[RESUME-FLOW] Resume generation finished for user: {}", user.getLogin());
        return resumeService.save(resume);
    }

    @Override
    public Resume regenerateField(String resumeId, ResumeField resumeField) {
        log.debug("[RESUME-FLOW] Attempting regenerate field: {} for resume: {}", resumeField, resumeId);
        Resume resume = resumeService.findById(resumeId);
        Object block = resume.getBlocks().get(resumeField.name());
        return null;
    }

    @Override
    public Resume ats(String resumeId, String jobLink) {
        log.debug("[RESUME-FLOW] Attempting ats for resume: {} for job: {}", resumeId, MaskUtil.mask(jobLink, 10));
        String jobDescription = jobParserService.parse(jobLink);
        Resume resume = resumeService.findById(resumeId);
        UserProfile userProfile = securityService.findCurrentUser();
        analyzerService.ats(resume, jobDescription, userProfile);
        return resume;
    }

    private Resume createEmptyResume(String name, UserProfile user) {
        if (name == null) {
            name = String.format("resume__%s__%s__%s", user.getLogin(), LocalDate.now(), UUID.randomUUID());
        }
        return Resume.builder()
                .generatedWithChat(false)
                .resumeSettings(Resume.ResumeSettings.builder()
                        .name(name)
                        .ownerId(user.getId())
                        .ownerLogin(user.getLogin())
                        .build())
                .build();
    }

    private Map<String, Object> extractPersonalInformation(ResumePayload payload, UserProfile user) {
        String firstName = payload.personalInformation().get("firstName");
        String lastName = payload.personalInformation().get("lastName");
        String email = payload.personalInformation().get("email");
        String phone = payload.personalInformation().get("phone");
        if (firstName == null || firstName.isEmpty()) {
            firstName = user.getFirstName();
        }
        if (lastName == null || lastName.isEmpty()) {
            lastName = user.getLastName();
        }
        if (email == null || email.isEmpty()) {
            email = user.getEmail();
        }
        if (phone == null || phone.isEmpty()) {
            phone = user.getPhoneNumber();
        }
        return Map.of(
                "First name", firstName,
                "Last name", lastName,
                "Email", email,
                "Phone", phone
        );
    }

    private Map<String, Object> extractLinks(ResumePayload payload) {
        return Map.of(
                "Linkedin", payload.links().get("linkedin"),
                "Github", payload.links().get("github"),
                "Portfolio", payload.links().get("portfolio")
        );
    }

    private List<Map<String, String>> extractJobExperience(ResumePayload payload) {
        return payload.job().stream()
                .map(j -> Map.of(
                        "Company", j.get("company"),
                        "Position", j.get("position"),
                        "Job period", j.get("jobPeriod")
                ))
                .toList();
    }

    private List<Map<String, String>> extractEducation(ResumePayload payload) {
        return payload.education().stream()
                .map(ed -> Map.of(
                        "University", ed.get("university"),
                        "Degree", ed.get("degree"),
                        "Education Period", ed.get("educationPeriod")
                ))
                .toList();
    }

    public static List<FollowUpQuestion> buildQuestions(ResumeFlowStep step, UserProfile user) {
        return step.getQuestions().stream()
                .filter(q -> !isAlreadyFilled(q.getField(), user))
                .toList();
    }

    private static boolean isAlreadyFilled(ResumeField field, UserProfile user) {
        return switch (field) {
            case FIRST_NAME -> user.getFirstName() != null;
            case LAST_NAME -> user.getLastName() != null;
            case EMAIL -> user.getEmail() != null;
            case PHONE -> user.getPhoneNumber() != null;
            default -> false;
        };
    }
}