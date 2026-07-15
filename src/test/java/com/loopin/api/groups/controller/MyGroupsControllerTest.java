package com.loopin.api.groups.controller;

import com.loopin.api.common.security.SecurityUtils;
import com.loopin.api.groups.dto.response.GroupResponse;
import com.loopin.api.groups.enums.GroupSizeType;
import com.loopin.api.groups.enums.GroupStatus;
import com.loopin.api.groups.listmygroups.ListMyGroupsHandler;
import com.loopin.api.groups.listmygroups.ListMyGroupsQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

@ExtendWith(MockitoExtension.class)
class MyGroupsControllerTest {

    @Mock
    private ListMyGroupsHandler handler;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        MyGroupsController controller =
            new MyGroupsController(handler);

        mockMvc =
            standaloneSetup(controller)
                .build();
    }

    @Test
    void getMyGroups_authenticatedUser_returnsGroups()
        throws Exception {

        String currentUsername =
            "member@loopin.test";

        UUID groupId =
            UUID.randomUUID();

        UUID eventId =
            UUID.randomUUID();

        UUID adminId =
            UUID.randomUUID();

        GroupResponse response =
            new GroupResponse(
                groupId,
                eventId,
                adminId,
                "admin@loopin.test",
                "Baku Java Group",
                GroupSizeType.FOUR_PLUS,
                8,
                GroupStatus.OPEN,
                "Meet near the entrance.",
                3,
                LocalDateTime.of(
                    2026,
                    7,
                    15,
                    12,
                    0
                )
            );

        when(
            handler.handle(
                new ListMyGroupsQuery(
                    currentUsername
                )
            )
        ).thenReturn(
            List.of(response)
        );

        try (
            MockedStatic<SecurityUtils> securityUtils =
                mockStatic(SecurityUtils.class)
        ) {
            securityUtils
                .when(
                    SecurityUtils::
                        getRequiredCurrentUserEmail
                )
                .thenReturn(currentUsername);

            mockMvc.perform(
                    get("/v1/me/groups")
                )
                .andExpect(
                    status().isOk()
                )
                .andExpect(
                    jsonPath("$[0].id")
                        .value(
                            groupId.toString()
                        )
                )
                .andExpect(
                    jsonPath("$[0].eventId")
                        .value(
                            eventId.toString()
                        )
                )
                .andExpect(
                    jsonPath("$[0].adminId")
                        .value(
                            adminId.toString()
                        )
                )
                .andExpect(
                    jsonPath("$[0].title")
                        .value(
                            "Baku Java Group"
                        )
                )
                .andExpect(
                    jsonPath("$[0].status")
                        .value("OPEN")
                )
                .andExpect(
                    jsonPath("$[0].memberCount")
                        .value(3)
                );
        }

        verify(handler).handle(
            new ListMyGroupsQuery(
                currentUsername
            )
        );
    }

    @Test
    void getMyGroups_userWithoutGroups_returnsEmptyArray()
        throws Exception {

        String currentUsername =
            "member@loopin.test";

        when(
            handler.handle(
                new ListMyGroupsQuery(
                    currentUsername
                )
            )
        ).thenReturn(List.of());

        try (
            MockedStatic<SecurityUtils> securityUtils =
                mockStatic(SecurityUtils.class)
        ) {
            securityUtils
                .when(
                    SecurityUtils::
                        getRequiredCurrentUserEmail
                )
                .thenReturn(currentUsername);

            mockMvc.perform(
                    get("/v1/me/groups")
                )
                .andExpect(
                    status().isOk()
                )
                .andExpect(
                    content().json("[]")
                );
        }

        verify(handler).handle(
            new ListMyGroupsQuery(
                currentUsername
            )
        );
    }
}
