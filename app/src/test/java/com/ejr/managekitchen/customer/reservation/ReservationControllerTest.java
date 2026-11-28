package com.ejr.managekitchen.customer.reservation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReservationControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String registerClientAndGetToken(String fullName) throws Exception {
        String email = "res-" + UUID.randomUUID() + "@ejr.local";
        MvcResult result = mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email,
                                "password", "password123",
                                "fullName", fullName
                        ))))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.get("accessToken").asText();
    }

    @Test
    void disponibilitesConsultablesPubliquementPourUneDateFuture() throws Exception {
        LocalDate futureDate = LocalDate.now().plusDays(10);

        mvc.perform(get("/api/customer/reservations/availability")
                        .param("date", futureDate.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(ReservationService.ALLOWED_SLOTS.size()))
                .andExpect(jsonPath("$[0].timeSlot").value("12:00"))
                .andExpect(jsonPath("$[0].capacity").value(ReservationService.MAX_CAPACITY_PER_SLOT))
                .andExpect(jsonPath("$[0].remainingSeats").value(ReservationService.MAX_CAPACITY_PER_SLOT))
                .andExpect(jsonPath("$[0].available").value(true));
    }

    @Test
    void cycleCompletCreationListingEtAnnulationLibereLesPlaces() throws Exception {
        String token = registerClientAndGetToken("Camille Laurent");
        LocalDate date = LocalDate.now().plusDays(11);

        // 1. Création de la réservation (6 couverts à 19:30)
        MvcResult createdResult = mvc.perform(post("/api/customer/reservations")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "reservationDate", date.toString(),
                                "timeSlot", "19:30",
                                "guestsCount", 6,
                                "clientName", "Camille Laurent",
                                "notes", "Table près de la fenêtre"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CONFIRMEE"))
                .andExpect(jsonPath("$.guestsCount").value(6))
                .andExpect(jsonPath("$.timeSlot").value("19:30"))
                .andReturn();

        long reservationId = objectMapper.readTree(createdResult.getResponse().getContentAsString())
                .get("id").asLong();

        // 2. Vérification de la décrémentation des places (40 - 6 = 34)
        mvc.perform(get("/api/customer/reservations/availability")
                        .param("date", date.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[3].timeSlot").value("19:30"))
                .andExpect(jsonPath("$[3].reservedSeats").value(6))
                .andExpect(jsonPath("$[3].remainingSeats").value(34));

        // 3. Listing /me
        mvc.perform(get("/api/customer/reservations/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(reservationId));

        // 4. Annulation
        mvc.perform(patch("/api/customer/reservations/" + reservationId + "/cancel")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ANNULEE"));

        // 5. Les places sont bien libérées (retour à 40)
        mvc.perform(get("/api/customer/reservations/availability")
                        .param("date", date.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[3].reservedSeats").value(0))
                .andExpect(jsonPath("$[3].remainingSeats").value(40));
    }

    @Test
    void rejetteEn409SiCapaciteDuCreneauDepassee() throws Exception {
        String token = registerClientAndGetToken("Groupe Complet");
        LocalDate date = LocalDate.now().plusDays(12);

        // Remplir 36 places (3 x 12)
        for (int i = 0; i < 3; i++) {
            mvc.perform(post("/api/customer/reservations")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "reservationDate", date.toString(),
                                    "timeSlot", "20:30",
                                    "guestsCount", 12
                            ))))
                    .andExpect(status().isCreated());
        }

        // Il reste 4 places : demander 5 couverts doit renvoyer 409 Conflict
        mvc.perform(post("/api/customer/reservations")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "reservationDate", date.toString(),
                                "timeSlot", "20:30",
                                "guestsCount", 5
                        ))))
                .andExpect(status().isConflict());
    }

    @Test
    void unAutreClientNePeutPasAnnulerLaReservationDunTiers() throws Exception {
        String ownerToken = registerClientAndGetToken("Client Propriétaire");
        String otherToken = registerClientAndGetToken("Autre Client");
        LocalDate date = LocalDate.now().plusDays(13);

        MvcResult createdResult = mvc.perform(post("/api/customer/reservations")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "reservationDate", date.toString(),
                                "timeSlot", "12:45",
                                "guestsCount", 2
                        ))))
                .andExpect(status().isCreated())
                .andReturn();

        long reservationId = objectMapper.readTree(createdResult.getResponse().getContentAsString())
                .get("id").asLong();

        mvc.perform(patch("/api/customer/reservations/" + reservationId + "/cancel")
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isNotFound());
    }
}