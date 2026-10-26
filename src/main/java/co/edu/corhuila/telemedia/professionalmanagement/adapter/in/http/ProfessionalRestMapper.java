package co.edu.corhuila.telemedia.professionalmanagement.adapter.in.http;

import co.edu.corhuila.telemedia.professionalmanagement.application.port.in.Page;
import co.edu.corhuila.telemedia.professionalmanagement.application.port.in.ProfessionalView;
import co.edu.corhuila.telemedia.professionalmanagement.application.port.out.AccountName;
import co.edu.corhuila.telemedia.professionalmanagement.domain.model.Professional;
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
