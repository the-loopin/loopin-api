package com.loopin.api.interests.service;

import com.loopin.api.interests.dto.InterestResponse;

import java.util.List;

public interface InterestService {

    List<InterestResponse> getInterests();
}
