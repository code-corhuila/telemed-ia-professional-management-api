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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SpecialtyController.class)
@Import({SpecialtyRestMapper.class, ApiExceptionHandler.class})
class SpecialtyControllerWebTest {

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
        when(createSpecialtyUseCase.create(any(CreateSpecialtyCommand.class))).thenReturn(specialty);

        String body = objectMapper.writeValueAsString(new SpecialtyRequest("Neurology", "Brain health"));

        mockMvc.perform(post("/api/specialties")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(3))
                .andExpect(jsonPath("$.name").value("Neurology"));
    }

    @Test
    void shouldUpdateSpecialtySuccessfully() throws Exception {
        Specialty specialty = new Specialty(3L, "Neurology", "Brain care");
        when(updateSpecialtyUseCase.update(eq(3L), any(UpdateSpecialtyCommand.class))).thenReturn(specialty);

        String body = objectMapper.writeValueAsString(new SpecialtyRequest("Neurology", "Brain care"));

        mockMvc.perform(put("/api/specialties/3")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Brain care"));
    }

    @Test
    void shouldReturn404WhenSpecialtyDoesNotExist() throws Exception {
        when(updateSpecialtyUseCase.update(eq(404L), any(UpdateSpecialtyCommand.class)))
                .thenThrow(new SpecialtyNotFoundException(404L));

        String body = objectMapper.writeValueAsString(new SpecialtyRequest("Neurology", "Brain care"));

        mockMvc.perform(put("/api/specialties/404")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Specialty not found with id: 404"));
    }

    @Test
    void shouldListSpecialties() throws Exception {
        when(listSpecialtiesUseCase.listAll()).thenReturn(List.of(
                new Specialty(1L, "Cardiology", "Heart"),
                new Specialty(2L, "Neurology", "Brain")
        ));

        mockMvc.perform(get("/api/specialties"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }
}
