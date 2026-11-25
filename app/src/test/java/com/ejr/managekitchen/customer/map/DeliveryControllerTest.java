package com.ejr.managekitchen.customer.map;

import com.ejr.managekitchen.common.security.JwtService;
import com.ejr.managekitchen.common.security.Role;
import com.ejr.managekitchen.operations.user.User;
import com.ejr.managekitchen.operations.user.UserRepository;
import com.jayway.jsonpath.JsonPath;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DeliveryControllerTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JwtService jwtService;

    @Test
    void cycleCompletDeSuiviDeLivraisonAvecEvenementCuisine() throws Exception {
        String client = registerClient();
        String cook = createToken(Role.CUISINIER);

        // 1. Le client passe une commande en LIVRAISON
        MvcResult orderRes = mvc.perform(post("/api/orders")
                        .header("Authorization", bearer(client))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fulfillmentType":"LIVRAISON","items":[{"dishId":1,"quantity":1}]}
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        Integer orderId = JsonPath.read(orderRes.getResponse().getContentAsString(), "$.id");

        // 2. Il enregistre son adresse de livraison
        mvc.perform(post("/api/customer/deliveries")
                        .header("Authorization", bearer(client))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orderId":%d,"address":"15 Rue de Rivoli, Paris","latitude":48.8556,"longitude":2.3580}
                                """.formatted(orderId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("EN_ATTENTE"))
                .andExpect(jsonPath("$.progressPercent").value(0));

        // 3. La cuisine passe la commande EN_PREPARATION puis PRETE -> déclenche le suivi EN_COURS
        changeOrderStatus(orderId, "EN_PREPARATION", cook);
        changeOrderStatus(orderId, "PRETE", cook);

        mvc.perform(get("/api/customer/deliveries/" + orderId)
                        .header("Authorization", bearer(client)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EN_COURS"))
                .andExpect(jsonPath("$.address").value("15 Rue de Rivoli, Paris"));

        // 4. La cuisine clôture en LIVREE -> suivi à 100%
        changeOrderStatus(orderId, "LIVREE", cook);

        mvc.perform(get("/api/customer/deliveries/" + orderId)
                        .header("Authorization", bearer(client)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("LIVREE"))
                .andExpect(jsonPath("$.progressPercent").value(100));
    }

    @Test
    void refuseUneAdresseHorsZoneEtProtegeAccesEntreClients() throws Exception {
        String alice = registerClient();
        String bob = registerClient();

        // Adresse à Marseille (> 15 km de Paris) -> 409 Conflict
        mvc.perform(post("/api/customer/deliveries")
                        .header("Authorization", bearer(alice))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orderId":9991,"address":"Marseille","latitude":43.2965,"longitude":5.3698}
                                """))
                .andExpect(status().isConflict());

        // Adresse valide pour Alice
        mvc.perform(post("/api/customer/deliveries")
                        .header("Authorization", bearer(alice))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orderId":9992,"address":"Gennevilliers","latitude":48.9333,"longitude":2.3000}
                                """))
                .andExpect(status().isCreated());

        // Bob ne peut pas voir la livraison d'Alice -> 404
        mvc.perform(get("/api/customer/deliveries/9992")
                        .header("Authorization", bearer(bob)))
                .andExpect(status().isNotFound());
    }

    private String registerClient() throws Exception {
        String email = "client-" + UUID.randomUUID() + "@test.local";
        MvcResult result = mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"motdepasse123","fullName":"Client Map"}
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

    private void changeOrderStatus(Integer orderId, String target, String token) throws Exception {
        mvc.perform(patch("/api/orders/" + orderId + "/status")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"" + target + "\"}"))
                .andExpect(status().isOk());
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }
}