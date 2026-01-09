package com.youngyoung.server.mora.repo;

import com.youngyoung.server.mora.dto.UserRes;
import com.youngyoung.server.mora.entity.Scrap;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface ScrapRepo extends JpaRepository<Scrap,Long> {
    @Query("SELECT new com.youngyoung.server.mora.dto.UserRes$ScrapInfo(" +
            "s.petId, p.title, p.status, p.result, p.voteStartDate, p.voteEndDate ) "+
            "FROM Scrap s JOIN Petition p ON p.id = s.petId " +
            "WHERE s.userId = :myId")
    List<UserRes.ScrapInfo> findByUserId(UUID myId);

    void deleteByUserId(UUID myId);

    @Modifying
    @Query("DELETE FROM Scrap s " +
            "WHERE s.userId = :myId AND s.petId IN :id")
    void deleteByUserIdAndPetId(UUID myId, List<Long> id);

    // 청원 ID로 스크랩한 유저들 다 찾기
    @Query("SELECT new com.youngyoung.server.mora.dto.UserRes$EmailInfo(" +
            "u.name, u.email ) "+
            "FROM Scrap s JOIN User u ON s.userId = u.id " +
            "WHERE s.petId = :petitionId")
    List<UserRes.EmailInfo> findEmailByPetId(Long petitionId);
}
