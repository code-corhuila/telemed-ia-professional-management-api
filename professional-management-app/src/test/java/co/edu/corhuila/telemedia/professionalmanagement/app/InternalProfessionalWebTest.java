package co.edu.corhuila.telemedia.professionalmanagement.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.edu.corhuila.telemedia.professionalmanagement.adapter.in.http.ApiExceptionHandler;
import co.edu.corhuila.telemedia.professionalmanagement.adapter.in.http.InternalProfessionalController;
import co.edu.corhuila.telemedia.professionalmanagement.adapter.in.http.ProfessionalRestMapper;
import co.edu.corhuila.telemedia.professionalmanagement.adapter.in.http.TestTokens;
import co.edu.corhuila.telemedia.professionalmanagement.application.port.in.AccessDeniedException;
import co.edu.corhuila.telemedia.professionalmanagement.application.port.in.Caller;
import co.edu.corhuila.telemedia.professionalmanagement.application.port.in.CreateProfessionalProfileUseCase;
import co.edu.corhuila.telemedia.professionalmanagement.application.port.in.IdempotencyKeyReusedException;
import co.edu.corhuila.telemedia.professionalmanagement.application.port.in.ProfessionalProfileCommand;
import co.edu.corhuila.telemedia.professionalmanagement.application.port.in.Registration;
import co.edu.corhuila.telemedia.professionalmanagement.domain.model.DuplicateProfessionalLicenseException;
import co.edu.corhuila.telemedia.professionalmanagement.domain.model.Professional;
import co.edu.corhuila.telemedia.professionalmanagement.domain.model.ProfessionalStatus;
import co.edu.corhuila.telemedia.professionalmanagement.domain.model.ProfessionalType;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/** {@code POST /internal/professionals} of professional-service.yaml (DEC-015), at the HTTP edge. */
@WebMvcTest(InternalProfessionalController.class)
@Import({ProfessionalRestMapper.class, ApiExceptionHandler.class, TestSecurityConfiguration.class})
class InternalProfessionalWebTest {

    private static final String ROUTE = "/internal/professionals";
    private static final String WORKFLOW = TestTokens.bearer("workflow", "SERVICE");
    private static final String KEY = "saga-7:create-profile";
    private static final UUID ACCOUNT = UUID.fromString("c1000000-0000-4000-8000-000000000081");
    private static final String BODY = """
            {"accountId":"%s","licenseNumber":"RM-8100","yearsExperience":5}""".formatted(ACCOUNT);
    private static final Professional CREATED = new Professional(
            UUID.fromString("c0000000-0000-4000-8000-000000000081"), ACCOUNT, "RM-8100", 5,
            ProfessionalType.GENERAL_PRACTITIONER, ProfessionalStatus.ACTIVE);

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CreateProfessionalProfileUseCase profiles;

    private ResultActions create(String key, String body) throws Exception {
        var request = post(ROUTE).header("Authorization", WORKFLOW).header("X-Correlation-Id", "trace-saga")
                .contentType(MediaType.APPLICATION_JSON).content(body);
        if (key != null) {
            request.header("Idempotency-Key", key);
        }
        return mockMvc.perform(request);
    }

    private void whenCreating(Object outcome) {
        var stub = when(profiles.create(any(Caller.class), eq(KEY), any(ProfessionalProfileCommand.class)));
        if (outcome instanceof RuntimeException failure) {
            stub.thenThrow(failure);
        } else {
            stub.thenReturn((Registration) outcome);
        }
    }

    @Test
    void theFirstRequestIs201WithTheLocationOfTheProfessional() throws Exception {
        whenCreating(new Registration(CREATED, true));

        create(KEY, BODY)
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/professionals/" + CREATED.getId()))
                .andExpect(header().string("X-Correlation-Id", "trace-saga"))
                .andExpect(jsonPath("$.id").value(CREATED.getId().toString()))
                .andExpect(jsonPath("$.identityUserId").value(ACCOUNT.toString()))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        ArgumentCaptor<Caller> caller = ArgumentCaptor.forClass(Caller.class);
        ArgumentCaptor<ProfessionalProfileCommand> command = ArgumentCaptor.forClass(ProfessionalProfileCommand.class);
        verify(profiles).create(caller.capture(), eq(KEY), command.capture());
        assertThat(caller.getValue().subject()).isEqualTo("workflow");
        assertThat(caller.getValue().hasRole("SERVICE")).isTrue();
        assertThat(command.getValue()).isEqualTo(new ProfessionalProfileCommand(ACCOUNT, "RM-8100", 5));
    }

    @Test
    void aRetryWithTheSameKeyAndBodyIs200() throws Exception {
        whenCreating(new Registration(CREATED, false));

        create(KEY, BODY)
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("Location"))
                .andExpect(jsonPath("$.id").value(CREATED.getId().toString()));
    }

    @Test
    void theKeyAndTheBodyAreValidatedTogether() throws Exception {
        create(null, "{\"licenseNumber\":\"\",\"yearsExperience\":-1}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.details[?(@.field=='Idempotency-Key')]").exists())
                .andExpect(jsonPath("$.details[?(@.field=='accountId')]").exists())
                .andExpect(jsonPath("$.details[?(@.field=='licenseNumber')]").exists())
                .andExpect(jsonPath("$.details[?(@.field=='yearsExperience')]").exists())
                .andExpect(jsonPath("$.traceId").value("trace-saga"));
        verifyNoInteractions(profiles);
    }

    @Test
    void anUnknownFieldIsRejected() throws Exception {
        create(KEY, "{\"accountId\":\"%s\",\"licenseNumber\":\"RM-8100\",\"yearsExperience\":5,\"status\":\"INACTIVE\"}"
                .formatted(ACCOUNT))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(profiles);
    }

    @Test
    void aCallerThatIsNotTheWorkflowGets403() throws Exception {
        whenCreating(new AccessDeniedException());

        create(KEY, BODY).andExpect(status().isForbidden()).andExpect(jsonPath("$.error").value("FORBIDDEN"));
    }

    @Test
    void aLicenseAlreadyRegisteredIs409() throws Exception {
        whenCreating(new DuplicateProfessionalLicenseException("RM-8100"));

        create(KEY, BODY).andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("CONFLICT"));
    }

    @Test
    void theSameKeyWithAnotherBodyIs422() throws Exception {
        whenCreating(new IdempotencyKeyReusedException());

        create(KEY, BODY)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("BUSINESS_RULE_VIOLATION"));
    }

    @Test
    void withoutATokenItIs401() throws Exception {
        mockMvc.perform(post(ROUTE).header("Idempotency-Key", KEY).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(profiles);
    }
}
