package com.loopin.api.controller;

import com.loopin.api.auth.enums.Role;
import com.loopin.api.common.security.JwtUtils;
import com.loopin.api.core.interests.entity.Interest;
import com.loopin.api.core.users.entity.User;
import com.loopin.api.core.users.entity.UserProfile;
import com.loopin.api.core.interests.repository.InterestRepository;
import com.loopin.api.core.interests.repository.UserInterestRepository;
import com.loopin.api.core.users.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UserProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private InterestRepository interestRepository;

    @Autowired
    private UserInterestRepository userInterestRepository;

    @Autowired
    private JwtUtils jwtUtils;

    private User user;
    private Interest tech;
    private Interest music;
    private String token;

    @BeforeEach
    void setUp() {
        userInterestRepository.deleteAll();
        interestRepository.deleteAll();
        userRepository.deleteAll();

        user = new User("profile@email.com", "Profile User", null);
        user.setRole(Role.USER);

        UserProfile profile = new UserProfile();
        profile.setUser(user);
        profile.setName(user.getName());
        user.setProfile(profile);

        user = userRepository.save(user);
        tech = interestRepository.save(interest("Tech", "tech", "Professional"));
        music = interestRepository.save(interest("Music", "music", "Culture"));
        token = jwtUtils.generateToken(user.getEmail(), user.getRole().name());
    }

    @Test
    void updateMyInterests_ReplacesUserInterests() throws Exception {
        mockMvc.perform(put("/me/interests")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "interests": [
                                    { "interestId": "%s", "weight": 1.00, "source": "USER" },
                                    { "interestId": "%s", "weight": 0.75, "source": "USER" }
                                  ]
                                }
                                """.formatted(tech.getPublicId(), music.getPublicId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].slug", is("tech")))
                .andExpect(jsonPath("$[1].slug", is("music")));

        assertEquals(2, userInterestRepository.findByUser_Id(user.getId()).size());
    }

    private Interest interest(String name, String slug, String category) {
        Interest interest = new Interest();
        interest.setName(name);
        interest.setSlug(slug);
        interest.setCategory(category);
        return interest;
    }
}
