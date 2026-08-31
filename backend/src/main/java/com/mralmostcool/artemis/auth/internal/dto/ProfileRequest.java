package com.mralmostcool.artemis.auth.internal.dto;

import com.mralmostcool.artemis.auth.internal.model.Role;
import lombok.*;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfileRequest {
    private String organizationName;
    private Long organizationId;
    private Role role;
    private String firstName;
    private String lastName;
    private String displayName;
    private String phoneNumber;
    private String gender;
    private LocalDate dateOfBirth;
    private String avatarUrl;
}
