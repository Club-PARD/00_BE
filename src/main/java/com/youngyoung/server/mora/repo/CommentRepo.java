package com.youngyoung.server.mora.repo;

import com.youngyoung.server.mora.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepo extends JpaRepository<Comment,Long> {
}