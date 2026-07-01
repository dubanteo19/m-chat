package com.mchat.model;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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

  public static class TitleStyle {
    public String textColor = "#7e22ce";
    public String backgroundColor = "#f3e8ff";
    public String borderRadius = "4px";
    public String borderStyle = "none";
    public String borderColor = "transparent";
    public String textEffect = "none";
    public String animationVibe = "none";
  }

  public String avatarUrl;

  public User() {
  }

  public User(String username, String password, String displayName, String avatarUrl, String title,
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

  public void setAndHashPassword(String plainPassword) {
    this.password = BcryptUtil.bcryptHash(plainPassword);
  }

  public boolean checkPassword(String plainPassword) {
    return BcryptUtil.matches(plainPassword, this.password);
  }
}
