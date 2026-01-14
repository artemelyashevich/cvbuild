package com.bsu.cvbuilder.domain.entity.user;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@Document("userStats")
@AllArgsConstructor
@NoArgsConstructor
public class UserStats {

    @Id
    private String id;

    private String userId;

    @Builder.Default
    private Integer resumesCreated = 0;
    
    @Builder.Default
    private Integer aiRequestsUsed = 0;
    
    @Builder.Default
    private Integer totalDownloads = 0;
    
    @Builder.Default
    private Integer totalViews = 0;

    @Builder.Default
    private MonthlyUsage currentMonthUsage = new MonthlyUsage();

    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthlyUsage {
        @Builder.Default
        private Integer aiRequests = 0;

        @Builder.Default
        private Integer jobAnalyses = 0;

        @Builder.Default
        private Integer resumesCreated = 0;

        private LocalDateTime periodStart = LocalDateTime.now().withDayOfMonth(1);
    }

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @CreatedDate
    private LocalDateTime createdAt;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @LastModifiedDate
    private LocalDateTime updatedAt;
}
