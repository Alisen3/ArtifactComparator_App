package com.halenteck.demo.repository;

import com.halenteck.demo.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<UserEntity, Long> {
    Optional<UserEntity> findByName(String name);
    Optional<UserEntity> findByEmail(String email);
    Optional<UserEntity> findByNameOrEmail(String name, String email);
    List<UserEntity> findByNameContaining(String name);
    Optional<UserEntity> findByNameAndPassword(String name, String password);
    List<UserEntity> findByEmailEndingWith(String domain);
}

