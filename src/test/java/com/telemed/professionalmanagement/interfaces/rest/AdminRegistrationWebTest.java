package com.telemed.professionalmanagement.interfaces.rest;

import static org.hamcrest.Matchers.hasItems;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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

import com.telemed.professionalmanagement.application.command.ProfessionalRegistrationCommand;
import com.telemed.professionalmanagement.application.port.in.AccessDeniedException;
import com.telemed.professionalmanagement.application.port.in.Caller;
import com.telemed.professionalmanagement.application.port.in.IdempotencyKeyReusedException;
import com.telemed.professionalmanagement.application.port.in.RegisterProfessionalAccountUseCase;
import com.telemed.professionalmanagement.application.port.in.Registration;
import com.telemed.professionalmanagement.application.port.out.IdentityAccountConflictException;
import com.telemed.professionalmanagement.application.port.out.IdentityAccountRejectedException;
import com.telemed.professionalmanagement.application.port.out.IdentityUnavailableException;
import com.telemed.professionalmanagement.domain.DuplicateProfessionalLicenseException;
import com.telemed.professionalmanagement.domain.Professional;
import com.telemed.professionalmanagement.domain.ProfessionalStatus;
import com.telemed.professionalmanagement.domain.ProfessionalType;
import com.telemed.professionalmanagement.interfaces.rest.auth.TestSecurityConfiguration;
import com.telemed.professionalmanagement.interfaces.rest.auth.TestTokens;
import com.telemed.professionalmanagement.interfaces.rest.controller.ProfessionalRegistrationController;
import com.telemed.professionalmanagement.interfaces.rest.exception.ApiExceptionHandler;
import com.telemed.professionalmanagement.interfaces.rest.mapper.ProfessionalRestMapper;

/** {@code POST /admin/register} of professional-service.yaml, at the HTTP edge. */
@WebMvcTest(ProfessionalRegistrationController.class)
@Import({ProfessionalRestMapper.class, ApiExceptionHandler.class, TestSecurityConfiguration.class})
class AdminRegistrationWebTest {

    private static final String ROUTE = "/api/professionals/admin/register";
    private static final String ADMIN = TestTokens.bearer("admin-1", "ADMIN");
    private static final String KEY = "register-web-0001";
    private static final String BODY = """
            {"fullName":"Ana Gómez","email":"ana@telemed.test","identityDocument":"1075000001",
             "password":"temporary-pass-1","licenseNumber":"RM-1001","yearsExperience":6}""";
    private static final Professional REGISTERED = new Professional(
            UUID.fromString("c0000000-0000-4000-8000-000000000021"),
            UUID.fromString("c1000000-0000-4000-8000-000000000021"),
            "RM-1001", 6, ProfessionalType.GENERAL_PRACTITIONER, ProfessionalStatus.ACTIVE);

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RegisterProfessionalAccountUseCase registration;

    private ResultActions register(String key, String body) throws Exception {
        var request = post(ROUTE).header("Authorization", ADMIN).header("X-Correlation-Id", "trace-reg")
                .contentType(MediaType.APPLICATION_JSON).content(body);
        if (key != null) {
            request.header("Idempotency-Key", key);
        }
        return mockMvc.perform(request);
    }

    private void whenRegistering(Object outcome) {
        var stub = when(registration.register(any(Caller.class), eq(KEY), any(ProfessionalRegistrationCommand.class)));
        if (outcome instanceof RuntimeException failure) {
            stub.thenThrow(failure);
        } else {
            stub.thenReturn((Registration) outcome);
        }
    }

    @Test
    void theFirstRegistrationIs201WithItsLocation() throws Exception {
        whenRegistering(new Registration(REGISTERED, true));

        register(KEY, BODY)
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/professionals/" + REGISTERED.getId()))
                .andExpect(jsonPath("$.id").value(REGISTERED.getId().toString()))
                .andExpect(jsonPath("$.licenseNumber").value("RM-1001"))
                .andExpect(jsonPath("$.professionalType").value("GENERAL_PRACTITIONER"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.password").doesNotExist());

        verify(registration).register(any(Caller.class), eq(KEY), eq(new ProfessionalRegistrationCommand(
                "Ana Gómez", "ana@telemed.test", "1075000001", "temporary-pass-1", "RM-1001", 6)));
    }

    @Test
    void aRetryWithTheSameKeyIs200WithTheSameProfessional() throws Exception {
        whenRegistering(new Registration(REGISTERED, false));

        register(KEY, BODY)
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("Location"))
                .andExpect(jsonPath("$.id").value(REGISTERED.getId().toString()));
    }

    @Test
    void anInvalidBodyNamesEveryInvalidFieldIncludingTheIdempotencyKeyHeader() throws Exception {
        register(null, """
                {"fullName":"","email":"not-an-email","identityDocument":"","password":"short",
                 "licenseNumber":"","yearsExperience":-1}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.details[*].field").value(hasItems("Idempotency-Key", "fullName", "email",
                        "identityDocument", "password", "licenseNumber", "yearsExperience")))
                .andExpect(jsonPath("$.traceId").value("trace-reg"));
        verifyNoInteractions(registration);
    }

    @Test
    void anIdempotencyKeyOutsideEightTo128CharactersIs400() throws Exception {
        register("short", BODY)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0].field").value("Idempotency-Key"));
        register("k".repeat(129), BODY).andExpect(status().isBadRequest());
        verifyNoInteractions(registration);
    }

    @Test
    void aNonAdministratorIs403() throws Exception {
        whenRegistering(new AccessDeniedException());

        register(KEY, BODY).andExpect(status().isForbidden()).andExpect(jsonPath("$.error").value("FORBIDDEN"));
    }

    @Test
    void anAccountOrLicenseAlreadyRegisteredIs409() throws Exception {
        whenRegistering(new IdentityAccountConflictException());
        register(KEY, BODY).andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("CONFLICT"));
    }

    @Test
    void aRepeatedLicenseIs409() throws Exception {
        whenRegistering(new DuplicateProfessionalLicenseException("RM-1001"));
        register(KEY, BODY).andExpect(status().isConflict());
    }

    @Test
    void theSameKeyWithAnotherBodyIs422() throws Exception {
        whenRegistering(new IdempotencyKeyReusedException());

        register(KEY, BODY)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("BUSINESS_RULE_VIOLATION"));
    }

    @Test
    void accountDataTheIdentityServiceRejectsIs400() throws Exception {
        whenRegistering(new IdentityAccountRejectedException());

        register(KEY, BODY)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.traceId").value("trace-reg"));
    }

    @Test
    void anUnavailableIdentityServiceIs503WithoutItsReason() throws Exception {
        whenRegistering(new IdentityUnavailableException("internal reason"));

        register(KEY, BODY)
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error").value("SERVICE_UNAVAILABLE"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.not("internal reason")))
                .andExpect(jsonPath("$.traceId").value("trace-reg"));
    }
}
