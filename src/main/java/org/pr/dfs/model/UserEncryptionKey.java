package org.pr.dfs.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "user_encryption_keys")
public class UserEncryptionKey {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String keyId;

    @Column(nullable = false, unique = true)
    private String userId;

    @Column(nullable = false, length = 1000)
    private String encryptedKey;

    @Column(nullable = false)
    private String salt;

    @Column(nullable = false)
    private String algorithm;

    @Column(nullable = false)
    private int keyLength;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime lastUsed;

    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private Boolean active = true;
}
