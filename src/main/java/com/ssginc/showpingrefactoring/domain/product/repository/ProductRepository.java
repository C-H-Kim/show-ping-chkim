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

    // LIKE 방식
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
    List<ProductItemProjection> searchInitialLike(@Param("keyword") String keyword, @Param("size") int size);

    // FULLTEXT 방식 - boolean mode
    @Query(value = """
        SELECT
            product_no AS productNo,
            product_name AS productName,
            product_price AS productPrice,
            product_img AS productImg,
            MATCH(product_name) AGAINST (:keyword IN BOOLEAN MODE) AS score
        FROM product
        WHERE MATCH(product_name) AGAINST (:keyword IN BOOLEAN MODE)
        ORDER BY
            score DESC,
            product_no ASC
        LIMIT :size
    """, nativeQuery = true)
    List<ProductItemProjection> searchInitialFt(@Param("keyword") String keyword, @Param("size") int size);

//    // FULLTEXT 방식 - natural language mode
//    @Query(value = """
//        SELECT
//            product_no AS productNo,
//            product_name AS productName,
//            product_price AS productPrice,
//            product_img AS productImg,
//            MATCH(product_name) AGAINST (:keyword IN NATURAL LANGUAGE MODE) AS score
//        FROM product
//        WHERE MATCH(product_name) AGAINST (:keyword IN NATURAL LANGUAGE MODE)
//        ORDER BY
//            score DESC,
//            product_no ASC
//        LIMIT :size
//    """, nativeQuery = true)
//    List<ProductItemProjection> searchInitialFt(@Param("keyword") String keyword, @Param("size") int size);

    // LIKE 방식
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
    List<ProductItemProjection> searchNextLike(@Param("keyword") String keyword, @Param("lastProductNo") Long lastProductNo, @Param("size") int size);

    // FULLTEXT 방식 - boolean mode
    @Query(value = """
        SELECT *
        FROM (
            SELECT
                p.product_no AS productNo,
                p.product_name AS productName,
                p.product_price AS productPrice,
                p.product_img AS productImg,
                MATCH(product_name) AGAINST (:keyword IN BOOLEAN MODE) AS score
            FROM product p
            WHERE MATCH(product_name) AGAINST (:keyword IN BOOLEAN MODE)
        ) t
        WHERE (t.score < :lastScore)
            OR (t.score = :lastScore AND t.productNo > :lastProductNo)
        ORDER BY
            t.score DESC,
            t.productNo ASC
        LIMIT :size
    """, nativeQuery = true)
    List<ProductItemProjection> searchNextFt(@Param("keyword") String keyword, @Param("lastProductNo") Long lastProductNo, @Param("lastScore") Double lastScore, @Param("size") int size);

//    // FULLTEXT 방식 - natural mdoe
//    @Query(value = """
//        SELECT *
//        FROM (
//            SELECT
//                p.product_no AS productNo,
//                p.product_name AS productName,
//                p.product_price AS productPrice,
//                p.product_img AS productImg,
//                MATCH(product_name) AGAINST (:keyword IN NATURAL LANGUAGE MODE) AS score
//            FROM product p
//            WHERE MATCH(product_name) AGAINST (:keyword IN NATURAL LANGUAGE MODE)
//        ) t
//        WHERE (t.score < :lastScore)
//            OR (t.score = :lastScore AND t.productNo > :lastProductNo)
//        ORDER BY
//            t.score DESC,
//            t.productNo ASC
//        LIMIT :size
//    """, nativeQuery = true)
//    List<ProductItemProjection> searchNextFt(@Param("keyword") String keyword, @Param("lastProductNo") Long lastProductNo, @Param("lastScore") Double lastScore, @Param("size") int size);

}