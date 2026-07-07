package com.loopin.api.dto.interest;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InterestResponse {

    private UUID id;
    private String name;
    private String slug;
    private String category;
}
