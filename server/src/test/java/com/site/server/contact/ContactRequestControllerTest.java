package com.site.server.contact;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ContactRequestControllerTest {
    private static final AtomicInteger NEXT_IP = new AtomicInteger();
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ContactRequestRepository repository;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void validRequestIsSavedAsNewAndReturnsOnlyReceiptFields() throws Exception {
        String response = mockMvc.perform(post("/api/contact")
                        .with(request -> { request.setRemoteAddr("192.0.2." + NEXT_IP.incrementAndGet()); return request; })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.fullName").doesNotExist())
                .andExpect(jsonPath("$.phone").doesNotExist())
                .andReturn().getResponse().getContentAsString();

        var responseJson = objectMapper.readTree(response);
        Set<String> responseFields = new HashSet<>();
        responseJson.fieldNames().forEachRemaining(responseFields::add);
        assertThat(responseFields).containsExactlyInAnyOrder("id", "createdAt");
        long id = responseJson.get("id").asLong();
        ContactRequest saved = repository.findById(id).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(RequestStatus.NEW);
        assertThat(saved.getPhone()).isEqualTo("0501234567");
        assertThat(saved.getFullName()).isEqualTo("Noa Cohen");
    }

    @Test
    void clientCannotChooseIdOrStatus() throws Exception {
        String body = """
                {
                  "id": 99999,
                  "status": "HANDLED",
                  "fullName": "Noa Cohen",
                  "phone": "+972 50-123-4567",
                  "workType": "PAINTING"
                }
                """;

        String response = mockMvc.perform(post("/api/contact")
                        .with(request -> { request.setRemoteAddr("192.0.2." + NEXT_IP.incrementAndGet()); return request; })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        long id = objectMapper.readTree(response).get("id").asLong();
        ContactRequest saved = repository.findById(id).orElseThrow();
        assertThat(id).isNotEqualTo(99999);
        assertThat(saved.getStatus()).isEqualTo(RequestStatus.NEW);
        assertThat(saved.getPhone()).isEqualTo("0501234567");
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.EnumSource(value = WorkType.class,
            names = {"PERGOLA_FIXED", "PERGOLA_ELECTRIC", "BALCONY_ENCLOSURE", "BOTH", "OTHER"})
    void acceptsAndPersistsAdditionalAndLegacyWorkTypes(WorkType workType) throws Exception {
        String response = mockMvc.perform(post("/api/contact")
                        .with(request -> { request.setRemoteAddr("192.0.2." + NEXT_IP.incrementAndGet()); return request; })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"New project\",\"phone\":\"0501234567\",\"workType\":\"" + workType + "\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        assertThat(repository.findById(objectMapper.readTree(response).get("id").asLong()).orElseThrow().getWorkType())
                .isEqualTo(workType);
    }

    @ParameterizedTest
    @MethodSource("invalidRequests")
    void invalidFieldsReturnBadRequestWithFieldErrors(String request, String expectedField) throws Exception {
        mockMvc.perform(post("/api/contact")
                        .with(httpRequest -> { httpRequest.setRemoteAddr("192.0.2." + NEXT_IP.incrementAndGet()); return httpRequest; })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fields." + expectedField).exists());
    }

    static Stream<Arguments> invalidRequests() {
        return Stream.of(
                Arguments.of("{\"phone\":\"0501234567\",\"workType\":\"BOTH\"}", "fullName"),
                Arguments.of("{\"fullName\":\"N\",\"phone\":\"0501234567\",\"workType\":\"BOTH\"}", "fullName"),
                Arguments.of("{\"fullName\":\"Noa Cohen\",\"phone\":\"12345\",\"workType\":\"BOTH\"}", "phone"),
                Arguments.of("{\"fullName\":\"Noa Cohen\",\"phone\":\"0501234567\",\"workType\":\"ROOFING\"}", "workType"),
                Arguments.of("{\"fullName\":\"Noa Cohen\",\"phone\":\"0501234567\",\"workType\":\"BOTH\",\"email\":\"not-an-email\"}", "email")
        );
    }

    @Test
    void malformedJsonReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/contact")
                        .with(request -> { request.setRemoteAddr("192.0.2." + NEXT_IP.incrementAndGet()); return request; })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not-json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MALFORMED_JSON"));
    }

    private String validRequest() {
        return """
                {
                  "fullName": "  Noa Cohen  ",
                  "phone": " 050-123-4567 ",
                  "email": " noa@example.com ",
                  "city": " Tel Aviv ",
                  "workType": "CLADDING",
                  "description": " Interior wall cladding "
                }
                """;
    }
}
