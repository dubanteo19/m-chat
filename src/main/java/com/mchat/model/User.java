package com.mchat.model;

import java.util.List;

import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.mchat.model.json.PushSubscription;
import com.mchat.model.json.TitleStyle;

import io.quarkus.elytron.security.common.BcryptUtil;
import io.quarkus.hibernate.reactive.panache.PanacheEntity;
import io.smallrye.mutiny.Uni;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "chat_users")
public class User extends PanacheEntity {

  @Column(unique = true, nullable = false)
  public String username;

  @Column(nullable = false)
  public String password;

  @Column(nullable = false)
  public String displayName;

  public String title;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "title_style")
  public TitleStyle titleStyle;

  public String avatarUrl;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "push_subscription")
  public PushSubscription pushSubscription;

  @Column(name = "allow_notify", nullable = false)
  @ColumnDefault("true")
  public boolean allowNotify = true;
  public User() {
  }

  public User(Long id) {
    this.id = id;
  }

  public User(
      String username,
      String password,
      String displayName,
      String avatarUrl,
      String title,
      TitleStyle titleStyle) {
    this.username = username;
    this.password = password;
    this.displayName = displayName;
    this.avatarUrl = avatarUrl;
    this.title = title;
    this.titleStyle = titleStyle;
  }

  public static Uni<User> findByUsername(String username) {
    return find("username", username).firstResult();
  }

  public static Uni<User> findById(Long userId) {
    return find("id", userId).firstResult();
  }

  public static Uni<List<User>> searchByDisplayName(String displayName) {
    return find(
        "displayName ILIKE ?1",
        "%" + displayName + "%")
        .page(0, 20)
        .list();
  }

  public void setAndHashPassword(String plainPassword) {
    this.password = BcryptUtil.bcryptHash(plainPassword);
  }

  public boolean checkPassword(String plainPassword) {
    return BcryptUtil.matches(plainPassword, this.password);
  }
}
