package co.edu.corhuila.telemedia.professionalmanagement.app;

import com.fasterxml.jackson.databind.ObjectMapper;
import co.edu.corhuila.telemedia.professionalmanagement.adapter.in.http.ApiExceptionHandler;
import co.edu.corhuila.telemedia.professionalmanagement.adapter.in.http.ProfessionalController;
import co.edu.corhuila.telemedia.professionalmanagement.adapter.in.http.ProfessionalRestMapper;
import co.edu.corhuila.telemedia.professionalmanagement.application.port.in.Caller;
import co.edu.corhuila.telemedia.professionalmanagement.application.port.in.GetProfessionalUseCase;
import co.edu.corhuila.telemedia.professionalmanagement.application.port.in.ListProfessionalsUseCase;
import co.edu.corhuila.telemedia.professionalmanagement.application.port.in.ProfessionalView;
import co.edu.corhuila.telemedia.professionalmanagement.application.port.out.AccountName;
import co.edu.corhuila.telemedia.professionalmanagement.domain.model.Professional;
import co.edu.corhuila.telemedia.professionalmanagement.domain.model.ProfessionalNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProfessionalController.class)
@Import({ProfessionalRestMapper.class, ApiExceptionHandler.class,
        co.edu.corhuila.telemedia.professionalmanagement.app.TestSecurityConfiguration.class})
class ProfessionalControllerWebTest {

    private static final String ADMIN_TOKEN =
            co.edu.corhuila.telemedia.professionalmanagement.adapter.in.http.TestTokens.bearer("admin-1", "ADMIN");

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
        when(getProfessionalUseCase.getById(any(), eq(ID))).thenReturn(new ProfessionalView(new Professional(ID, ACCOUNT, "ABC-123", 12, co.edu.corhuila.telemedia.professionalmanagement.domain.model.ProfessionalType.GENERAL_PRACTITIONER, co.edu.corhuila.telemedia.professionalmanagement.domain.model.ProfessionalStatus.ACTIVE),
                new AccountName("Ana Gómez", "ana@telemed.test")));

        mockMvc.perform(get("/api/professionals/" + ID).header("Authorization", ADMIN_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ID.toString()))
                .andExpect(jsonPath("$.fullName").value("Ana Gómez"))
                .andExpect(jsonPath("$.email").value("ana@telemed.test"))
                .andExpect(jsonPath("$.professionalType").value("GENERAL_PRACTITIONER"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.specialtyId").doesNotExist());
    }

    @Test
    void shouldReturn404WhenProfessionalDoesNotExist() throws Exception {
        when(getProfessionalUseCase.getById(any(), eq(MISSING))).thenThrow(new ProfessionalNotFoundException(MISSING));

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
        when(listProfessionalsUseCase.list(any(), eq(new co.edu.corhuila.telemedia.professionalmanagement.application.port.in.PageRequest(1, 20))))
                .thenReturn(co.edu.corhuila.telemedia.professionalmanagement.application.port.in.Page.of(List.of(
                new ProfessionalView(new Professional(UUID.randomUUID(), UUID.randomUUID(), "A1", 5, co.edu.corhuila.telemedia.professionalmanagement.domain.model.ProfessionalType.GENERAL_PRACTITIONER, co.edu.corhuila.telemedia.professionalmanagement.domain.model.ProfessionalStatus.ACTIVE),
                        new AccountName("Ana Gómez", "ana@telemed.test")),
                new ProfessionalView(new Professional(UUID.randomUUID(), UUID.randomUUID(), "B2", 6, co.edu.corhuila.telemedia.professionalmanagement.domain.model.ProfessionalType.GENERAL_PRACTITIONER, co.edu.corhuila.telemedia.professionalmanagement.domain.model.ProfessionalStatus.ACTIVE),
                        null)
        ), new co.edu.corhuila.telemedia.professionalmanagement.application.port.in.PageRequest(1, 20), 42));

        mockMvc.perform(get("/api/professionals").header("Authorization", ADMIN_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].licenseNumber").value("A1"))
                .andExpect(jsonPath("$.data[0].fullName").value("Ana Gómez"))
                .andExpect(jsonPath("$.data[0].email").value("ana@telemed.test"))
                .andExpect(jsonPath("$.data[1].fullName").doesNotExist())
                .andExpect(jsonPath("$.data[1].email").doesNotExist())
                .andExpect(jsonPath("$.meta.page").value(1))
                .andExpect(jsonPath("$.meta.limit").value(20))
                .andExpect(jsonPath("$.meta.total").value(42))
                .andExpect(jsonPath("$.meta.totalPages").value(3));
    }

    @Test
    void theListPassesTheRequestedPageToTheUseCase() throws Exception {
        co.edu.corhuila.telemedia.professionalmanagement.application.port.in.PageRequest request =
                new co.edu.corhuila.telemedia.professionalmanagement.application.port.in.PageRequest(3, 100);
        when(listProfessionalsUseCase.list(any(), eq(request)))
                .thenReturn(co.edu.corhuila.telemedia.professionalmanagement.application.port.in.Page.of(List.of(), request, 0));

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
    void theListAndTheDetailReceiveTheCallerOfTheToken() throws Exception {
        // DEC-009: the use case decides what the caller may see, so it needs who calls.
        Caller patient = new Caller("patient-1", Set.of("PATIENT"));
        co.edu.corhuila.telemedia.professionalmanagement.application.port.in.PageRequest request =
                new co.edu.corhuila.telemedia.professionalmanagement.application.port.in.PageRequest(1, 20);
        when(listProfessionalsUseCase.list(any(), any()))
                .thenReturn(co.edu.corhuila.telemedia.professionalmanagement.application.port.in.Page.of(List.of(), request, 0));
        when(getProfessionalUseCase.getById(any(), any())).thenReturn(new ProfessionalView(new Professional(ID, ACCOUNT,
                "ABC-123", 12, co.edu.corhuila.telemedia.professionalmanagement.domain.model.ProfessionalType.GENERAL_PRACTITIONER,
                co.edu.corhuila.telemedia.professionalmanagement.domain.model.ProfessionalStatus.ACTIVE), null));
        String token = co.edu.corhuila.telemedia.professionalmanagement.adapter.in.http.TestTokens.bearer("patient-1", "PATIENT");

        mockMvc.perform(get("/api/professionals").header("Authorization", token)).andExpect(status().isOk());
        mockMvc.perform(get("/api/professionals/" + ID).header("Authorization", token)).andExpect(status().isOk());

        verify(listProfessionalsUseCase).list(patient, request);
        verify(getProfessionalUseCase).getById(patient, ID);
    }

    @Test
    void aPageThatIsNotAnIntegerIsAValidationError() throws Exception {
        mockMvc.perform(get("/api/professionals?page=abc").header("Authorization", ADMIN_TOKEN))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.details[0].field").value("page"));
    }
}
