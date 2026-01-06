package com.youngyoung.server.mora.controller;

import com.youngyoung.server.mora.dto.PetitionReq;
import com.youngyoung.server.mora.dto.PetitionRes;
import com.youngyoung.server.mora.dto.SessionUser;
import com.youngyoung.server.mora.entity.User;
import com.youngyoung.server.mora.service.PetitionService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/petition")
@RequiredArgsConstructor
public class PetitionController {
    private final PetitionService petitionService;

    @GetMapping("/{id}")
    public ResponseEntity<?> getPetition(@PathVariable Long id) {
        try {
            PetitionRes.PetitionInfo result = petitionService.getPetition(id);
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(e.getMessage());
        }
    }

    @GetMapping("/news/{id}")
    public ResponseEntity<?> getNews(@PathVariable Long id) {
        try {
            List<PetitionRes.NewsInfo> result = petitionService.getNews(id);
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(e.getMessage());
        }
    }

    @GetMapping("/laws/{id}")
    public ResponseEntity<?> getLaws(@PathVariable Long id) {
        try {
            List<PetitionRes.LawsInfo> result = petitionService.getLaws(id);
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(e.getMessage());
        }
    }

    @GetMapping("/cardNews")
    public ResponseEntity<?> cardNews(@RequestParam(required = false) Integer type,
                                    @RequestParam(required = false) Integer status,
                                    @RequestParam(required = false) Integer limit,
                                    @RequestParam(required = false) Integer page,
                                    @RequestParam(defaultValue = "0") Integer how,
                                    @RequestParam(required = false) String keyWord,
                                    @RequestParam(required = false) List<String> category) {
        try {
            List<PetitionRes.CardNewsInfo> result = petitionService.getCardNews(type, status, limit, page, how, keyWord, category);
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(e.getMessage());
        }
    }

    @GetMapping("/cardNews/total")
    public ResponseEntity<?> cardNewsTotal(@RequestParam(required = false) Integer type,
                                    @RequestParam(required = false) Integer status,
                                    @RequestParam(required = false) Integer limit,
                                    @RequestParam(required = false) Integer page,
                                    @RequestParam(defaultValue = "0") Integer how,
                                    @RequestParam(required = false) String keyWord,
                                    @RequestParam(required = false) String category) {
        try {
            PetitionRes.CardNewsTotal result = petitionService.getCardNewsTotal(type, status, limit, page, how, keyWord, category);
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(e.getMessage());
        }
    }

    //like 처리(같은 거 한 번 더 누르면 삭제) / 로그인 됐는지 확인해야함
//    @Transactional
//    @PostMapping("/likes")
//    public ResponseEntity<?> postLike(@AuthenticationPrincipal OAuth2User oAuth2User,
//                                      @RequestBody PetitionReq.LikeInfo ans) {
//        try {
//            //id 확인
//            if (oAuth2User instanceof SessionUser) {
//                SessionUser sessionUser = (SessionUser) oAuth2User;
//                UUID myId = sessionUser.getId();
//                Integer result = petitionService.postLike(ans);
//                // 1이면 취소, 0이면
//                if(result == 1){
//                    PetitionReq.LikeInfo newLike = new PetitionReq.LikeInfo(ans.getId(), -ans.getLikes());
//                    petitionService.updateLike(newLike);
//                }else petitionService.updateLike(ans);
//                return ResponseEntity.ok(200);
//            }
//            return ResponseEntity.status(401).body("로그인 해주세요.");
//
//        } catch (RuntimeException e) {
//            return ResponseEntity.status(HttpStatus.NOT_FOUND)
//                    .body(e.getMessage());
//        }
//    }

    //내가 게시물에 뭘 눌렀는지
    @GetMapping("/likes/{id}")
    public ResponseEntity<?> postLike(@AuthenticationPrincipal OAuth2User oAuth2User,
                                      @PathVariable Long id) {
        try {
            //id 확인
            if (oAuth2User instanceof SessionUser) {
                SessionUser sessionUser = (SessionUser) oAuth2User;
                UUID myId = sessionUser.getId();

                Integer result = petitionService.getLike(id);
                if(result == 0){result = null;}
                return ResponseEntity.ok(result);
            }
            return ResponseEntity.status(401).body("로그인 해주세요.");

        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(e.getMessage());
        }
    }

    //내가 게시물에 뭘 눌렀는지
    @GetMapping("/comment")
    public ResponseEntity<?> postComment(@AuthenticationPrincipal OAuth2User oAuth2User,
                                      @RequestBody PetitionReq.CommentInfo comment) {
        try {
            //id 확인
            if (oAuth2User instanceof SessionUser) {
                SessionUser sessionUser = (SessionUser) oAuth2User;
                UUID myId = sessionUser.getId();

                Integer result = petitionService.postComment(myId, comment);
                if(result == 0){result = null;}
                return ResponseEntity.ok(result);
            }
            return ResponseEntity.status(401).body("로그인 해주세요.");

        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(e.getMessage());
        }
    }
}
