package com.ssginc.showpingrefactoring.domain.product.repository;

import com.ssginc.showpingrefactoring.domain.product.dto.object.ProductItemProjection;
import com.ssginc.showpingrefactoring.domain.product.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    // 카테고리 번호에 해당하는 상품과 해당 상품의 평균 리뷰 평점 및 리뷰 개수를 가져오는 쿼리
    @Query("SELECT p, AVG(r.reviewRating) AS reviewAverage, COUNT(r) AS reviewCount " +
            "FROM Product p " +
            "LEFT JOIN p.reviews r " +
            "WHERE (:categoryNo = 0 OR p.category.categoryNo = :categoryNo) " +
            "GROUP BY p.productNo")
    Page<Object[]> findByCategoryCategoryNo(@Param("categoryNo") Long categoryNo, Pageable pageable);

    @Query("SELECT p, AVG(r.reviewRating) AS reviewAverage, COUNT(r) AS reviewCount " +
            "FROM Product p " +
            "LEFT JOIN p.reviews r " +
            "WHERE (:categoryNo = 0 OR p.category.categoryNo = :categoryNo) " +
            "GROUP BY p.productNo " +
            "ORDER BY p.productSaleQuantity DESC")
    List<Object[]> findTopProductsBySaleQuantity(@Param("categoryNo") Long categoryNo, Pageable pageable);

    @Query("SELECT p, AVG(r.reviewRating) AS reviewAverage, COUNT(r) AS reviewCount " +
            "FROM Product p " +
            "LEFT JOIN p.reviews r " +
            "WHERE (:categoryNo = 0 OR p.category.categoryNo = :categoryNo) " +
            "AND p.productSale <> 0 " +
            "GROUP BY p.productNo " +
            "ORDER BY p.productSale DESC")
    List<Object[]> findTopProductsBySale(@Param("categoryNo") Long categoryNo, Pageable pageable);

    @Query("SELECT p " +
            "FROM Product p " +
            "ORDER BY p.productNo ASC")
    List<Product> findInitial(Pageable pageable);

    @Query("SELECT p " +
            "FROM Product p " +
            "WHERE p.productNo > :lastProductNo " +
            "ORDER BY p.productNo ASC")
    List<Product> findNext(@Param("lastProductNo") Long lastProductNo, Pageable pageable);

    @Query(value = """
        SELECT
            product_no AS productNo,
            product_name AS productName,
            product_price AS productPrice,
            product_img AS productImg
        FROM product
        WHERE product_name LIKE CONCAT('%', :keyword, '%')
        ORDER BY product_no ASC
        LIMIT :size
    """, nativeQuery = true)
    List<ProductItemProjection> searchInitial(@Param("keyword") String keyword, @Param("size") int size);

    @Query(value = """
        SELECT
            product_no AS productNo,
            product_name AS productName,
            product_price AS productPrice,
            product_img AS productImg
        FROM product
        WHERE product_no > :lastProductNo
            AND product_name LIKE CONCAT('%', :keyword, '%')
        ORDER BY product_no ASC
        LIMIT :size
    """, nativeQuery = true)
    List<ProductItemProjection> searchNext(@Param("keyword") String keyword, @Param("lastProductNo") Long lastProductNo, @Param("size") int size);

}