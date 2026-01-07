package com.youngyoung.server.mora.repo;

import com.youngyoung.server.mora.entity.LawsLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LawsLinkRepo extends JpaRepository<LawsLink, Integer> {
}
