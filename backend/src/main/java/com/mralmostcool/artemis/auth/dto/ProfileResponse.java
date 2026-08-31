package com.mralmostcool.artemis.auth.dto;

import com.mralmostcool.artemis.auth.model.Role;
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
}
