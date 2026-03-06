package com.bsu.cvbuilder.service.flow.form;

import com.bsu.cvbuilder.domain.entity.Resume;
import com.bsu.cvbuilder.domain.entity.UserProfile;
import com.bsu.cvbuilder.service.AiService;
import com.bsu.cvbuilder.service.ResumeService;
import com.bsu.cvbuilder.service.SecurityService;
import com.bsu.cvbuilder.service.flow.form.domain.FollowUpQuestion;
import com.bsu.cvbuilder.service.flow.form.domain.ResumeField;
import com.bsu.cvbuilder.service.flow.form.domain.ResumeFlowStep;
import com.bsu.cvbuilder.service.flow.form.domain.ResumePayload;
import com.bsu.cvbuilder.util.JsonHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeFlowServiceImpl implements ResumeFlowService {

    private final SecurityService securityService;
    private final ResumeService resumeService;
    private final AiService aiService;

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
        log.debug("Generating resume for user: {}", user.getLogin());

        Resume resume = createEmptyResume(user);

        Map<String, Object> personalInfos = extractPersonalInformation(payload);
        Map<String, Object> links = extractLinks(payload);
        List<Map<String, String>> jobList = extractJobExperience(payload);
        List<Map<String, String>> educationList = extractEducation(payload);
        Map<String, Object> skills = Map.of("Skills", payload.skills());
        Map<String, Object> highlights = Map.of("Highlights", payload.highlights());
        Map<String, Object> goals = Map.of("Goals", payload.careerGoals());

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

        skills = Map.of("Skills", skillsFuture.join());
        goals = Map.of("Career Goals", goalFuture.join());

        Map<String, Object> blocks = new LinkedHashMap<>();
        blocks.put("Personal Information", personalInfos);
        blocks.put("Professional Media", links);
        blocks.put("Job Experience", jobList);
        blocks.put("Education", educationList);
        blocks.put("Skills", skills);
        blocks.put("Key Highlights", highlights);
        blocks.put("Career Goals", goals);

        resume.setBlocks(blocks);

        log.info("Resume generation finished for user: {}", user.getLogin());
        return resumeService.save(resume);
    }

    private Resume createEmptyResume(UserProfile user) {
        return Resume.builder()
                .generatedWithChat(false)
                .resumeSettings(Resume.ResumeSettings.builder()
                        .name(String.format("resume_%s_%s", user.getLogin(), LocalDate.now()))
                        .ownerId(user.getId())
                        .ownerLogin(user.getLogin())
                        .build())
                .build();
    }

    private Map<String, Object> extractPersonalInformation(ResumePayload payload) {
        return Map.of(
                "First name", payload.personalInformation().get("firstName"),
                "Last name", payload.personalInformation().get("lastName"),
                "Email", payload.personalInformation().get("email"),
                "Phone", payload.personalInformation().get("phone")
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