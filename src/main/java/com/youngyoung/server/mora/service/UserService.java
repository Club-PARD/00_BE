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
import java.util.Optional;
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

    // 이름은 signUp이지만 실제로는 사용자 정보 수정
    @Transactional
    public Integer save(UserReq.UserInfo userInfo){
        // 이메일로 유저 찾기
        Optional<User> opUser = userRepo.findByEmail(userInfo.getEmail());

        if (opUser.isPresent()) {
            // [기존 회원] -> 정보 수정 (Update)
            User user = opUser.get();
            user.updateUser(
                    userInfo.getName(),
                    userInfo.getAge(),
                    userInfo.getStatus()
            );
            return 0; // 수정 완료
        } else {
            // [신규 회원] -> 새로 생성 (Insert) ⭐️ 여기가 핵심
            User newUser = User.builder()
                    .email(userInfo.getEmail())
                    .name(userInfo.getName())
                    .age(userInfo.getAge())
                    .status(userInfo.getStatus())
                    .build();

            userRepo.save(newUser);
            return 0;
        }
    }

    public Integer check(String name) {
        //있으면? 0
        if(userRepo.findByName(name)) return 0;
        else return 1;
    }

    public UserRes.UserInfo getUser(UUID myId) {
        return userRepo.findByIdFromFront(myId);
    }

    public List<UserRes.ScrapInfo> getScrap(UUID myId) {
        return scrapRepo.findByUserId(myId);
    }

    @Transactional
    public void deleteUser(UUID myId) {
        likesRepo.deleteByUserId(myId);
        scrapRepo.deleteByUserId(myId);
        commentRepo.deleteByUserId(myId);
        userRepo.deleteById(myId);
    }

    @Transactional
    public void deleteScraps(UUID myId, List<Long> id) {
        scrapRepo.deleteByUserIdAndPetId(myId, id);
    }
}
