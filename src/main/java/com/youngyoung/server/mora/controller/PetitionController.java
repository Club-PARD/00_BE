package com.youngyoung.server.mora.controller;

import com.youngyoung.server.mora.dto.PetitionRes;
import com.youngyoung.server.mora.service.PetitionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/petition")
@RequiredArgsConstructor
public class PetitionController {
    private final PetitionService petitionService;

//    @GetMapping("/{id}")
//    public ResponseEntity<?> getPetition(@PathVariable Long id) {
//        try {
//            PetitionRes.PetitionInfo result = petitionService.getPetition(id);
//            return ResponseEntity.ok(result);
//        } catch (RuntimeException e) {
//            return ResponseEntity.status(HttpStatus.NOT_FOUND)
//                    .body(e.getMessage());
//        }
//    }

    @GetMapping("/cardNews")
    public ResponseEntity<?> cardNews(@RequestParam(required = false) Integer type,
                                    @RequestParam(required = false) Integer status,
                                    @RequestParam(required = false) Integer limit,
                                    @RequestParam(required = false) Integer page,
                                    @RequestParam(defaultValue = "0") Integer how,
                                    @RequestParam(required = false) String keyWord,
                                    @RequestParam(required = false) String category) {
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
}
