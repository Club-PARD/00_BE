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
import org.springframework.transaction.annotation.Transactional;

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

    // 이름은 signUp이지만 실제로는 사용자 정보 '수정'
    @Transactional
    public Integer save(UserReq.UserInfo userInfo){
        // 전달받은 이메일로 기존 사용자를 찾음
        User user = userRepo.findByEmail(userInfo.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다. email=" + userInfo.getEmail()));

        // User 엔티티에 있는 업데이트 메소드를 사용해 정보 수정
        user.updateNameAndStatusAndAge(
                userInfo.getName(),
                userInfo.getAge(),
                userInfo.getStatus()
        );

        // @Transactional 어노테이션으로 인해 메소드가 종료될 때 변경된 내용이 자동으로 DB에 반영(UPDATE)됨
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
