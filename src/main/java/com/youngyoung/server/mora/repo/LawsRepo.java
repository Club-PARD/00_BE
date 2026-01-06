package com.youngyoung.server.mora.repo;

import com.youngyoung.server.mora.dto.PetitionRes;
import com.youngyoung.server.mora.entity.Laws;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LawsRepo  extends JpaRepository<Laws, Integer> {
    @Query("SELECT new com.youngyoung.server.mora.dto.PetitionRes$LawsInfo(" +
            "l.title, l.summary) "+
            "FROM Laws l JOIN LawsLink ll ON ll.lawId = l.id " +
            "WHERE ll.petId = :id")
    List<PetitionRes.LawsInfo> findByPetId(Long id);
}
