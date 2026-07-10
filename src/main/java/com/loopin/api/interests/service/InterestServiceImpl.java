package com.loopin.api.core.interests.service;

import com.loopin.api.core.interests.dto.InterestResponse;
import com.loopin.api.core.interests.mapper.InterestMapper;
import com.loopin.api.core.interests.repository.InterestRepository;
import com.loopin.api.core.interests.service.InterestService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class InterestServiceImpl implements InterestService {

    private final InterestRepository interestRepository;
    private final InterestMapper interestMapper;

    @Override
    @Transactional(readOnly = true)
    public List<InterestResponse> getInterests() {
        return interestRepository.findAllByDeletedAtIsNullOrderByCategoryAscNameAsc()
                .stream()
                .map(interestMapper::toResponse)
                .toList();
    }
}
