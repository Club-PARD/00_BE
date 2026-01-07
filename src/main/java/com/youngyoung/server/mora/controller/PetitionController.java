package com.youngyoung.server.mora.controller;

import com.youngyoung.server.mora.dto.PetitionReq;
import com.youngyoung.server.mora.dto.PetitionRes;
import com.youngyoung.server.mora.dto.SessionUser;
import com.youngyoung.server.mora.entity.Likes;
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
    @Transactional
    @PostMapping("/likes")
    public ResponseEntity<?> postLike(@AuthenticationPrincipal OAuth2User oAuth2User,
                                      @RequestBody PetitionReq.LikeInfo ans) {
        try {
            //id 확인
            if (oAuth2User instanceof SessionUser) {
                SessionUser sessionUser = (SessionUser) oAuth2User;
                UUID myId = sessionUser.getId();
                Likes result = petitionService.findLike(ans, myId);
                // result가 null이 아니면 전적 있음
                if(result != null){
                    if(result.getLikes() == ans.getLikes()){
                        petitionService.deleteLike(result);
                    }
                    else petitionService.updateLike(result);
                }else petitionService.postLike(ans, myId);
                return ResponseEntity.ok(200);
            }
            return ResponseEntity.status(401).body("로그인 해주세요.");

        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(e.getMessage());
        }
    }

    //내가 게시물에 반응 누른 적 있는지 확인
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

    //댓글 달기
    @PostMapping("/comment")
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

    //댓글 불러오기 (id = petition_id)
    @GetMapping("/comment/{id}")
    public ResponseEntity<?> getComment(@AuthenticationPrincipal OAuth2User oAuth2User,
                                        @PathVariable Long id) {
        try {
            UUID myId = null;
            if (oAuth2User instanceof SessionUser) {
                SessionUser sessionUser = (SessionUser) oAuth2User;
                myId = sessionUser.getId();}
                List<PetitionRes.CommentInfo> result = petitionService.getComment(myId, id);
                return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(e.getMessage());
        }
    }

    //댓글 삭제 (id = comment_id)
    @DeleteMapping("/comment/{id}")
    public ResponseEntity<?> delelteComment(@AuthenticationPrincipal OAuth2User oAuth2User,
                                        @PathVariable Long id) {
        try {
            if (oAuth2User instanceof SessionUser) {
                SessionUser sessionUser = (SessionUser) oAuth2User;
                UUID myId = sessionUser.getId();
            Integer result = petitionService.deleteComment(myId, id);
            if(result == 0){return ResponseEntity.ok("완료");}else return ResponseEntity.status(402).body("해당 권한이 없습니다.");}
            else{
                return ResponseEntity.status(401).body("로그인 해주세요.");
            }
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(e.getMessage());
        }
    }
    //스크랩 등록
    @PostMapping("/scrap/{id}")
    public ResponseEntity<?> postComment(@AuthenticationPrincipal OAuth2User oAuth2User,
                                         @PathVariable Long id) {
        try {
            //id 확인
            if (oAuth2User instanceof SessionUser) {
                SessionUser sessionUser = (SessionUser) oAuth2User;
                UUID myId = sessionUser.getId();

                petitionService.postScrap(myId, id);
                return ResponseEntity.ok(200);
            }
            return ResponseEntity.status(401).body("로그인 해주세요.");

        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(e.getMessage());
        }
    }

}
