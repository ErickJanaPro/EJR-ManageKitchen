package com.ejr.managekitchen.supplychain.supplier;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser(roles = "ADMIN")
class SupplierControllerTest {

    @Autowired
    MockMvc mvc;

    @Test
    void createThenGetSupplier() throws Exception {
        mvc.perform(post("/api/suppliers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Metro Test\",\"email\":\"contact@metro.test\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("Metro Test"));

        mvc.perform(get("/api/suppliers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.name=='Metro Test')]").exists());
    }

    @Test
    void blankNameIsRejected() throws Exception {
        mvc.perform(post("/api/suppliers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unknownSupplierReturns404() throws Exception {
        mvc.perform(get("/api/suppliers/999999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void duplicateNameReturns409() throws Exception {
        String body = "{\"name\":\"Doublon SA\"}";
        mvc.perform(post("/api/suppliers").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/suppliers").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
    }

    @Test
    void addIngredientToSupplier() throws Exception {
        String created = mvc.perform(post("/api/suppliers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Primeur Bio\"}"))
                .andReturn().getResponse().getContentAsString();
        String id = created.replaceAll(".*\"id\":(\\d+).*", "$1");

        mvc.perform(post("/api/suppliers/" + id + "/ingredients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Tomate\",\"unit\":\"kg\",\"unitPrice\":2.50}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Tomate"));

        mvc.perform(get("/api/suppliers/" + id + "/ingredients"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }
}
