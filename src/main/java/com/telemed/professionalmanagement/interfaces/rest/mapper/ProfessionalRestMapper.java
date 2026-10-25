package com.telemed.professionalmanagement.interfaces.rest.mapper;

import com.telemed.professionalmanagement.application.port.in.Page;
import com.telemed.professionalmanagement.application.port.in.ProfessionalView;
import com.telemed.professionalmanagement.application.port.out.AccountName;
import com.telemed.professionalmanagement.domain.Professional;
import com.telemed.professionalmanagement.interfaces.rest.dto.ProfessionalPageResponse;
import com.telemed.professionalmanagement.interfaces.rest.dto.ProfessionalResponse;
import org.springframework.stereotype.Component;

@Component
public class ProfessionalRestMapper {

    public ProfessionalResponse toResponse(Professional professional) {
        return toResponse(new ProfessionalView(professional, null));
    }

    public ProfessionalResponse toResponse(ProfessionalView view) {
        Professional professional = view.professional();
        AccountName account = view.account();
        return new ProfessionalResponse(
                professional.getId(),
                professional.getIdentityUserId(),
                account == null ? null : account.fullName(),
                account == null ? null : account.email(),
                professional.getLicenseNumber(),
                professional.getYearsExperience(),
                professional.getProfessionalType().name(),
                professional.getStatus().name()
        );
    }

    public ProfessionalPageResponse toPageResponse(Page<ProfessionalView> page) {
        return new ProfessionalPageResponse(
                page.items().stream().map(this::toResponse).toList(),
                new ProfessionalPageResponse.Meta(page.page(), page.limit(), page.total(), page.totalPages()));
    }
}
