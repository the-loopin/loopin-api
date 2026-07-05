package com.loopin.api.controller;

import com.loopin.api.dto.interest.InterestResponse;
import com.loopin.api.service.abstraction.InterestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/interests")
@RequiredArgsConstructor
public class InterestController {

    private final InterestService interestService;

    @GetMapping
    public ResponseEntity<List<InterestResponse>> getInterests() {
        return ResponseEntity.ok(interestService.getInterests());
    }
}
