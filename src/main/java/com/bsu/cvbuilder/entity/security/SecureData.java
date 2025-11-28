package com.bsu.cvbuilder.entity.security;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Document(collection = "secureData")
public class SecureData {

    private String id;

    @DBRef(db = "users")
    private String userId;

    @ToString.Exclude
    private SecureInfo data;

    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SecureInfo {
        private String passwordSalt;
        private String passwordHash;
        private String ipAddress;
        private String country;
    }
}
