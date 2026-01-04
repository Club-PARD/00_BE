package com.youngyoung.server.mora.service;

import com.youngyoung.server.mora.dto.PetitionRes;
import com.youngyoung.server.mora.entity.Petition;
import com.youngyoung.server.mora.repo.PetitionRepo;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Getter
@Setter
public class PetitionService {
    private final PetitionRepo petitionRepo;

    public PetitionService(PetitionRepo petitionRepo) {
        this.petitionRepo = petitionRepo;
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
    //cardNews 보여주기
    public List<PetitionRes.CardNewsInfo> getCardNews(
            Integer type, Integer status, Integer limit, Integer page,
            Integer how, String keyWord, String category) {

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
            entityList = petitionRepo.findCardNewsPopular(type, status, category, keyWord, pageSize, offset);
        } else {
            entityList = petitionRepo.findCardNewsRecent(type, status, category, keyWord, pageSize, offset);
        }

        // Entity -> DTO 변환
        List<PetitionRes.CardNewsInfo> dtoList = entityList.stream()
                .map(p -> new PetitionRes.CardNewsInfo(
                        p.getId(), p.getTitle(), p.getType(), p.getStatus(),
                        p.getCategory(), p.getVoteStartDate(), p.getVoteEndDate(), p.getAllows()
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

//    public PetitionRes.PetitionInfo getPetition(Long id) {
//
//    }
}
