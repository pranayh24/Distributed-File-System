package org.pr.dfs.model;

import jakarta.persistence.*;
import lombok.*;

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

    @Column(nullable = false)
    private String shareKey;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false)
    private String fileId;

    private String password;

    private int shareLimit = 5;

    @Column(nullable = false)
    private String filePath; // will be - users/username/filPath

    private List<String> sharedUsersIP;
}
