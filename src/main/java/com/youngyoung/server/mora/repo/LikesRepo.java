package com.youngyoung.server.mora.repo;

import com.youngyoung.server.mora.entity.Likes;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface LikesRepo extends JpaRepository<Likes, Long> {
    void deleteByUserId(UUID myId);

    Likes findByPetIdAndUserId(Long id, UUID myId);
}
