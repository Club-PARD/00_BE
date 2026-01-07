package com.youngyoung.server.mora.repo;

import com.youngyoung.server.mora.dto.UserRes;
import com.youngyoung.server.mora.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepo extends JpaRepository<User, Integer> {
    User findById(UUID id);

    Optional<User> findByEmail(java.lang.String email);

    @Query("SELECT new com.youngyoung.server.mora.dto.UserRes$UserInfo(" +
            "u.name, u.email, u.age, u.status ) "+
            "FROM User u " +
            "WHERE u.id = :myId")
    UserRes.UserInfo findByIdFromFront(UUID myId);

    void deleteById(UUID myId);

    boolean findByName(String name);
}
