package org.pr.dfs.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Table(name = "share_files")
public class Share {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long shareId;

    @Column(nullable = false, unique = true)
    private String shareKey;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false)
    private String fileId;

    private String passwordHash;

    @Builder.Default
    private int shareLimit = 5;

    @Builder.Default
    private int accessCount = 0;

    @Column(nullable = false)
    private String filePath; // will be - users/username/filePath

    @Column(nullable = false)
    private String fileName;

    private long fileSize;

    @ElementCollection
    @CollectionTable(name = "share_access_ip", joinColumns = @JoinColumn(name = "share_id"))
    @Column(name = "ip_address")
    private List<String> accessedFromIPs;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime expiresAt;

    @Builder.Default
    private boolean active = true;

    private String description;

    @PrePersist
    protected void onCreate() {
        if(createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if(expiresAt == null) {
            expiresAt = createdAt.plusDays(7);
        }
    }
}
