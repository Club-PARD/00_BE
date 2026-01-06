package com.youngyoung.server.mora.entity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Likes {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private Long petId;
    @Column(nullable = false)
    private UUID userId;
    @Column(nullable = false)
    private Integer likes;

    public void updateLikes(Integer likes) {
        this.likes = -likes;
    }
}
