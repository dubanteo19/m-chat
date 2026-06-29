
package com.mchat.user;

import com.mchat.model.User;

import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class UserService {

  @WithTransaction
  public Uni<User> findByUsername(String username) {
    return User.findByUsername(username);
  }
}
