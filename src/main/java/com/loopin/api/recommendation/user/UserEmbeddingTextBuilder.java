package com.loopin.api.recommendation.user;

import com.loopin.api.interests.entity.Interest;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class UserEmbeddingTextBuilder {

    public String build(List<Interest> interests) {
        if (interests == null || interests.isEmpty()) {
            return "";
        }

        return interests.stream()
                .filter(interest -> interest != null && interest.getName() != null)
                .sorted(Comparator.comparing(interest -> interest.getName().toLowerCase()))
                .map(interest -> "interest: " + interest.getName())
                .collect(Collectors.joining("\n"));
    }
}
