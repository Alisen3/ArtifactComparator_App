package com.halenteck.demo.security;

import com.halenteck.demo.service.CustomUserDetailsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

// Bu sınıf, her istekte YALNIZCA BİR KEZ çalışacak bir filtre görevi görür.
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private CustomUserDetailsService customUserDetailsService;

    // Gelen isteği filtreleyen ana metod
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String requestURI = request.getRequestURI();
            String method = request.getMethod();

            // 1. İstekten JWT'yi (Token'ı) al
            String jwt = getJwtFromRequest(request);

            System.out.println("=== JWT AUTHENTICATION FILTER ===");
            System.out.println("Request: " + method + " " + requestURI);
            System.out.println("Has JWT: " + (jwt != null));

            // 2. Token'ı doğrula
            if (StringUtils.hasText(jwt) && tokenProvider.validateToken(jwt)) {

                // 3. Token'dan kullanıcı adını (username) al
                String username = tokenProvider.getUsernameFromJWT(jwt);
                System.out.println("Username from JWT: " + username);

                // 4. Veritabanından kullanıcıyı (UserDetails) yükle
                UserDetails userDetails = customUserDetailsService.loadUserByUsername(username);
                System.out.println("User loaded, authorities: " + userDetails.getAuthorities());

                // 5. Spring Security için bir "Kimlik Doğrulama" (Authentication) nesnesi oluştur
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities()
                );

                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // 6. Bu kimlik doğrulamasını Spring Security'nin "Context"ine (bağlamına) yerleştir.
                // Artık Spring Security bu kullanıcının giriş yapmış olduğunu bilir.
                SecurityContextHolder.getContext().setAuthentication(authentication);
                System.out.println("Authentication set successfully");
            } else {
                System.out.println("JWT validation failed or no JWT present");
            }
        } catch (Exception ex) {
            System.err.println("JWT Authentication error: " + ex.getMessage());
            logger.error("Could not set user authentication in security context", ex);
        }

        // 7. İsteğin, filtreden geçip Controller'a doğru devam etmesini sağla
        filterChain.doFilter(request, response);
    }

    // "Authorization: Bearer [TOKEN]" başlığından token'ı çıkaran yardımcı metod
    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7); // "Bearer " kısmını (7 karakter) atla
        }
        return null;
    }
}