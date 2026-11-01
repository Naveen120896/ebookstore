package com.ebookstore.service;

import com.ebookstore.dto.Dtos.*;
import com.ebookstore.dto.Mapper;
import com.ebookstore.entity.Brand;
import com.ebookstore.entity.Product;
import com.ebookstore.exception.ApiException;
import com.ebookstore.repository.BrandRepository;
import com.ebookstore.repository.CategoryRepository;
import com.ebookstore.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CatalogueService {
    private final ProductRepository productRepo;
    private final CategoryRepository categoryRepo;
    private final BrandRepository brandRepo;

    public List<CategoryDto> categories() {
        return categoryRepo.findAll().stream().map(Mapper::category).toList();
    }

    public List<BrandDto> brands(Long categoryId) {
        List<Brand> list = categoryId == null ? brandRepo.findAll() : brandRepo.findByCategory(categoryId);
        return list.stream().map(Mapper::brand).toList();
    }

    public ProductPageDto search(Long categoryId, Long brandId, String q, int page, int size) {
        Specification<Product> spec = (root, query, cb) -> cb.conjunction();
        if (categoryId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("category").get("id"), categoryId));
        }
        if (brandId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("brand").get("id"), brandId));
        }
        if (q != null && !q.isBlank()) {
            String pattern = "%" + q.trim().toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("title")), pattern),
                    cb.like(cb.lower(root.get("author")), pattern)));
        }
        Page<Product> result = productRepo.findAll(spec, PageRequest.of(page, size, Sort.by("title")));
        return new ProductPageDto(result.getContent().stream().map(Mapper::product).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements());
    }

    public ProductDto get(Long id) {
        return Mapper.product(find(id));
    }

    public List<ProductDto> related(Long id) {
        Product p = find(id);
        List<Product> related = new ArrayList<>(p.getRelated());
        if (related.isEmpty()) { // fallback: other books in the same category
            related = productRepo.findTop5ByCategoryIdAndIdNot(p.getCategory().getId(), id);
        }
        return related.stream().map(Mapper::product).toList();
    }

    public List<ProductDto> recommendations(Long userId) {
        List<Product> list = productRepo.recommend(userId, PageRequest.of(0, 10));
        if (list.isEmpty()) { // new user with no history: show popular/first books
            list = productRepo.findAll(PageRequest.of(0, 10, Sort.by("title"))).getContent();
        }
        return list.stream().map(Mapper::product).toList();
    }

    private Product find(Long id) {
        return productRepo.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Product not found"));
    }
}
