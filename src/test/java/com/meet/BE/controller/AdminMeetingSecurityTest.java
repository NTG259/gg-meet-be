package com.meet.BE.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AdminMeetingSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldReturnUnauthorizedWhenUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/admin/meetings"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USER")
    void shouldReturnForbiddenForNonAdmin() throws Exception {
        mockMvc.perform(get("/api/admin/meetings"))
                .andExpect(status().isForbidden());
    }
}
