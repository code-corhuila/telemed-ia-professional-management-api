package com.telemed.professionalmanagement.interfaces.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.telemed.professionalmanagement.application.port.in.GetProfessionalUseCase;
import com.telemed.professionalmanagement.application.port.in.ListProfessionalsUseCase;
import com.telemed.professionalmanagement.domain.Professional;
import com.telemed.professionalmanagement.domain.ProfessionalNotFoundException;
import com.telemed.professionalmanagement.interfaces.rest.controller.ProfessionalController;
import com.telemed.professionalmanagement.interfaces.rest.exception.ApiExceptionHandler;
import com.telemed.professionalmanagement.interfaces.rest.mapper.ProfessionalRestMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProfessionalController.class)
@Import({ProfessionalRestMapper.class, ApiExceptionHandler.class,
        com.telemed.professionalmanagement.interfaces.rest.auth.TestSecurityConfiguration.class})
class ProfessionalControllerWebTest {

    private static final String ADMIN_TOKEN =
            com.telemed.professionalmanagement.interfaces.rest.auth.TestTokens.bearer("admin-1", "ADMIN");

    private static final UUID ID = UUID.fromString("c0000000-0000-4000-8000-000000000005");
    private static final UUID ACCOUNT = UUID.fromString("c1000000-0000-4000-8000-000000000099");
    private static final UUID MISSING = UUID.fromString("c0000000-0000-4000-8000-000000000404");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ListProfessionalsUseCase listProfessionalsUseCase;

    @MockBean
    private GetProfessionalUseCase getProfessionalUseCase;

    @Test
    void shouldReturnProfessionalById() throws Exception {
        when(getProfessionalUseCase.getById(ID)).thenReturn(new Professional(ID, ACCOUNT, "ABC-123", 12, com.telemed.professionalmanagement.domain.ProfessionalType.GENERAL_PRACTITIONER, com.telemed.professionalmanagement.domain.ProfessionalStatus.ACTIVE));

        mockMvc.perform(get("/api/professionals/" + ID).header("Authorization", ADMIN_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ID.toString()))
                .andExpect(jsonPath("$.professionalType").value("GENERAL_PRACTITIONER"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.specialtyId").doesNotExist());
    }

    @Test
    void shouldReturn404WhenProfessionalDoesNotExist() throws Exception {
        when(getProfessionalUseCase.getById(MISSING)).thenThrow(new ProfessionalNotFoundException(MISSING));

        mockMvc.perform(get("/api/professionals/" + MISSING).header("Authorization", ADMIN_TOKEN).header("X-Correlation-Id", "trace-404"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.traceId").value("trace-404"));
    }

    @Test
    void anIdentifierThatIsNotAUuidIsAValidationError() throws Exception {
        mockMvc.perform(get("/api/professionals/5").header("Authorization", ADMIN_TOKEN))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    @Test
    void registeringIsNotARouteOfTheCollection() throws Exception {
        // POST /admin/register registers; the collection only reads (professional-service.yaml).
        mockMvc.perform(post("/api/professionals").header("Authorization", ADMIN_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void shouldListProfessionals() throws Exception {
        when(listProfessionalsUseCase.list(new com.telemed.professionalmanagement.application.port.in.PageRequest(1, 20)))
                .thenReturn(com.telemed.professionalmanagement.application.port.in.Page.of(List.of(
                new Professional(UUID.randomUUID(), UUID.randomUUID(), "A1", 5, com.telemed.professionalmanagement.domain.ProfessionalType.GENERAL_PRACTITIONER, com.telemed.professionalmanagement.domain.ProfessionalStatus.ACTIVE),
                new Professional(UUID.randomUUID(), UUID.randomUUID(), "B2", 6, com.telemed.professionalmanagement.domain.ProfessionalType.GENERAL_PRACTITIONER, com.telemed.professionalmanagement.domain.ProfessionalStatus.ACTIVE)
        ), new com.telemed.professionalmanagement.application.port.in.PageRequest(1, 20), 42));

        mockMvc.perform(get("/api/professionals").header("Authorization", ADMIN_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].licenseNumber").value("A1"))
                .andExpect(jsonPath("$.meta.page").value(1))
                .andExpect(jsonPath("$.meta.limit").value(20))
                .andExpect(jsonPath("$.meta.total").value(42))
                .andExpect(jsonPath("$.meta.totalPages").value(3));
    }

    @Test
    void theListPassesTheRequestedPageToTheUseCase() throws Exception {
        com.telemed.professionalmanagement.application.port.in.PageRequest request =
                new com.telemed.professionalmanagement.application.port.in.PageRequest(3, 100);
        when(listProfessionalsUseCase.list(request))
                .thenReturn(com.telemed.professionalmanagement.application.port.in.Page.of(List.of(), request, 0));

        mockMvc.perform(get("/api/professionals?page=3&limit=100").header("Authorization", ADMIN_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0))
                .andExpect(jsonPath("$.meta.page").value(3))
                .andExpect(jsonPath("$.meta.limit").value(100))
                .andExpect(jsonPath("$.meta.total").value(0))
                .andExpect(jsonPath("$.meta.totalPages").value(0));
    }

    @Test
    void aPageOutOfRangeIsAValidationErrorPerField() throws Exception {
        mockMvc.perform(get("/api/professionals?page=0&limit=101").header("Authorization", ADMIN_TOKEN)
                        .header("X-Correlation-Id", "trace-page"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.details[*].field").value(org.hamcrest.Matchers.containsInAnyOrder("page", "limit")))
                .andExpect(jsonPath("$.traceId").value("trace-page"));
        mockMvc.perform(get("/api/professionals?limit=0").header("Authorization", ADMIN_TOKEN))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0].field").value("limit"));
        org.mockito.Mockito.verifyNoInteractions(listProfessionalsUseCase);
    }

    @Test
    void aPageThatIsNotAnIntegerIsAValidationError() throws Exception {
        mockMvc.perform(get("/api/professionals?page=abc").header("Authorization", ADMIN_TOKEN))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.details[0].field").value("page"));
    }
}
