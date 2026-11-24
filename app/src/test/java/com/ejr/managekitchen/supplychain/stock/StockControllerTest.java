package com.ejr.managekitchen.supplychain.stock;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser(roles = "ADMIN")
class StockControllerTest {

    @Autowired
    MockMvc mvc;
    @Autowired
    StockService stockService;

    /** Crée un fournisseur + un ingrédient et renvoie l'id de l'ingrédient. */
    private String newIngredient(String supplierName, String ingredientName) throws Exception {
        String supplier = mvc.perform(post("/api/suppliers").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + supplierName + "\"}"))
                .andReturn().getResponse().getContentAsString();
        String sid = supplier.replaceAll(".*\"id\":(\\d+).*", "$1");
        String ingredient = mvc.perform(post("/api/suppliers/" + sid + "/ingredients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + ingredientName + "\",\"unit\":\"kg\",\"unitPrice\":2.50}"))
                .andReturn().getResponse().getContentAsString();
        return ingredient.replaceAll(".*\"id\":(\\d+).*", "$1");
    }

    private void json(String method, String url, String body, int status) throws Exception {
        var req = method.equals("PUT") ? put(url) : post(url);
        mvc.perform(req.contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().is(status));
    }

    @Test
    void newIngredientStartsAtZero() throws Exception {
        String id = newIngredient("Four Zero", "Farine");
        mvc.perform(get("/api/stock/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ingredientName").value("Farine"))
                .andExpect(jsonPath("$.quantity").value(0));
    }

    @Test
    void receiveIncreasesStockAndLogsMovement() throws Exception {
        String id = newIngredient("Four Recep", "Tomate");
        json("POST", "/api/stock/" + id + "/receive", "{\"quantity\":12.5,\"reason\":\"Livraison\"}", 201);
        mvc.perform(get("/api/stock/" + id)).andExpect(jsonPath("$.quantity").value(12.5));
        mvc.perform(get("/api/stock/" + id + "/movements"))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].type").value("RECEPTION"))
                .andExpect(jsonPath("$[0].delta").value(12.5));
    }

    @Test
    void invalidQuantityIsRejected() throws Exception {
        String id = newIngredient("Four Invalid", "Sel");
        json("POST", "/api/stock/" + id + "/receive", "{\"quantity\":0}", 400);
        json("POST", "/api/stock/" + id + "/receive", "{\"quantity\":-3}", 400);
        json("POST", "/api/stock/" + id + "/adjust", "{\"newQuantity\":-1}", 400);
    }

    @Test
    void adjustSetsRealQuantity() throws Exception {
        String id = newIngredient("Four Adjust", "Oignon");
        json("POST", "/api/stock/" + id + "/receive", "{\"quantity\":10}", 201);
        json("POST", "/api/stock/" + id + "/adjust", "{\"newQuantity\":7,\"reason\":\"Inventaire\"}", 200);
        mvc.perform(get("/api/stock/" + id)).andExpect(jsonPath("$.quantity").value(7));
        mvc.perform(get("/api/stock/" + id + "/movements"))
                .andExpect(jsonPath("$[0].type").value("AJUSTEMENT"))
                .andExpect(jsonPath("$[0].delta").value(-3));
    }

    @Test
    void lowStockListUsesThreshold() throws Exception {
        String id = newIngredient("Four Low", "Basilic");
        json("POST", "/api/stock/" + id + "/receive", "{\"quantity\":5}", 201);
        json("PUT", "/api/stock/" + id + "/threshold", "{\"alertThreshold\":10}", 200);
        mvc.perform(get("/api/stock/low"))
                .andExpect(jsonPath("$[?(@.ingredientName=='Basilic')].low").value(true));

        json("PUT", "/api/stock/" + id + "/threshold", "{\"alertThreshold\":2}", 200);
        mvc.perform(get("/api/stock/low"))
                .andExpect(jsonPath("$[?(@.ingredientName=='Basilic')]").isEmpty());
    }

    @Test
    void unknownIngredientReturns404() throws Exception {
        mvc.perform(get("/api/stock/999999")).andExpect(status().isNotFound());
    }

    @Test
    void consumeDecrementsOrFailsWhenInsufficient() throws Exception {
        String id = newIngredient("Four Consume", "Mozzarella");
        Long ingredientId = Long.valueOf(id);
        json("POST", "/api/stock/" + id + "/receive", "{\"quantity\":3}", 201);

        stockService.consume(ingredientId, new BigDecimal("2"), "Commande #1");
        assertThat(stockService.get(ingredientId).getQuantity()).isEqualByComparingTo("1");

        assertThatThrownBy(() -> stockService.consume(ingredientId, new BigDecimal("5"), "Commande #2"))
                .hasMessageContaining("Stock insuffisant");
        assertThat(stockService.get(ingredientId).getQuantity()).isEqualByComparingTo("1");
    }

    @Test
    @WithMockUser(roles = "CLIENT")
    void clientCannotAccessStock() throws Exception {
        mvc.perform(get("/api/stock")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "CUISINIER")
    void cookCanReadButNotWrite() throws Exception {
        mvc.perform(get("/api/stock")).andExpect(status().isOk());
        json("POST", "/api/stock/1/receive", "{\"quantity\":1}", 403);
    }
}
