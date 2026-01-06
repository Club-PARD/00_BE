package com.youngyoung.server.mora.repo;

import com.youngyoung.server.mora.dto.PetitionRes;
import com.youngyoung.server.mora.entity.News;
import com.youngyoung.server.mora.entity.Petition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NewsRepo extends JpaRepository<News, Integer> {
    @Query("SELECT new com.youngyoung.server.mora.dto.PetitionRes$NewsInfo(" +
            "n.title, n.url) "+
            "FROM News n WHERE " +
            "n.petId =:id")
    List<PetitionRes.NewsInfo> findById(Long id);
}
