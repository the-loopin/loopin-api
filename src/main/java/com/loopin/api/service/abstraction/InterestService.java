package com.loopin.api.service.abstraction;

import com.loopin.api.dto.interest.InterestResponse;

import java.util.List;

public interface InterestService {

    List<InterestResponse> getInterests();
}
