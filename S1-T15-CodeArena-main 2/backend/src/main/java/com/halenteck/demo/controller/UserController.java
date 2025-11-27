// Dosya Yolu: demo/controller/UserController.java
// (EKSİK OLAN /api/users/participants ENDPOINT'İ EKLENDİ)

package com.halenteck.demo.controller;

import com.halenteck.demo.dto.AuthResponse;
import com.halenteck.demo.UserRole;
import com.halenteck.demo.entity.UserEntity;
import com.halenteck.demo.repository.UserRepository;
import com.halenteck.demo.security.CustomUserDetails;
import com.halenteck.demo.security.JwtTokenProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.security.Principal; // EKLENDİ
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors; // EKLENDİ

@RestController
public class UserController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;

    public UserController(UserRepository userRepository,
                          PasswordEncoder passwordEncoder,
                          AuthenticationManager authenticationManager,
                          JwtTokenProvider tokenProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.tokenProvider = tokenProvider;
    }

    // --- YENİ EKLENEN METOT (ManageStudyTasks sayfası için) ---

    /**
     * Katılımcı (PARTICIPANT) rolündeki tüm kullanıcıları
     * güvenli bir DTO listesi olarak döndürür.
     * (Görev atama formu dropdown'ını doldurmak için)
     */
    // (Bu iç DTO, şifre gibi verilerin sızmasını engeller)
    private record ParticipantDTO(Long id, String name) {}

    @GetMapping("/api/users/participants")
    public ResponseEntity<List<ParticipantDTO>> getParticipants() {

        List<ParticipantDTO> participants = userRepository.findAll()
                .stream()
                .filter(user -> user.getRole() == UserRole.PARTICIPANT)
                .map(user -> new ParticipantDTO(user.getId(), user.getName()))
                .collect(Collectors.toList());

        return ResponseEntity.ok(participants);
    }
    // --- --- --- --- --- --- --- --- --- ---


    @GetMapping("/users")
    public ResponseEntity<List<UserEntity>> getAllUsers() {
        return ResponseEntity.ok(userRepository.findAll());
    }

    @PostMapping("/users")
    public ResponseEntity<UserEntity> createUser(@RequestBody UserEntity user) {
        UserEntity saved = userRepository.save(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody UserEntity userRequest) {
        if (userRepository.findByName(userRequest.getName()).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "Username already exists"));
        }
        if (userRepository.findByEmail(userRequest.getEmail()).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "Email already exists"));
        }

        UserEntity newUser = new UserEntity();
        newUser.setName(userRequest.getName());
        newUser.setEmail(userRequest.getEmail());
        newUser.setPassword(passwordEncoder.encode(userRequest.getPassword()));
        newUser.setRole(UserRole.PARTICIPANT);

        userRepository.save(newUser);

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        userRequest.getName(),
                        userRequest.getPassword()
                )
        );

        String token = tokenProvider.generateToken(authentication);
        UserEntity authedUser = ((CustomUserDetails) authentication.getPrincipal()).getUserEntity();

        AuthResponse authResponse = new AuthResponse(
                token,
                authedUser.getId(),
                authedUser.getName(),
                authedUser.getEmail(),
                authedUser.getRole()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(authResponse);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody UserEntity reqUser) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        reqUser.getName(),
                        reqUser.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        String token = tokenProvider.generateToken(authentication);
        UserEntity authedUser = ((CustomUserDetails) authentication.getPrincipal()).getUserEntity();

        AuthResponse authResponse = new AuthResponse(
                token,
                authedUser.getId(),
                authedUser.getName(),
                authedUser.getEmail(),
                authedUser.getRole()
        );

        return ResponseEntity.ok(authResponse);
    }

    @DeleteMapping("/users")
    public ResponseEntity<?> deleteUser(@RequestParam String name) {
        Optional<UserEntity> user = userRepository.findByName(name);
        if (user.isPresent()) {
            userRepository.delete(user.get());
            return ResponseEntity.ok(Map.of("message", "User deleted successfully"));
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "User not found"));
    }

    @PostMapping("/change-email")
    public ResponseEntity<?> changeEmail(@RequestBody Map<String, String> request, java.security.Principal principal) {
        String name = principal.getName();
        String newEmail = request.get("newEmail");

        Optional<UserEntity> userOpt = userRepository.findByName(name);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "User not found"));
        }

        if (newEmail == null || newEmail.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "New email cannot be empty"));
        }

        if (userRepository.findByEmail(newEmail).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "Email already exists"));
        }

        UserEntity user = userOpt.get();
        user.setEmail(newEmail);
        userRepository.save(user);

        return ResponseEntity.ok(Map.of("message", "Email updated successfully", "newEmail", newEmail));
    }
}