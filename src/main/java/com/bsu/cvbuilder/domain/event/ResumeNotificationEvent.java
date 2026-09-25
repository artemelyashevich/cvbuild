package com.bsu.cvbuilder.domain.event;

/**
 * Asks for user-facing notifications about a resume. Not an {@link AbstractEvent}, so it is not stored in history.
 *
 * @param login    receiver of the WS notification
 * @param email    receiver of the email, if the kind sends one
 * @param resumeId affected resume, may be {@code null} when generation failed
 */
public record ResumeNotificationEvent(String login, String email, String resumeId, Kind kind) {

    public enum Kind {
        GENERATED, GENERATION_FAILED, ATS_ACCEPTED, ATS_FAILED, FORM_GENERATED
    }
}
