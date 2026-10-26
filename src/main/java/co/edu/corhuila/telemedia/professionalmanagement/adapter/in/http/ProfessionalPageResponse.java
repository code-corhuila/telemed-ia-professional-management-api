package co.edu.corhuila.telemedia.professionalmanagement.adapter.in.http;

import java.util.List;

/** {@code PaginatedProfessionalResponse}: {@code {data, meta}}. */
public record ProfessionalPageResponse(List<ProfessionalResponse> data, Meta meta) {

    public record Meta(int page, int limit, long total, long totalPages) {
    }
}
