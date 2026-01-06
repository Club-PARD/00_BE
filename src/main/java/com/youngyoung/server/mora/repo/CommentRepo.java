package com.youngyoung.server.mora.repo;

import com.youngyoung.server.mora.dto.PetitionRes;
import com.youngyoung.server.mora.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface CommentRepo extends JpaRepository<Comment,Long> {
    @Query("""
    SELECT new com.youngyoung.server.mora.dto.PetitionRes$CommentInfo(
        c.id,
        u.name,
        c.body,
        CASE WHEN c.userId = :myId THEN true ELSE false END
    )
    FROM Comment c
    JOIN User u ON u.id = c.userId
    WHERE c.petId = :id
""")
    List<PetitionRes.CommentInfo> findByPetId(UUID myId, Long id);

    void deleteByUserId(UUID myId);
}