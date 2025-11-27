package com.halenteck.demo.security;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtTokenProvider {

    private static final Logger logger = LoggerFactory.getLogger(JwtTokenProvider.class);

    // application.properties'den gizli anahtarı ve geçerlilik süresini oku
    @Value("${app.jwt.secret}")
    private String jwtSecretString;

    @Value("${app.jwt.expiration-ms}")
    private long jwtExpirationInMs;

    private SecretKey jwtSecretKey; // İmzalama için kullanılacak 'SecretKey' nesnesi

    // Bu metod, @Value'lar enjekte edildikten sonra SADECE BİR KEZ çalışır.
    // String (Base64) olan gizli anahtarı, güvenli bir 'SecretKey' nesnesine dönüştürür.
    @PostConstruct
    public void init() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecretString);
        this.jwtSecretKey = Keys.hmacShaKeyFor(keyBytes);
    }

    // Bir kullanıcı için JWT oluşturan metod
    public String generateToken(Authentication authentication) {
        // Giriş yapmış kullanıcının detaylarını al
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        String username = userDetails.getUsername();
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationInMs);

        // JWT'yi oluştur
        return Jwts.builder()
                .subject(username) // Token'in "konusu" (sahibi)
                .issuedAt(now) // Ne zaman oluşturulduğu
                .expiration(expiryDate) // Ne zaman geçersiz olacağı
                // Rol bilgisini de token'a ekleyelim
                .claim("role", userDetails.getAuthorities().stream().findFirst().get().getAuthority())
                .signWith(jwtSecretKey, Jwts.SIG.HS512) // Anahtar ile imzala
                .compact();
    }

    // Gelen bir JWT'den kullanıcı adını (subject) çıkaran metod
    public String getUsernameFromJWT(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(jwtSecretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return claims.getSubject();
    }

    // Gelen bir JWT'nin geçerli olup olmadığını doğrulayan metod
    public boolean validateToken(String authToken) {
        try {
            Jwts.parser().verifyWith(jwtSecretKey).build().parseSignedClaims(authToken);
            return true;
        } catch (MalformedJwtException ex) {
            logger.error("Invalid JWT token");
        } catch (ExpiredJwtException ex) {
            logger.error("Expired JWT token");
        } catch (UnsupportedJwtException ex) {
            logger.error("Unsupported JWT token");
        } catch (IllegalArgumentException ex) {
            logger.error("JWT claims string is empty.");
        }
        return false;
    }
}