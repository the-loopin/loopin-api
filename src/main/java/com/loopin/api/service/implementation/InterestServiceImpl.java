package com.loopin.api.service.implementation;

import com.loopin.api.dto.interest.InterestResponse;
import com.loopin.api.mapper.InterestMapper;
import com.loopin.api.repository.InterestRepository;
import com.loopin.api.service.abstraction.InterestService;
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
