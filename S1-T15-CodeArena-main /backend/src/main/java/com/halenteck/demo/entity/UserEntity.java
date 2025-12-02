package com.halenteck.demo.entity;

import jakarta.persistence.*;
import java.util.Set;
import java.util.HashSet;
import com.halenteck.demo.UserRole;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "users")
public class UserEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String email;
    private String password;
    private String skills;
    private Integer yearsOfExperience;

    @Enumerated(EnumType.STRING)
    private UserRole role;

    @JsonIgnore
    @ManyToMany(mappedBy = "participants")
    private Set<StudyEntity> studies = new HashSet<>();

    public UserEntity() {
    }

    public UserEntity(String name, String email, String password, UserRole role) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.role = role;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }
    
    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public UserRole getRole() {
        return role;
    }

    public String getSkills() {
        return skills;
    }

    public Integer getYearsOfExperience() {
        return yearsOfExperience;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setRole(UserRole role){
        this.role = role;
    }

    public void setSkills(String skills) {
        this.skills = skills;
    }

    public void setYearsOfExperience(Integer yearsOfExperience) {
        this.yearsOfExperience = yearsOfExperience;
    }

    public Set<StudyEntity> getStudies() {
        return studies;
    }

}