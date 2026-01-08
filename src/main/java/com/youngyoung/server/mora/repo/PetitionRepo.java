package com.youngyoung.server.mora.repo;

import com.youngyoung.server.mora.dto.PetitionRes;
import com.youngyoung.server.mora.entity.Petition;
import com.youngyoung.server.mora.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.awt.print.Pageable;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PetitionRepo extends JpaRepository<Petition, Integer> {
    // 1. 인기순(allows DESC) + 필터링 + 페이징
    @Query("SELECT new com.youngyoung.server.mora.dto.PetitionRes$CardNewsInfo(" +
            "p.id, p.title, p.type, p.status, p.category, p.subTitle, p.voteStartDate, p.voteEndDate, p.allows) " +
            "FROM Petition p WHERE " +
            "(:type IS NULL OR p.type = :type) AND " +
            "(:status IS NULL OR p.status = :status) AND " +
            "(:categories IS NULL OR p.category IN :categories) AND " +
            "(:keyWord IS NULL OR p.title LIKE %:keyWord%) " +
            "ORDER BY p.allows DESC LIMIT :limit OFFSET :offset")
    List<PetitionRes.CardNewsInfo> findCardNewsPopular(Integer type, Integer status, List<String> categories, String keyWord, int limit, int offset);

    // 2. 최신순(vote_start_date DESC) + 필터링 + 페이징
    @Query("SELECT new com.youngyoung.server.mora.dto.PetitionRes$CardNewsInfo(" +
            "p.id, p.title, p.type, p.status, p.category, p.subTitle, p.voteStartDate, p.voteEndDate, p.allows) " +
            "FROM Petition p WHERE " +
            "(:type IS NULL OR p.type = :type) AND " +
            "(:status IS NULL OR p.status = :status) AND " +
            "(:categories IS NULL OR p.category IN :categories) AND " +
            "(:keyWord IS NULL OR p.title LIKE :keyWord) " +
            "ORDER BY p.voteStartDate DESC LIMIT :limit OFFSET :offset")
    List<PetitionRes.CardNewsInfo> findCardNewsRecent(Integer type, Integer status, List<String> categories, String keyWord, int limit, int offset);
    // 3. 전체 개수 조회 (totalPages 계산용)
    @Query(value = "SELECT COUNT(*) FROM petition p WHERE " +
            "(:type IS NULL OR p.type = :type) AND " +
            "(:status IS NULL OR p.status = :status) AND " +
            "(:category IS NULL OR p.category = :category) AND " +
            "(:keyWord IS NULL OR p.title LIKE :keyWord)", nativeQuery = true)
    Integer countFilteredPetitions(Integer type, Integer status, String category, String keyWord);

    @Query("SELECT new com.youngyoung.server.mora.dto.PetitionRes$PetitionInfo(" +
            "p.title, p.type, p.subTitle, p.petitionNeeds, p.petitionSummary, p.status, p.category, p.finalDate, p.voteStartDate, p.voteEndDate, p.result, p.positiveEx, p.negativeEx, p.good, p.bad, p.allows, p.url, p.department) " +
            "FROM Petition p WHERE "+
            "p.id =:id")
    PetitionRes.PetitionInfo findByPetId(Long id);

    Petition findById(Long petId);

    // 종료된 청원 찾기 (status=0 이면서 종료일이 지난 것)
    List<Petition> findAllByStatusAndVoteEndDateBefore(Integer status, LocalDateTime now);

    // 계류현황 업데이트 대상 (status=1, finalDate=null)
    List<Petition> findByStatusAndFinalDateIsNull(Integer status);

    // 처리결과 업데이트 대상 (result="-", finalDate가 과거인 것)
    List<Petition> findByResultAndFinalDateBefore(String result, LocalDateTime now);
}
