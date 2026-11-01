package com.ebookstore.repository;

import com.ebookstore.entity.Brand;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface BrandRepository extends JpaRepository<Brand, Long> {
    @Query("select distinct p.brand from Product p where p.category.id = :categoryId and p.brand is not null")
    List<Brand> findByCategory(@Param("categoryId") Long categoryId);
}
