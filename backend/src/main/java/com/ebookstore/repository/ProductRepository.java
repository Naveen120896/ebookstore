package com.ebookstore.repository;

import com.ebookstore.entity.Product;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    List<Product> findTop5ByCategoryIdAndIdNot(Long categoryId, Long id);

    /** Books in categories the user bought from before, excluding books already bought. */
    @Query("select distinct p from Product p " +
           "where p.category.id in (select oi.product.category.id from OrderItem oi where oi.order.user.id = :userId) " +
           "and p.id not in (select oi2.product.id from OrderItem oi2 where oi2.order.user.id = :userId)")
    List<Product> recommend(@Param("userId") Long userId, Pageable pageable);
}
