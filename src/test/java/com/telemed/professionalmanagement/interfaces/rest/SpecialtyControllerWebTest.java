package com.telemed.professionalmanagement.interfaces.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.telemed.professionalmanagement.application.command.CreateSpecialtyCommand;
import com.telemed.professionalmanagement.application.command.UpdateSpecialtyCommand;
import com.telemed.professionalmanagement.application.port.in.CreateSpecialtyUseCase;
import com.telemed.professionalmanagement.application.port.in.ListSpecialtiesUseCase;
import com.telemed.professionalmanagement.application.port.in.UpdateSpecialtyUseCase;
import com.telemed.professionalmanagement.domain.Specialty;
import com.telemed.professionalmanagement.domain.SpecialtyNotFoundException;
import com.telemed.professionalmanagement.interfaces.rest.controller.SpecialtyController;
import com.telemed.professionalmanagement.interfaces.rest.dto.SpecialtyRequest;
import com.telemed.professionalmanagement.interfaces.rest.exception.ApiExceptionHandler;
import com.telemed.professionalmanagement.interfaces.rest.mapper.SpecialtyRestMapper;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SpecialtyController.class)
@Import({SpecialtyRestMapper.class, ApiExceptionHandler.class,
        com.telemed.professionalmanagement.interfaces.rest.auth.TestSecurityConfiguration.class})
class SpecialtyControllerWebTest {

    private static final String ADMIN_TOKEN =
            com.telemed.professionalmanagement.interfaces.rest.auth.TestTokens.bearer("admin-1", "ADMIN");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CreateSpecialtyUseCase createSpecialtyUseCase;

    @MockBean
    private UpdateSpecialtyUseCase updateSpecialtyUseCase;

    @MockBean
    private ListSpecialtiesUseCase listSpecialtiesUseCase;

    @Test
    void shouldCreateSpecialtySuccessfully() throws Exception {
        Specialty specialty = new Specialty(3L, "Neurology", "Brain health");
        when(createSpecialtyUseCase.create(any(com.telemed.professionalmanagement.application.port.in.Caller.class), any(CreateSpecialtyCommand.class))).thenReturn(specialty);

        String body = objectMapper.writeValueAsString(new SpecialtyRequest("Neurology", "Brain health"));

        mockMvc.perform(post("/api/specialties").header("Authorization", ADMIN_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(3))
                .andExpect(jsonPath("$.name").value("Neurology"));

        ArgumentCaptor<CreateSpecialtyCommand> commandCaptor =
                ArgumentCaptor.forClass(CreateSpecialtyCommand.class);
        verify(createSpecialtyUseCase, times(1)).create(any(com.telemed.professionalmanagement.application.port.in.Caller.class), commandCaptor.capture());
        assertEquals("Neurology", commandCaptor.getValue().name());
        assertEquals("Brain health", commandCaptor.getValue().description());
    }

    @Test
    void shouldUpdateSpecialtySuccessfully() throws Exception {
        Specialty specialty = new Specialty(3L, "Neurology", "Brain care");
        when(updateSpecialtyUseCase.update(any(com.telemed.professionalmanagement.application.port.in.Caller.class), eq(3L), any(UpdateSpecialtyCommand.class))).thenReturn(specialty);

        String body = objectMapper.writeValueAsString(new SpecialtyRequest("Neurology", "Brain care"));

        mockMvc.perform(put("/api/specialties/3").header("Authorization", ADMIN_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Brain care"));

        ArgumentCaptor<UpdateSpecialtyCommand> commandCaptor =
                ArgumentCaptor.forClass(UpdateSpecialtyCommand.class);
        verify(updateSpecialtyUseCase, times(1)).update(any(com.telemed.professionalmanagement.application.port.in.Caller.class), eq(3L), commandCaptor.capture());
        assertEquals("Neurology", commandCaptor.getValue().name());
        assertEquals("Brain care", commandCaptor.getValue().description());
    }

    @Test
    void shouldReturn404WhenSpecialtyDoesNotExist() throws Exception {
        when(updateSpecialtyUseCase.update(any(com.telemed.professionalmanagement.application.port.in.Caller.class), eq(404L), any(UpdateSpecialtyCommand.class)))
                .thenThrow(new SpecialtyNotFoundException(404L));

        String body = objectMapper.writeValueAsString(new SpecialtyRequest("Neurology", "Brain care"));

        mockMvc.perform(put("/api/specialties/404").header("Authorization", ADMIN_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"))
                .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    @Test
    void shouldListSpecialties() throws Exception {
        when(listSpecialtiesUseCase.listAll()).thenReturn(List.of(
                new Specialty(1L, "Cardiology", "Heart"),
                new Specialty(2L, "Neurology", "Brain")
        ));

        mockMvc.perform(get("/api/specialties").header("Authorization", ADMIN_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }
}
