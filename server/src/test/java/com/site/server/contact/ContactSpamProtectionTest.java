package com.site.server.contact;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ContactSpamProtectionTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ContactRequestRepository repository;

    @MockBean
    private NotificationService notifications;

    @BeforeEach
    void clearRows() {
        repository.deleteAll();
    }

    @Test
    void filledHoneypotReturnsReceiptWithoutSavingOrNotifyingEvenWithMissingFields() throws Exception {
        mockMvc.perform(request("198.51.100.1", "{\"website\":\"https://spam.example\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.createdAt").isNotEmpty());

        assertThat(repository.count()).isZero();
        verify(notifications, after(200).never()).notifyNewRequest(any());
    }

    @Test
    void thirdRequestFromSameIpIsLimitedAndOtherRoutesAndIpsStillWork() throws Exception {
        for (int index = 0; index < 2; index++) {
            mockMvc.perform(request("198.51.100.2", "{\"website\":\"bot\"}"))
                    .andExpect(status().isCreated());
        }
        mockMvc.perform(request("198.51.100.2", "{\"website\":\"bot\"}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error").value("TOO_MANY_REQUESTS"));
        mockMvc.perform(request("198.51.100.3", "{\"website\":\"bot\"}"))
                .andExpect(status().isCreated());
        mockMvc.perform(get("/api/health").with(request -> {
            request.setRemoteAddr("198.51.100.2");
            return request;
        })).andExpect(status().isOk());
    }

    @Test
    void validRequestNotifiesOnceOnAnotherThreadAfterRowIsCommitted() throws Exception {
        Thread requestThread = Thread.currentThread();
        CompletableFuture<Void> delivered = new CompletableFuture<>();
        doAnswer(invocation -> {
            try {
                assertThat(Thread.currentThread()).isNotSameAs(requestThread);
                assertThat(repository.count()).isEqualTo(1);
                assertThat(repository.findAll().getFirst().getStatus()).isEqualTo(RequestStatus.NEW);
                delivered.complete(null);
            } catch (Throwable error) {
                delivered.completeExceptionally(error);
            }
            return null;
        }).when(notifications).notifyNewRequest(any());

        mockMvc.perform(request("198.51.100.4", validBody()))
                .andExpect(status().isCreated());
        delivered.get(3, TimeUnit.SECONDS);
        verify(notifications, after(100).times(1)).notifyNewRequest(any());
        assertThat(repository.count()).isEqualTo(1);
    }

    @Test
    void notificationFailureStillReturnsCreatedAndKeepsTheRow() throws Exception {
        doThrow(new IllegalStateException("mail failed")).when(notifications).notifyNewRequest(any());

        mockMvc.perform(request("198.51.100.5", validBody()))
                .andExpect(status().isCreated());

        verify(notifications, timeout(3000).times(1)).notifyNewRequest(any());
        assertThat(repository.count()).isEqualTo(1);
    }

    private MockHttpServletRequestBuilder request(String ip, String body) {
        return post("/api/contact").with(request -> {
            request.setRemoteAddr(ip);
            return request;
        }).contentType(MediaType.APPLICATION_JSON).content(body);
    }

    private String validBody() {
        return """
                {"fullName":"Noa Cohen","phone":"0501234567","workType":"CLADDING"}
                """;
    }
}
