package com.ebookstore.controller;

import com.ebookstore.dto.Dtos.*;
import com.ebookstore.service.CatalogueService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class CatalogueController {
    private final CatalogueService catalogue;

    @GetMapping("/categories")
    public List<CategoryDto> categories() {
        return catalogue.categories();
    }

    @GetMapping("/brands")
    public List<BrandDto> brands(@RequestParam(required = false) Long categoryId) {
        return catalogue.brands(categoryId);
    }

    @GetMapping("/products")
    public ProductPageDto products(@RequestParam(required = false) Long categoryId,
                                   @RequestParam(required = false) Long brandId,
                                   @RequestParam(required = false) String q,
                                   @RequestParam(defaultValue = "0") int page,
                                   @RequestParam(defaultValue = "20") int size) {
        return catalogue.search(categoryId, brandId, q, page, size);
    }

    @GetMapping("/products/recommendations")
    public List<ProductDto> recommendations(@AuthenticationPrincipal Long userId) {
        return catalogue.recommendations(userId);
    }

    @GetMapping("/products/{id}")
    public ProductDto product(@PathVariable Long id) {
        return catalogue.get(id);
    }

    @GetMapping("/products/{id}/related")
    public List<ProductDto> related(@PathVariable Long id) {
        return catalogue.related(id);
    }
}
