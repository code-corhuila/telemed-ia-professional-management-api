package co.edu.corhuila.telemedia.professionalmanagement.adapter.in.http;

import java.util.UUID;

import co.edu.corhuila.telemedia.professionalmanagement.adapter.in.http.ApiError.FieldDetail;
import co.edu.corhuila.telemedia.professionalmanagement.application.port.in.GetProfessionalUseCase;
import co.edu.corhuila.telemedia.professionalmanagement.application.port.in.ListProfessionalsUseCase;
import co.edu.corhuila.telemedia.professionalmanagement.application.port.in.PageRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api")
public class ProfessionalController {

    private final ListProfessionalsUseCase listProfessionalsUseCase;
    private final GetProfessionalUseCase getProfessionalUseCase;
    private final ProfessionalRestMapper professionalRestMapper;

    public ProfessionalController(ListProfessionalsUseCase listProfessionalsUseCase,
                                 GetProfessionalUseCase getProfessionalUseCase,
                                 ProfessionalRestMapper professionalRestMapper) {
        this.listProfessionalsUseCase = listProfessionalsUseCase;
        this.getProfessionalUseCase = getProfessionalUseCase;
        this.professionalRestMapper = professionalRestMapper;
    }

    @GetMapping("/professionals")
    public ResponseEntity<ProfessionalPageResponse> listProfessionals(
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "limit", defaultValue = "" + PageRequest.DEFAULT_LIMIT) int limit,
            HttpServletRequest http) {
        List<FieldDetail> invalid = new ArrayList<>();
        if (page < 1) {
            invalid.add(new FieldDetail("page", "must be 1 or greater"));
        }
        if (limit < 1 || limit > PageRequest.MAX_LIMIT) {
            invalid.add(new FieldDetail("limit", "must be between 1 and " + PageRequest.MAX_LIMIT));
        }
        if (!invalid.isEmpty()) {
            throw new InvalidRequestException(invalid);
        }
        return ResponseEntity.ok(professionalRestMapper.toPageResponse(
                listProfessionalsUseCase.list(AuthFilter.currentCaller(http), new PageRequest(page, limit))));
    }

    @GetMapping("/professionals/{professionalId}")
    public ResponseEntity<ProfessionalResponse> getProfessional(@PathVariable UUID professionalId,
                                                                HttpServletRequest http) {
        return ResponseEntity.ok(professionalRestMapper.toResponse(
                getProfessionalUseCase.getById(AuthFilter.currentCaller(http), professionalId)));
    }
}
