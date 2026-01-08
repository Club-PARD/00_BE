package com.youngyoung.server.mora.service;

import com.youngyoung.server.mora.dto.PetitionReq;
import com.youngyoung.server.mora.dto.PetitionRes;
import com.youngyoung.server.mora.entity.Comment;
import com.youngyoung.server.mora.entity.Likes;
import com.youngyoung.server.mora.entity.Petition;
import com.youngyoung.server.mora.entity.Scrap;
import com.youngyoung.server.mora.repo.*;
import jakarta.transaction.Transactional;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Getter
@Setter
public class PetitionService {
    private final PetitionRepo petitionRepo;
    private final NewsRepo newsRepo;
    private final LawsRepo lawsRepo;
    private final LikesRepo likesRepo;
    private final CommentRepo commentRepo;
    private final ScrapRepo scrapRepo;

    public PetitionService(PetitionRepo petitionRepo, NewsRepo newsRepo, LawsRepo lawsRepo, LikesRepo likesRepo, CommentRepo commentRepo, ScrapRepo scrapRepo) {
        this.petitionRepo = petitionRepo;
        this.newsRepo = newsRepo;
        this.lawsRepo = lawsRepo;
        this.likesRepo = likesRepo;
        this.commentRepo = commentRepo;
        this.scrapRepo = scrapRepo;
    }

    //petition post
//    public Integer save(PetitionRes.CardNewsInfo cardNewsInfoInfo){
//        Petition user = Petition.builder()
//                .name(userInfo.getName())
//
//                .build();
//        petitionRepo.save(user);
//        return 0;
//    }

    //petition 상세정보
    public PetitionRes.PetitionInfo getPetition(Long id) {
        PetitionRes.PetitionInfo dto = petitionRepo.findByPetId(id);
        return dto;
    }

    //cardNews 보여주기
    public List<PetitionRes.CardNewsInfo> getCardNews(
            Integer type, Integer status, Integer limit, Integer page,
            Integer how, String keyWord, List<String> categories) {

        // 키워드 % 추가
        String processedKeyWord = null;
        if (keyWord != null && !keyWord.isEmpty()) {
            processedKeyWord = "%" + keyWord + "%";
        }

        // 기본값 설정
        int pageSize = (limit != null) ? limit : 4;
        int pageNum = (page != null && page > 0) ? page : 1;
        int offset = (pageNum - 1) * pageSize; // 건너뛸 개수 계산

        List<PetitionRes.CardNewsInfo> entityList;

        // 경우의 수 나누기 (how == 0: 인기순, how == 1: 최신순)
        if (how == 0) {
            entityList = petitionRepo.findCardNewsPopular(type, status, categories, processedKeyWord, pageSize, offset);
        } else {
            entityList = petitionRepo.findCardNewsRecent(type, status, categories, processedKeyWord, pageSize, offset);
        }

        // Entity -> DTO 변환
        List<PetitionRes.CardNewsInfo> dtoList = entityList.stream()
                .map(p -> new PetitionRes.CardNewsInfo(
                        p.getId(), p.getTitle(), p.getType(), p.getStatus(),
                        p.getCategory(), p.getSubTitle(), p.getVoteStartDate(), p.getVoteEndDate(), p.getAllows()
                )).collect(Collectors.toList());

        return dtoList;
    }
    //cardNews 보여주기
    public PetitionRes.CardNewsTotal getCardNewsTotal(
            Integer type, Integer status, Integer limit, Integer page,
            Integer how, String keyWord, String category) {

        int pageSize = (limit != null) ? limit : 4;
        // 전체 개수 및 총 페이지 수 계산
        int totalElements = petitionRepo.countFilteredPetitions(type, status, category, keyWord);
        int totalPages = (int) Math.ceil((double) totalElements / pageSize);
        PetitionRes.CardNewsTotal dto = new PetitionRes.CardNewsTotal(totalElements,totalPages);
        return dto;
    }

    public List<PetitionRes.NewsInfo> getNews(Long id) {
        List<PetitionRes.NewsInfo> result = newsRepo.findById(id);
        return result;
    }

    public List<PetitionRes.LawsInfo> getLaws(Long id) {
        List<PetitionRes.LawsInfo> result = lawsRepo.findByPetId(id);
        return result;
    }

    //반응 처음 누름
    public Integer postLike(PetitionReq.LikeInfo ans, UUID myId) {
        Petition petition = petitionRepo.findById(ans.getId());
        petition.plusLikes(ans.getLikes());
        Likes likes = Likes.builder()
                .petId(ans.getId())
                .userId(myId)
                .likes(ans.getLikes())
                .build();
        likesRepo.save(likes);
        return 0;
    }

    //좋아요 <-> 싫어요 교차 누름 (기존 like를 보냄)
    @Transactional
    public Integer updateLike(Likes likes) {
        Petition petition = petitionRepo.findById(likes.getPetId());
        petition.updateLikes(likes.getLikes());
        likes.updateLikes(likes.getLikes());
        return 0;
    }

    public Integer getLike(Long id) {
        Optional<Likes> likes = likesRepo.findById(id);
        if(likes==null){return 0;}
        else{return likes.get().getLikes();}
    }

    public Integer postComment(UUID id, PetitionReq.CommentInfo comment) {
        Comment newComment = Comment.builder()
                .petId(comment.getId())
                .userId(id)
                .body(comment.getBody())
                .build();
        commentRepo.save(newComment);
        return 0;
    }

    public List<PetitionRes.CommentInfo> getComment(UUID myId, Long id) {
        List<PetitionRes.CommentInfo> comments = commentRepo.findByPetId(myId, id);
        return comments;
    }

    public Integer deleteComment(UUID myId, Long id) {
        Optional<Comment> comment  =  commentRepo.findById(id);
        if(comment.get().getUserId().equals(myId)){
            commentRepo.delete(comment.get());
            return 0;
        }
        else{return 1;}
    }

    public Likes findLike(PetitionReq.LikeInfo ans, UUID myId) {
        return likesRepo.findByPetIdAndUserId(ans.getId(),myId);
    }

    public void deleteLike(Likes result) {
        Petition petition = petitionRepo.findById(result.getPetId());
        petition.deleteLikes(result.getLikes());
        likesRepo.delete(result);
    }

    public void postScrap(UUID myId, Long id) {
        Scrap scrap = Scrap.builder()
                .petId(id)
                .userId(myId)
                .build();
        scrapRepo.save(scrap);
    }
}
