package com.ejr.managekitchen.customer.menu;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CustomerControllerTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void leMenuEstAccessibleSansAuthentificationEtAligneSurLeCatalogue() throws Exception {
        mvc.perform(get("/api/customer/menu"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].nom").value("Burger végétarien"))
                .andExpect(jsonPath("$[0].prix").value(12.5))
                .andExpect(jsonPath("$[0].disponible").value(true))
                .andExpect(jsonPath("$[3].id").value(4))
                .andExpect(jsonPath("$[3].disponible").value(false));
    }

    @Test
    void laPageAccueilClientEstAccessiblePubliquement() throws Exception {
        mvc.perform(get("/index.html"))
                .andExpect(status().isOk());
    }
}