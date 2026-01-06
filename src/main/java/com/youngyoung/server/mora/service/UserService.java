package com.youngyoung.server.mora.service;

import com.youngyoung.server.mora.dto.UserReq;
import com.youngyoung.server.mora.dto.UserRes;
import com.youngyoung.server.mora.entity.User;
import com.youngyoung.server.mora.repo.CommentRepo;
import com.youngyoung.server.mora.repo.LikesRepo;
import com.youngyoung.server.mora.repo.ScrapRepo;
import com.youngyoung.server.mora.repo.UserRepo;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@Getter
@Setter
public class UserService {
    private final UserRepo userRepo;
    private final ScrapRepo scrapRepo;
    private final LikesRepo likesRepo;
    private final CommentRepo commentRepo;

    public UserService(UserRepo userRepo, ScrapRepo scrapRepo, LikesRepo likesRepo, CommentRepo commentRepo) {
        this.userRepo = userRepo;
        this.scrapRepo = scrapRepo;
        this.likesRepo = likesRepo;
        this.commentRepo = commentRepo;
    }

    //signUp
    public Integer save(UserReq.UserInfo userInfo){
        User user = User.builder()
                .name(userInfo.getName())
                .email(userInfo.getEmail())
                .age(userInfo.getAge())
                .status(userInfo.getStatus())
                .build();
        userRepo.save(user);
        return 0;
    }

    public Integer check(String id) {
        if(userRepo.findById(UUID.fromString(id))==null) return 0;
        else return 1;
    }

    public UserRes.UserInfo getUser(UUID myId) {
        return userRepo.findByIdFromFront(myId);
    }

    public List<UserRes.ScrapInfo> getScrap(UUID myId) {
        return scrapRepo.findByUserId(myId);
    }

    public void deleteUser(UUID myId) {
        likesRepo.deleteByUserId(myId);
        scrapRepo.deleteByUserId(myId);
        commentRepo.deleteByUserId(myId);
        userRepo.deleteById(myId);
    }
}
