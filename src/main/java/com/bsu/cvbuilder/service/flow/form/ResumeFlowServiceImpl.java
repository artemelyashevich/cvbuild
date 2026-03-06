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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

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
        List<ResumeFlowStep> roadmap = ResumeFlowStep.getResumeFlowRoadmap();
        Map<String, Object> steps = new LinkedHashMap<>();

        for (ResumeFlowStep step : roadmap) {
            List<FollowUpQuestion> questions = buildQuestions(step, userProfile);
            Map<String, Object> stepData = Map.of(
                    "voiceInput", step.getVoiceInput(),
                    "questions", questions
            );
            steps.put(step.name().toLowerCase(), stepData);
        }

        return steps;
    }

    @Override
    public Resume generateResume(ResumePayload resumePayload, UserProfile userProfile) {
        log.debug("Attempting to generate resume for user: {}", userProfile.getLogin());

        Resume resume = Resume.builder()
                .generatedWithChat(false)
                .resumeSettings(Resume.ResumeSettings.builder()
                        .name("resume_%s_%s".formatted(userProfile.getLogin(), LocalDate.now()))
                        .ownerId(userProfile.getId())
                        .ownerLogin(userProfile.getLogin())
                        .build())
                .build();

        // Personal Information
        Map<String, Object> personalInfos = Map.of(
                "First name", resumePayload.personalInformation().get("firstName"),
                "Last name", resumePayload.personalInformation().get("lastName"),
                "Email", resumePayload.personalInformation().get("email"),
                "Phone", resumePayload.personalInformation().get("phone")
        );

        // Links
        Map<String, Object> links = Map.of(
                "Linkedin", resumePayload.links().get("linkedin"),
                "Github", resumePayload.links().get("github"),
                "Portfolio", resumePayload.links().get("portfolio")
        );

        // Job Experience
        List<Map<String, String>> jobList = resumePayload.job().stream()
                .map(j -> Map.of(
                        "Company", j.get("company"),
                        "Position", j.get("position"),
                        "Job period", j.get("jobPeriod")
                ))
                .toList();

        // Education
        List<Map<String, String>> educationList = resumePayload.education().stream()
                .map(ed -> Map.of(
                        "University", ed.get("university"),
                        "Degree", ed.get("degree"),
                        "Education Period", ed.get("educationPeriod")
                ))
                .toList();

        // Skills, Highlights, Goals
        Map<String, Object> skills = Map.of("Skills", resumePayload.skills());
        Map<String, Object> highlights = Map.of("Highlights", resumePayload.highlights());
        Map<String, Object> goals = Map.of("Goals", resumePayload.careerGoals());

        String userContent = JsonHelper.toJson(personalInfos) +
                JsonHelper.toJson(links) +
                JsonHelper.toJson(jobList) +
                JsonHelper.toJson(educationList) +
                JsonHelper.toJson(skills) +
                JsonHelper.toJson(goals) +
                JsonHelper.toJson(highlights);

        CompletableFuture<Object> skillsFuture = aiService.callFlow("skills", userContent);

        CompletableFuture<Object> jobFuture = aiService.callFlow("job", userContent);

        CompletableFuture<Object> goalFuture = aiService.callFlow("goals", userContent);

        CompletableFuture.allOf(skillsFuture, jobFuture, goalFuture).join();

        skills = Map.of("Skills", skillsFuture.join());
        //jobList.add(Map.of("Job AI Suggestion", jobFuture.join().toString()));
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
        log.info("Resume for user: {} finished", userProfile.getLogin());
        return resumeService.save(resume);
    }

    public static List<FollowUpQuestion> buildQuestions(
            ResumeFlowStep step,
            UserProfile user
    ) {

        List<FollowUpQuestion> result = new ArrayList<>();

        for (FollowUpQuestion question : step.getQuestions()) {

            ResumeField field = question.getField();

            boolean alreadyFilled = isAlreadyFilled(field, user);

            if (alreadyFilled) {
                continue;
            }

            result.add(question);
        }

        return result;
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
