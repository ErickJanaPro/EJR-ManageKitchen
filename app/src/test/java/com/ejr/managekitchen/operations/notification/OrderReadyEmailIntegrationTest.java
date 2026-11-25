package com.ejr.managekitchen.operations.notification;

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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** F7 : l'e-mail « commande prête » part vers le bon client, avec le texte adapté au mode de remise. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrderReadyEmailIntegrationTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JwtService jwtService;

    /** Remplace l'envoi d'e-mails par un faux, pour vérifier les appels. */
    @MockitoBean
    private EmailSender emailSender;

    @Test
    void retraitUnEmailEstEnvoyeQuandLaCommandeEstPrete() throws Exception {
        String email = "client-" + UUID.randomUUID() + "@test.local";
        String client = registerClient(email);
        String cook = createToken(Role.CUISINIER);
        Integer orderId = createOrder(client, "RETRAIT");

        changeStatus(orderId, "EN_PREPARATION", cook);
        verify(emailSender, never()).send(anyString(), anyString(), anyString());

        changeStatus(orderId, "PRETE", cook);
        verify(emailSender).send(eq(email), eq("Votre commande est prête"),
                contains("vous pouvez venir la récupérer"));
    }

    @Test
    void livraisonLeTexteDeLEmailEstAdapte() throws Exception {
        String email = "client-" + UUID.randomUUID() + "@test.local";
        String client = registerClient(email);
        String cook = createToken(Role.CUISINIER);
        Integer orderId = createOrder(client, "LIVRAISON");

        changeStatus(orderId, "EN_PREPARATION", cook);
        changeStatus(orderId, "PRETE", cook);

        verify(emailSender).send(eq(email), eq("Votre commande est prête"), contains("part en livraison"));
    }

    // ---------- utilitaires ----------

    private String registerClient(String email) throws Exception {
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

    private Integer createOrder(String clientToken, String fulfillmentType) throws Exception {
        MvcResult result = mvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer " + clientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fulfillmentType":"%s","items":[{"dishId":1,"quantity":1}]}
                                """.formatted(fulfillmentType)))
                .andExpect(status().isCreated())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
    }

    private void changeStatus(Integer orderId, String target, String token) throws Exception {
        mvc.perform(patch("/api/orders/" + orderId + "/status")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"" + target + "\"}"))
                .andExpect(status().isOk());
    }
}