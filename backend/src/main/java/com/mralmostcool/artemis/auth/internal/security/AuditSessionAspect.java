package com.mralmostcool.artemis.auth.internal.security;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.Authentication;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;
import java.util.UUID;

@Aspect
@Component
public class AuditSessionAspect {

    @PersistenceContext
    private EntityManager entityManager;

    @Before("@annotation(org.springframework.transaction.annotation.Transactional) || @within(org.springframework.transaction.annotation.Transactional)")
    public void setAuditUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal) {
            UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
            UUID userId = principal.getProfile() != null ? principal.getProfile().getId() : UUID.fromString(principal.getJwt().getSubject());
            try {
                entityManager.createNativeQuery("SET LOCAL app.current_user_id = :userId")
                        .setParameter("userId", userId.toString())
                        .executeUpdate();
            } catch (Exception e) {
                // Silently ignore if not in a transaction or connection error
            }
        }
    }
}
