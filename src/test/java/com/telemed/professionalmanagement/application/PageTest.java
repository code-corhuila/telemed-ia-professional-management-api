package com.telemed.professionalmanagement.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.telemed.professionalmanagement.application.port.in.Page;
import com.telemed.professionalmanagement.application.port.in.PageRequest;

class PageTest {

    @Test
    void aPageStartsAtOneAndHoldsUpToOneHundredItems() {
        assertThat(new PageRequest(1, 1).offset()).isZero();
        assertThat(new PageRequest(3, 20).offset()).isEqualTo(40);
        assertThat(new PageRequest(1, PageRequest.MAX_LIMIT).limit()).isEqualTo(100);
        assertThat(PageRequest.DEFAULT_LIMIT).isEqualTo(20);

        assertThatThrownBy(() -> new PageRequest(0, 20)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PageRequest(1, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PageRequest(1, 101)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void totalPagesRoundsUpAndIsZeroWithoutResults() {
        PageRequest request = new PageRequest(1, 20);

        assertThat(Page.of(List.of(), request, 0).totalPages()).isZero();
        assertThat(Page.of(List.of("a"), request, 1).totalPages()).isEqualTo(1);
        assertThat(Page.of(List.of("a"), request, 20).totalPages()).isEqualTo(1);
        assertThat(Page.of(List.of("a"), request, 21).totalPages()).isEqualTo(2);
    }

    @Test
    void aPageNeverHoldsMoreItemsThanItsLimitNorANegativeTotal() {
        assertThatThrownBy(() -> Page.of(List.of("a", "b"), new PageRequest(1, 1), 2))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Page.of(List.of(), new PageRequest(1, 1), -1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
