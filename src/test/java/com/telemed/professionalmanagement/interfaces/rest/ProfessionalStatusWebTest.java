package com.telemed.professionalmanagement.interfaces.rest;

import static org.hamcrest.Matchers.hasItem;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.telemed.professionalmanagement.application.port.in.AccessDeniedException;
import com.telemed.professionalmanagement.application.port.in.Caller;
import com.telemed.professionalmanagement.application.port.in.ChangeProfessionalStatusUseCase;
import com.telemed.professionalmanagement.domain.Professional;
import com.telemed.professionalmanagement.domain.ProfessionalNotFoundException;
import com.telemed.professionalmanagement.domain.ProfessionalStatus;
import com.telemed.professionalmanagement.domain.ProfessionalType;
import com.telemed.professionalmanagement.interfaces.rest.auth.TestSecurityConfiguration;
import com.telemed.professionalmanagement.interfaces.rest.auth.TestTokens;
import com.telemed.professionalmanagement.interfaces.rest.controller.ProfessionalStatusController;
import com.telemed.professionalmanagement.interfaces.rest.exception.ApiExceptionHandler;
import com.telemed.professionalmanagement.interfaces.rest.mapper.ProfessionalRestMapper;

/** {@code PATCH /{id}/status} of professional-service.yaml (DEC-007), at the HTTP edge. */
@WebMvcTest(ProfessionalStatusController.class)
@Import({ProfessionalRestMapper.class, ApiExceptionHandler.class, TestSecurityConfiguration.class})
class ProfessionalStatusWebTest {

    private static final String ADMIN = TestTokens.bearer("admin-1", "ADMIN");
    private static final UUID ID = UUID.fromString("c0000000-0000-4000-8000-000000000042");
    private static final Professional INACTIVE = new Professional(ID,
            UUID.fromString("c1000000-0000-4000-8000-000000000042"), "RM-4200", 4,
            ProfessionalType.GENERAL_PRACTITIONER, ProfessionalStatus.INACTIVE);

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ChangeProfessionalStatusUseCase statusChange;

    private ResultActions change(String id, String body) throws Exception {
        return mockMvc.perform(patch("/api/professionals/" + id + "/status").header("Authorization", ADMIN)
                .header("X-Correlation-Id", "trace-status").contentType(MediaType.APPLICATION_JSON).content(body));
    }

    @Test
    void anAdministratorSetsTheStatusAndGetsTheProfessional() throws Exception {
        when(statusChange.changeStatus(any(Caller.class), eq(ID), eq(ProfessionalStatus.INACTIVE))).thenReturn(INACTIVE);

        change(ID.toString(), "{\"status\":\"INACTIVE\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ID.toString()))
                .andExpect(jsonPath("$.status").value("INACTIVE"));
        verify(statusChange).changeStatus(any(Caller.class), eq(ID), eq(ProfessionalStatus.INACTIVE));
    }

    @Test
    void aStatusOutsideTheContractIs400() throws Exception {
        change(ID.toString(), "{\"status\":\"SUSPENDED\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.details[*].field").value(hasItem("status")))
                .andExpect(jsonPath("$.traceId").value("trace-status"));
        verifyNoInteractions(statusChange);
    }

    @Test
    void aMissingStatusOrAnUnknownFieldIs400() throws Exception {
        change(ID.toString(), "{}").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[*].field").value(hasItem("status")));
        change(ID.toString(), "{\"status\":\"ACTIVE\",\"licenseNumber\":\"X\"}").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[*].field").value(hasItem("licenseNumber")));
        change(ID.toString(), "[]").andExpect(status().isBadRequest());
        verifyNoInteractions(statusChange);
    }

    @Test
    void anIdThatIsNotAUuidIs400() throws Exception {
        change("5", "{\"status\":\"ACTIVE\"}").andExpect(status().isBadRequest());
        verifyNoInteractions(statusChange);
    }

    @Test
    void anUnknownProfessionalIs404() throws Exception {
        when(statusChange.changeStatus(any(Caller.class), eq(ID), any(ProfessionalStatus.class)))
                .thenThrow(new ProfessionalNotFoundException(ID));

        change(ID.toString(), "{\"status\":\"ACTIVE\"}").andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    @Test
    void aNonAdministratorIs403() throws Exception {
        when(statusChange.changeStatus(any(Caller.class), eq(ID), any(ProfessionalStatus.class)))
                .thenThrow(new AccessDeniedException());

        change(ID.toString(), "{\"status\":\"ACTIVE\"}").andExpect(status().isForbidden());
    }
}
