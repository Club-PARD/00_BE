package com.youngyoung.server.mora.service;

import com.youngyoung.server.mora.dto.PetitionRes;
import com.youngyoung.server.mora.entity.Petition;
import com.youngyoung.server.mora.repo.PetitionRepo;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Service;

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
//    public PetitionRes.CardNewsInfo showCardNews(Integer type, Integer status, Integer limit,
//                                                 Integer page, Integer how, String keyWord, String category){
//        PetitionRes.CardNewsInfo card = petitionRepo.findBy()
//    }
}
