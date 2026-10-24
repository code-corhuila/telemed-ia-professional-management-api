package com.telemed.professionalmanagement.interfaces.rest.dto;

import java.util.List;

/** {@code PaginatedProfessionalResponse}: {@code {data, meta}}. */
public record ProfessionalPageResponse(List<ProfessionalResponse> data, Meta meta) {

    public record Meta(int page, int limit, long total, long totalPages) {
    }
}
