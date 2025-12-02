// Dosya Yolu: src/main/java/com/halenteck/demo/security/SecurityConfig.java
package com.halenteck.demo.security;

// (Tüm import'lar aynı)
import com.halenteck.demo.UserRole;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.Arrays;


@Configuration
@EnableWebSecurity
public class SecurityConfig {

    // (Bean 1, 2, 3 değişmedi)

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:5173"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    // --- BEAN 4: Ana Güvenlik Zinciri (GÜNCELLENDİ) ---
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                .authorizeHttpRequests(auth -> auth

                        // --- PUBLIC Endpoints (Herkese Açık) ---
                        .requestMatchers("/register", "/login").permitAll()
                        // --- Static Resources (Webde Uploaded Files Görüntülemek için) ---
                        .requestMatchers("/uploads/**").permitAll()


                        // (YENİ EKLENEN 'participants' endpoint'i)
                        .requestMatchers(HttpMethod.GET, "/api/users/participants", "/api/users/researchers")
                        .hasAnyAuthority(UserRole.RESEARCHER.name(), UserRole.ADMIN.name())
                        
                        // Admin-only: Get all users and update user roles
                        .requestMatchers(HttpMethod.GET, "/api/users")
                        .hasAuthority(UserRole.ADMIN.name())
                        .requestMatchers(HttpMethod.PUT, "/api/users/*/role")
                        .hasAuthority(UserRole.ADMIN.name())

                        // --- PARTICIPANT Endpoints (Katılımcı Yetkileri) ---
                        .requestMatchers(HttpMethod.GET, "/api/studies/{studyId}/quiz")
                        .hasAuthority(UserRole.PARTICIPANT.name())
                        .requestMatchers(HttpMethod.POST, "/api/studies/{studyId}/quiz/submit")
                        .hasAuthority(UserRole.PARTICIPANT.name())

                        .requestMatchers(HttpMethod.GET, "/api/tasks/my-tasks", "/api/tasks/*")
                        .hasAuthority(UserRole.PARTICIPANT.name())
                        .requestMatchers(HttpMethod.POST, "/api/tasks/**")
                        .hasAuthority(UserRole.PARTICIPANT.name())


                        // --- RESEARCHER Endpoints (Araştırmacı Yetkileri) ---

                        // YENİ EKLENDİ (Puanları görme endpoint'i)
                        .requestMatchers(HttpMethod.GET, "/api/studies/{studyId}/quiz/submissions")
                        .hasAuthority(UserRole.RESEARCHER.name())

                        // --- YENİ EKLENDİ (Adım 1) ---
                        .requestMatchers(HttpMethod.GET, "/api/studies/{studyId}/tasks")
                        .hasAnyAuthority(UserRole.RESEARCHER.name(), UserRole.ADMIN.name())
                        // --- --- --- ---

                        .requestMatchers(HttpMethod.POST,
                                "/api/artifacts/upload",
                              "/api/store-artifacts/upload",
                                "/api/studies",
                                "/api/studies/{studyId}/tasks",
                                "/api/studies/{studyId}/assign-quiz")
                        .permitAll()
                        .requestMatchers(HttpMethod.PUT, "/api/studies/*")
                        .hasAuthority(UserRole.RESEARCHER.name())
                        .requestMatchers(HttpMethod.POST, "/api/studies/*/publish")
                        .permitAll()

                        .requestMatchers(HttpMethod.GET,
                                "/api/studies/*/permissions",
                                "/api/studies/*/collaborators")
                        .hasAnyAuthority(UserRole.RESEARCHER.name(), UserRole.ADMIN.name())
                        .requestMatchers(HttpMethod.POST, "/api/studies/*/collaborators")
                        .hasAnyAuthority(UserRole.RESEARCHER.name(), UserRole.ADMIN.name())
                        .requestMatchers(HttpMethod.PATCH, "/api/studies/*/collaborators/*")
                        .hasAnyAuthority(UserRole.RESEARCHER.name(), UserRole.ADMIN.name())
                        .requestMatchers(HttpMethod.DELETE, "/api/studies/*/collaborators/*")
                        .hasAnyAuthority(UserRole.RESEARCHER.name(), UserRole.ADMIN.name())

                        .requestMatchers(HttpMethod.GET, "/api/artifacts/my-artifacts", "/api/store-artifacts/my-artifacts", "/api/studies/my-studies")
                        .hasAnyAuthority(UserRole.RESEARCHER.name(), UserRole.ADMIN.name())

                        .requestMatchers("/api/quizzes", "/api/quizzes/**")
                        .hasAuthority(UserRole.RESEARCHER.name())


                        // --- AUTHENTICATED (Giriş yapmış herkes) ---
                        .requestMatchers(HttpMethod.GET, "/api/artifacts/*","/api/store-artifacts/*").authenticated()

                        // Geri kalan her şey kilitli
                        .anyRequest().authenticated()
                )

                .httpBasic(httpBasic -> httpBasic.disable());

        http.addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
