package com.telemed.professionalmanagement.interfaces.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.telemed.professionalmanagement.application.command.RegisterProfessionalCommand;
import com.telemed.professionalmanagement.application.port.in.GetProfessionalUseCase;
import com.telemed.professionalmanagement.application.port.in.ListProfessionalsUseCase;
import com.telemed.professionalmanagement.application.port.in.RegisterProfessionalUseCase;
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
@Import({ProfessionalRestMapper.class, ApiExceptionHandler.class})
class ProfessionalControllerWebTest {

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
        Professional professional = new Professional(5L, 99L, "ABC-123", 7L, 12);
        when(registerProfessionalUseCase.register(any(RegisterProfessionalCommand.class))).thenReturn(professional);

        String body = objectMapper.writeValueAsString(new com.telemed.professionalmanagement.interfaces.rest.dto.ProfessionalRequest(
                99L, "ABC-123", 7L, 12
        ));

        mockMvc.perform(post("/api/professionals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.identityUserId").value(99))
                .andExpect(jsonPath("$.licenseNumber").value("ABC-123"));

        ArgumentCaptor<RegisterProfessionalCommand> commandCaptor =
                ArgumentCaptor.forClass(RegisterProfessionalCommand.class);
        verify(registerProfessionalUseCase, times(1)).register(commandCaptor.capture());
        RegisterProfessionalCommand command = commandCaptor.getValue();
        assertEquals(99L, command.identityUserId());
        assertEquals("ABC-123", command.licenseNumber());
        assertEquals(7L, command.specialtyId());
        assertEquals(12, command.yearsExperience());
    }

    @Test
    void shouldReturnProfessionalById() throws Exception {
        when(getProfessionalUseCase.getById(5L)).thenReturn(new Professional(5L, 99L, "ABC-123", 7L, 12));

        mockMvc.perform(get("/api/professionals/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.specialtyId").value(7));
    }

    @Test
    void shouldReturn404WhenProfessionalDoesNotExist() throws Exception {
        when(getProfessionalUseCase.getById(404L)).thenThrow(new ProfessionalNotFoundException(404L));

        mockMvc.perform(get("/api/professionals/404"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Professional not found with id: 404"));
    }

    @Test
    void shouldFailValidationWhenRequestIsInvalid() throws Exception {
        String body = objectMapper.writeValueAsString(new com.telemed.professionalmanagement.interfaces.rest.dto.ProfessionalRequest(
                null, "", 7L, -1
        ));

        mockMvc.perform(post("/api/professionals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldListProfessionals() throws Exception {
        when(listProfessionalsUseCase.listAll()).thenReturn(List.of(
                new Professional(1L, 10L, "A1", 1L, 5),
                new Professional(2L, 11L, "B2", 2L, 6)
        ));

        mockMvc.perform(get("/api/professionals"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }
}
