package com.mralmostcool.artemis.auth.internal.dto;

import com.mralmostcool.artemis.auth.internal.model.Role;
import lombok.*;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfileResponse {
    private UUID id;
    private String email;
    private Role role;
    private Long organizationId;
    private String organizationName;
    private String displayName;
    private String phoneNumber;
    private boolean enabled;
}
