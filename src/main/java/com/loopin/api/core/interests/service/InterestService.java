package com.loopin.api.core.interests.service;

import com.loopin.api.core.interests.dto.InterestResponse;

import java.util.List;

public interface InterestService {

    List<InterestResponse> getInterests();
}
