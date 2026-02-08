package com.malva_pastry_shop.backend.dto.response.api;

import java.util.List;

public record StorefrontSectionApiDTO(
        Long id,
        String name,
        String slug,
        String description,
        List<ProductApiDTO.Simple> products) {

    public record Simple(
            Long id,
            String name,
            String slug,
            String description) {
    }
}
