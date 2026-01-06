package com.youngyoung.server.mora.repo;

import com.youngyoung.server.mora.dto.UserRes;
import com.youngyoung.server.mora.entity.Scrap;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface ScrapRepo extends JpaRepository<Scrap,Long> {
    @Query("SELECT new com.youngyoung.server.mora.dto.UserRes$ScrapInfo(" +
            "s.petId, p.title ) "+
            "FROM Scrap s JOIN Petition p ON p.id = s.petId " +
            "WHERE s.userId = :myId")
    List<UserRes.ScrapInfo> findByUserId(UUID myId);

    void deleteByUserId(UUID myId);
}
