package com.telemed.professionalmanagement.interfaces.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.telemed.professionalmanagement.application.command.RegisterProfessionalCommand;
import com.telemed.professionalmanagement.application.port.in.GetProfessionalUseCase;
import com.telemed.professionalmanagement.application.port.in.ListProfessionalsUseCase;
import com.telemed.professionalmanagement.application.port.in.RegisterProfessionalUseCase;
import com.telemed.professionalmanagement.domain.DuplicateProfessionalIdentityException;
import com.telemed.professionalmanagement.domain.DuplicateProfessionalLicenseException;
import com.telemed.professionalmanagement.domain.Professional;
import com.telemed.professionalmanagement.domain.ProfessionalNotFoundException;
import com.telemed.professionalmanagement.interfaces.rest.controller.ProfessionalController;
import com.telemed.professionalmanagement.interfaces.rest.exception.ApiExceptionHandler;
import com.telemed.professionalmanagement.interfaces.rest.mapper.ProfessionalRestMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
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

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RegisterProfessionalUseCase registerProfessionalUseCase;

    @MockBean
    private ListProfessionalsUseCase listProfessionalsUseCase;

    @MockBean
    private GetProfessionalUseCase getProfessionalUseCase;

    @Test
    void shouldCreateProfessionalSuccessfully() throws Exception {
        Professional professional = new Professional(5L, 99L, "ABC-123", 12, com.telemed.professionalmanagement.domain.ProfessionalType.GENERAL_PRACTITIONER, com.telemed.professionalmanagement.domain.ProfessionalStatus.ACTIVE);
        when(registerProfessionalUseCase.register(any(com.telemed.professionalmanagement.application.port.in.Caller.class), any(RegisterProfessionalCommand.class))).thenReturn(professional);

        String body = objectMapper.writeValueAsString(new com.telemed.professionalmanagement.interfaces.rest.dto.ProfessionalRequest(99L, "ABC-123", 12));

        mockMvc.perform(post("/api/professionals").header("Authorization", ADMIN_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.identityUserId").value(99))
                .andExpect(jsonPath("$.licenseNumber").value("ABC-123"));

        ArgumentCaptor<RegisterProfessionalCommand> commandCaptor =
                ArgumentCaptor.forClass(RegisterProfessionalCommand.class);
        verify(registerProfessionalUseCase, times(1)).register(any(com.telemed.professionalmanagement.application.port.in.Caller.class), commandCaptor.capture());
        RegisterProfessionalCommand command = commandCaptor.getValue();
        assertEquals(99L, command.identityUserId());
        assertEquals("ABC-123", command.licenseNumber());
        assertEquals(12, command.yearsExperience());
    }

    @Test
    void shouldReturnProfessionalById() throws Exception {
        when(getProfessionalUseCase.getById(5L)).thenReturn(new Professional(5L, 99L, "ABC-123", 12, com.telemed.professionalmanagement.domain.ProfessionalType.GENERAL_PRACTITIONER, com.telemed.professionalmanagement.domain.ProfessionalStatus.ACTIVE));

        mockMvc.perform(get("/api/professionals/5").header("Authorization", ADMIN_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.professionalType").value("GENERAL_PRACTITIONER"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.specialtyId").doesNotExist());
    }

    @Test
    void shouldReturn404WhenProfessionalDoesNotExist() throws Exception {
        when(getProfessionalUseCase.getById(404L)).thenThrow(new ProfessionalNotFoundException(404L));

        mockMvc.perform(get("/api/professionals/404").header("Authorization", ADMIN_TOKEN).header("X-Correlation-Id", "trace-404"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.traceId").value("trace-404"));
    }

    @Test
    void shouldFailValidationWhenRequestIsInvalid() throws Exception {
        String body = objectMapper.writeValueAsString(new com.telemed.professionalmanagement.interfaces.rest.dto.ProfessionalRequest(null, "", -1));

        mockMvc.perform(post("/api/professionals").header("Authorization", ADMIN_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.details[*].field").value(org.hamcrest.Matchers.hasItems(
                        "identityUserId", "licenseNumber", "yearsExperience")))
                .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    @Test
    void shouldReturnConflictWhenProfessionalLicenseIsDuplicate() throws Exception {
        when(registerProfessionalUseCase.register(any(com.telemed.professionalmanagement.application.port.in.Caller.class), any(RegisterProfessionalCommand.class)))
                .thenThrow(new DuplicateProfessionalLicenseException("ABC-123"));

        String body = objectMapper.writeValueAsString(new com.telemed.professionalmanagement.interfaces.rest.dto.ProfessionalRequest(99L, "ABC-123", 12));

        mockMvc.perform(post("/api/professionals").header("Authorization", ADMIN_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("CONFLICT"))
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.traceId").isNotEmpty())
                .andExpect(jsonPath("$.timestamp").doesNotExist())
                .andExpect(jsonPath("$.path").doesNotExist());
    }

    @Test
    void shouldReturnConflictWhenProfessionalIdentityIsDuplicate() throws Exception {
        when(registerProfessionalUseCase.register(any(com.telemed.professionalmanagement.application.port.in.Caller.class), any(RegisterProfessionalCommand.class)))
                .thenThrow(new DuplicateProfessionalIdentityException(99L));

        String body = objectMapper.writeValueAsString(new com.telemed.professionalmanagement.interfaces.rest.dto.ProfessionalRequest(99L, "ABC-123", 12));

        mockMvc.perform(post("/api/professionals").header("Authorization", ADMIN_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("CONFLICT"))
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.traceId").isNotEmpty())
                .andExpect(jsonPath("$.timestamp").doesNotExist())
                .andExpect(jsonPath("$.path").doesNotExist());
    }

    @Test
    void shouldListProfessionals() throws Exception {
        when(listProfessionalsUseCase.listAll()).thenReturn(List.of(
                new Professional(1L, 10L, "A1", 5, com.telemed.professionalmanagement.domain.ProfessionalType.GENERAL_PRACTITIONER, com.telemed.professionalmanagement.domain.ProfessionalStatus.ACTIVE),
                new Professional(2L, 11L, "B2", 6, com.telemed.professionalmanagement.domain.ProfessionalType.GENERAL_PRACTITIONER, com.telemed.professionalmanagement.domain.ProfessionalStatus.ACTIVE)
        ));

        mockMvc.perform(get("/api/professionals").header("Authorization", ADMIN_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }
}
