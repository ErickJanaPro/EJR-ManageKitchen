package com.ejr.managekitchen.operations.order;

import com.jayway.jsonpath.JsonPath;
import com.ejr.managekitchen.common.security.JwtService;
import com.ejr.managekitchen.common.security.Role;
import com.ejr.managekitchen.operations.user.User;
import com.ejr.managekitchen.operations.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests d'intégration du domaine operations : sécurité par rôle + scénario de commande.
 * Base H2 en mémoire, catalogue et stock simulés (StubSupplyChainAdapter).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrderFlowIntegrationTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JwtService jwtService;

    @Test
    void sansJetonLaRequeteEstRefuseeAvec401() throws Exception {
        mvc.perform(get("/api/orders/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void unClientNaPasAccesALaCuisine() throws Exception {
        String client = registerClient();
        mvc.perform(get("/api/kitchen/orders").header("Authorization", bearer(client)))
                .andExpect(status().isForbidden());
    }

    @Test
    void unClientNaPasAccesALaGestionDesUtilisateurs() throws Exception {
        String client = registerClient();
        mvc.perform(get("/api/users").header("Authorization", bearer(client)))
                .andExpect(status().isForbidden());
    }

    @Test
    void scenarioNominalDeCommande() throws Exception {
        String client = registerClient();
        String cook = createToken(Role.CUISINIER);

        // 1. Le client commande
        MvcResult created = mvc.perform(post("/api/orders")
                        .header("Authorization", bearer(client))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fulfillmentType":"RETRAIT","items":[{"dishId":1,"quantity":2},{"dishId":3,"quantity":1}]}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("NOUVELLE"))
                .andExpect(jsonPath("$.total").value(31.5))
                .andReturn();
        Integer orderId = JsonPath.read(created.getResponse().getContentAsString(), "$.id");

        // 2. Elle apparaît en cuisine
        mvc.perform(get("/api/kitchen/orders").header("Authorization", bearer(cook)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + orderId + ")]").exists());

        // 3. Le client ne peut pas changer le statut
        changeStatus(orderId, "EN_PREPARATION", client).andExpect(status().isForbidden());

        // 4. Sauter une étape est refusé
        changeStatus(orderId, "PRETE", cook).andExpect(status().isConflict());

        // 5. Parcours normal jusqu'au retrait
        changeStatus(orderId, "EN_PREPARATION", cook).andExpect(status().isOk());
        changeStatus(orderId, "PRETE", cook).andExpect(status().isOk());
        changeStatus(orderId, "LIVREE", cook).andExpect(status().isConflict()); // commande à emporter
        changeStatus(orderId, "RECUPEREE", cook)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RECUPEREE"));

        // 6. Le client a reçu la notification « commande prête »
        String notifications = mvc.perform(get("/api/notifications/me").header("Authorization", bearer(client)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(notifications).contains("ORDER_READY");
    }

    @Test
    void unPlatIndisponibleEstRefuse() throws Exception {
        String client = registerClient();
        mvc.perform(post("/api/orders")
                        .header("Authorization", bearer(client))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fulfillmentType":"LIVRAISON","items":[{"dishId":4,"quantity":1}]}
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void unClientNeVoitPasLaCommandeDunAutre() throws Exception {
        String alice = registerClient();
        String bob = registerClient();
        MvcResult created = mvc.perform(post("/api/orders")
                        .header("Authorization", bearer(alice))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fulfillmentType":"RETRAIT","items":[{"dishId":2,"quantity":1}]}
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        Integer orderId = JsonPath.read(created.getResponse().getContentAsString(), "$.id");

        mvc.perform(get("/api/orders/" + orderId).header("Authorization", bearer(bob)))
                .andExpect(status().isNotFound());
    }

    // ---------- utilitaires ----------

    private String registerClient() throws Exception {
        String email = "client-" + UUID.randomUUID() + "@test.local";
        MvcResult result = mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"motdepasse123","fullName":"Client Test"}
                                """.formatted(email)))
                .andExpect(status().isCreated())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.accessToken");
    }

    private String createToken(Role role) {
        User user = userRepository.save(new User(role.name().toLowerCase() + "-" + UUID.randomUUID() + "@test.local",
                passwordEncoder.encode("motdepasse123"), "Test " + role, role));
        return jwtService.generate(user.toAuthenticatedUser());
    }

    private org.springframework.test.web.servlet.ResultActions changeStatus(Integer orderId, String target, String token)
            throws Exception {
        return mvc.perform(patch("/api/orders/" + orderId + "/status")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"" + target + "\"}"));
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }
}
