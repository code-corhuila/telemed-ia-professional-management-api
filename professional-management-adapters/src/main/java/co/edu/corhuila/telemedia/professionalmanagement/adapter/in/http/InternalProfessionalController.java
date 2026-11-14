package co.edu.corhuila.telemedia.professionalmanagement.adapter.in.http;

import co.edu.corhuila.telemedia.professionalmanagement.adapter.in.http.ApiError.FieldDetail;
import co.edu.corhuila.telemedia.professionalmanagement.application.port.in.CreateProfessionalProfileUseCase;
import co.edu.corhuila.telemedia.professionalmanagement.application.port.in.ProfessionalProfileCommand;
import co.edu.corhuila.telemedia.professionalmanagement.application.port.in.Registration;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Validator;
import java.net.URI;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * {@code POST /internal/professionals} of professional-service.yaml (DEC-015): step 2 of the
 * onboarding saga. Reached only inside the platform network (the gateway does not route it);
 * who may call it is decided by the use case. The {@code Idempotency-Key} header and the body
 * are validated together, as in {@code /admin/register}. The body is closed
 * ({@code additionalProperties: false}): it is read as a tree so unknown fields are rejected here.
 */
@RestController
@RequestMapping(path = "/internal/professionals", produces = MediaType.APPLICATION_JSON_VALUE)
public class InternalProfessionalController {

    private static final Set<String> FIELDS = Set.of("accountId", "licenseNumber", "yearsExperience");

    private final CreateProfessionalProfileUseCase profiles;
    private final ProfessionalRestMapper mapper;
    private final Validator validator;
    private final ObjectMapper json;

    public InternalProfessionalController(CreateProfessionalProfileUseCase profiles,
                                          ProfessionalRestMapper mapper, Validator validator, ObjectMapper json) {
        this.profiles = profiles;
        this.mapper = mapper;
        this.validator = validator;
        this.json = json;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ProfessionalResponse> create(HttpServletRequest http,
            @RequestHeader(name = ProfessionalRegistrationController.IDEMPOTENCY_KEY, required = false) String key,
            @RequestBody JsonNode tree) {
        List<FieldDetail> invalid = new ArrayList<>();
        ProfessionalRegistrationController.checkIdempotencyKey(key, invalid);
        CreateProfileRequest body = read(tree, invalid);
        validator.validate(body).stream()
                .map(violation -> new FieldDetail(violation.getPropertyPath().toString(), violation.getMessage()))
                .sorted(Comparator.comparing(FieldDetail::field))
                .forEach(invalid::add);
        if (!invalid.isEmpty()) {
            throw new InvalidRequestException(invalid);
        }
        Registration result = profiles.create(AuthFilter.currentCaller(http), key,
                new ProfessionalProfileCommand(body.accountId(), body.licenseNumber(), body.yearsExperience()));
        ProfessionalResponse response = mapper.toResponse(result.professional());
        if (result.created()) {
            return ResponseEntity.created(URI.create(ProfessionalRegistrationController.PUBLIC_PATH + response.id()))
                    .body(response);
        }
        return ResponseEntity.ok(response);
    }

    private CreateProfileRequest read(JsonNode tree, List<FieldDetail> invalid) {
        if (tree == null || !tree.isObject()) {
            invalid.add(new FieldDetail("body", "must be a JSON object"));
            throw new InvalidRequestException(invalid);
        }
        tree.fieldNames().forEachRemaining(name -> {
            if (!FIELDS.contains(name)) {
                invalid.add(new FieldDetail(name, "is not allowed"));
            }
        });
        try {
            return json.treeToValue(tree, CreateProfileRequest.class);
        } catch (JsonProcessingException | IllegalArgumentException malformed) {
            invalid.add(new FieldDetail("body", "accountId must be a UUID and yearsExperience an integer"));
            throw new InvalidRequestException(invalid);
        }
    }
}
