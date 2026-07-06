package com.mchat.auth;

import io.quarkus.elytron.security.common.BcryptUtil;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class PasswordService {

    public Uni<String> hashPassword(String plainPassword) {
        return Uni.createFrom().item(() -> BcryptUtil.bcryptHash(plainPassword));
    }
}