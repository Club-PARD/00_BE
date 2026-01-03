package com.youngyoung.server.mora.repo;

import com.youngyoung.server.mora.entity.Petition;
import com.youngyoung.server.mora.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PetitionRepo extends JpaRepository<Petition, Integer> {

}
