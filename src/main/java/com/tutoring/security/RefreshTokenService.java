package com.tutoring.security;


import com.tutoring.entity.RefreshToken;
import com.tutoring.exception.InvalidRefreshTokenException;
import com.tutoring.repository.RefreshTokenRepository;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;



@Service
public class RefreshTokenService {

    private static final int TOKEN_BYTES = 64;
    private static final long REFRESH_TOKEN_TTL_DAYS = 30;
    private final RefreshTokenRepository refreshTokenRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }


    public String issue(Long userId){
        String rawToken = generateRawToken();
        RefreshToken entity = new RefreshToken();
        entity.setUserId(userId);
        entity.setTokenHash(hash(rawToken));
        entity.setExpiresAt(LocalDateTime.now().plusDays(REFRESH_TOKEN_TTL_DAYS));
        entity.setCreatedAt(LocalDateTime.now());
        refreshTokenRepository.save(entity);
        return rawToken;
    }



    public RotationResult rotate(String rawToken){
        String presentedHash = hash(rawToken);
        RefreshToken existing = refreshTokenRepository.findByTokenHash(presentedHash)
                .orElseThrow(() -> new InvalidRefreshTokenException("Unknown refresh token"));

        if (existing.getRevokedAt() != null){
            revokeChainFrom(existing);
            throw new InvalidRefreshTokenException("Refresh token expired");
        }

        String newRawToken = generateRawToken();
        RefreshToken next = new RefreshToken();
        next.setUserId(existing.getUserId());
        next.setTokenHash(hash(newRawToken));
        next.setExpiresAt(LocalDateTime.now().plusDays(REFRESH_TOKEN_TTL_DAYS));
        next.setCreatedAt(LocalDateTime.now());
        refreshTokenRepository.save(next);

        existing.setRevokedAt(LocalDateTime.now());
        existing.setReplacedById(next.getId());
        refreshTokenRepository.save(existing);

        return new RotationResult(existing.getUserId(), newRawToken);
    }

    public void revoke(String rawToken){
        refreshTokenRepository.findByTokenHash(hash(rawToken))
                .ifPresent(t-> {
                    t.setRevokedAt(LocalDateTime.now());
                    refreshTokenRepository.save(t);
                });
    }

    private void revokeChainFrom(RefreshToken start){
        RefreshToken current = start;
        while (current.getReplacedById() != null){
            RefreshToken next = refreshTokenRepository.findById(current.getReplacedById()).orElse(null);
            if (next == null) break;
            if (next.getRevokedAt() == null){
                next.setRevokedAt(LocalDateTime.now());
                refreshTokenRepository.save(next);
            }
            current = next;
        }
    }


    private String generateRawToken(){
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String rawToken){
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(rawToken.getBytes());
            return HexFormat.of().formatHex(hashBytes);
        }catch (NoSuchAlgorithmException e){
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    public record RotationResult(Long userId, String newRawToken) {}

}
