package com.mralmostcool.artemis.auth.internal.dto;

import com.mralmostcool.artemis.auth.internal.model.Role;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfileRequest {
    private String organizationName;
    private Long organizationId;
    private Role role;
}
