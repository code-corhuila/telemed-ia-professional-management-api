package co.edu.corhuila.telemedia.professionalmanagement.adapter.out.persistence;

import co.edu.corhuila.telemedia.professionalmanagement.domain.model.Professional;
import co.edu.corhuila.telemedia.professionalmanagement.domain.model.ProfessionalStatus;
import co.edu.corhuila.telemedia.professionalmanagement.domain.model.ProfessionalType;
import org.springframework.stereotype.Component;

@Component
public class ProfessionalPersistenceMapper {

    public Professional toDomain(ProfessionalEntity entity) {
        return new Professional(
                entity.getId(),
                entity.getIdentityUserId(),
                entity.getLicenseNumber(),
                entity.getYearsExperience(),
                ProfessionalType.valueOf(entity.getProfessionalType()),
                ProfessionalStatus.valueOf(entity.getStatus())
        );
    }

    public ProfessionalEntity toEntity(Professional professional) {
        return new ProfessionalEntity(
                professional.getId(),
                professional.getIdentityUserId(),
                professional.getLicenseNumber(),
                professional.getYearsExperience(),
                professional.getProfessionalType().name(),
                professional.getStatus().name()
        );
    }
}
