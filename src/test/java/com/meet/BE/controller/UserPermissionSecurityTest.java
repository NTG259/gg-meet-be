package com.meet.BE.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

@SpringBootTest
@AutoConfigureMockMvc
class UserPermissionSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldAllowAdminToAccessUserMeetingEndpoints() throws Exception {
        mockMvc.perform(get("/api/meetings/upcoming"))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isNotEqualTo(403));

        mockMvc.perform(post("/api/meetings").param("title", "Admin meeting"))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isNotEqualTo(403));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldAllowAdminToAccessUserAccountEndpoints() throws Exception {
        mockMvc.perform(get("/api/auth/account"))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isNotEqualTo(403));

        mockMvc.perform(put("/api/auth/account")
                        .contentType("application/json")
                        .content("{\"fullName\":\"Admin User\"}"))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isNotEqualTo(403));
    }
}
