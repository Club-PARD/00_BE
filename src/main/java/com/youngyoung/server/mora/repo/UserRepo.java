package com.youngyoung.server.mora.repo;

import com.youngyoung.server.mora.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepo extends JpaRepository<User, Integer> {
    User findById(UUID id);

    Optional<User> findByEmail(java.lang.String email);
}
