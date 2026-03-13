package com.ssginc.showpingrefactoring.domain.product.service.implement;

import com.ssginc.showpingrefactoring.domain.product.dto.object.ProductItemDto;
import com.ssginc.showpingrefactoring.domain.product.dto.object.ProductDto;
import com.ssginc.showpingrefactoring.domain.product.dto.object.ProductItemProjection;
import com.ssginc.showpingrefactoring.domain.product.dto.object.SearchProductItemDto;
import com.ssginc.showpingrefactoring.domain.product.dto.response.GetProductListResponseDto;
import com.ssginc.showpingrefactoring.domain.product.dto.response.GetSearchProductListResponseDto;
import com.ssginc.showpingrefactoring.domain.product.entity.Product;
import com.ssginc.showpingrefactoring.domain.product.repository.ProductRepository;
import com.ssginc.showpingrefactoring.domain.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;

    public Page<ProductDto> getProductsByCategory(Long categoryNo, Pageable pageable) {
        // 상품 목록과 해당 상품의 평균 리뷰 평점 및 리뷰 개수를 가져오는 쿼리 호출
        Page<Object[]> productPage = productRepository.findByCategoryCategoryNo(categoryNo, pageable);

        List<ProductDto> productDtoList = productPage.getContent().stream()
                .map(productData -> {
                    Product product = (Product) productData[0]; // 첫 번째 요소는 Product 객체
                    Double reviewAverage = (Double) productData[1]; // 두 번째 요소는 평균 리뷰 평점
                    Long reviewCount = (Long) productData[2]; // 세 번째 요소는 리뷰 개수

                    return new ProductDto(
                            product.getProductNo(),
                            product.getProductName(),
                            product.getProductPrice(),
                            product.getProductQuantity(),
                            product.getProductImg(),
                            product.getProductDescript(),
                            product.getProductSale(),
                            product.getProductPrice() - (product.getProductPrice() * product.getProductSale() / 100),
                            reviewCount,
                            reviewAverage != null ? reviewAverage : 0.0,
                            product.getProductSaleQuantity()
                    );
                })
                .collect(Collectors.toList());

        return new PageImpl<>(productDtoList, pageable, productPage.getTotalElements());
    }

    // 상품 상세 조회 (ID로)
    @Override
    public ProductDto getProductById(Long productId) {
        // 상품 ID로 해당 상품을 조회
        Optional<Product> productOpt = productRepository.findById(productId);
        if (productOpt.isPresent()) {
            Product product = productOpt.get();
            // 평균 별점 계산
            Double reviewAverage = product.getReviews().stream()
                    .mapToDouble(review -> review.getReviewRating())
                    .average()
                    .orElse(0.0);  // 리뷰가 없으면 0.0으로 설정

            // 리뷰 개수 계산
            Long reviewCount = (long) product.getReviews().size();

            return new ProductDto(
                    product.getProductNo(),
                    product.getProductName(),
                    product.getProductPrice(),
                    product.getProductQuantity(),
                    product.getProductImg(),
                    product.getProductDescript(),
                    product.getProductSale(),
                    product.getProductPrice() - (product.getProductPrice() * product.getProductSale() / 100),
                    reviewCount,
                    reviewAverage,
                    product.getProductSaleQuantity()
            );
        } else {
            throw new RuntimeException("상품을 찾을 수 없습니다: " + productId);
        }
    }

//    public List<ProductItemDto> getProducts() {
//        try {
//            List<Product> products = productRepository.findAll();
//
//            if (products.isEmpty()) {
//                throw new RuntimeException("상품이 없습니다.");
//            }
//
//            return products.stream().map(product -> {
//                Long productPrice = product.getProductPrice();
//
//                return ProductItemDto.builder()
//                        .productNo(product.getProductNo())
//                        .productName(product.getProductName())
//                        .productPrice(productPrice)
//                        .productImg(product.getProductImg())
//                        .build();
//            }).toList();
//        } catch (RuntimeException e) {
//            log.error("Exception [Err_Msg]: {}", e.getMessage());
//            log.error("Exception [Err_Where]: {}", e.getStackTrace()[0]);
//
//            return null;
//        }
//    }

//    public Page<ProductItemDto> getProducts(int page, int size) {
//        Pageable pageable = PageRequest.of(page, size, Sort.by("productNo").ascending());
//
//        return productRepository.findAll(pageable)
//                .map(ProductItemDto::fromEntity);
//    }

    @Override
    public GetProductListResponseDto getProducts(Long lastProductNo, int size) {
        Pageable limit = PageRequest.of(0, size);

        List<Product> entities;
        if (lastProductNo == null) {
            entities = productRepository.findInitial(limit);
        } else {
            entities = productRepository.findNext(lastProductNo, limit);
        }

        List<ProductItemDto> data = entities.stream()
                .map(ProductItemDto::fromEntity)
                .toList();

        // 다음 마지막 ProductNo
        Long nextLastProductNo = data.isEmpty() ? null : data.get(data.size() - 1).getProductNo();

        // hasNext 여부 조회 건수가 설정한 size보다 작다면 다음 페이지가 없음
        boolean hasNext = data.size() == size;

        return new GetProductListResponseDto(data, hasNext, nextLastProductNo);
    }

    @Override
    public GetSearchProductListResponseDto getSearchProducts(String keyword, Long lastProductNo, int size) {
        List<ProductItemProjection> result;
        if (lastProductNo == null) {
            result = productRepository.searchInitialLike(keyword, size);
        } else {
            result = productRepository.searchNextLike(keyword, lastProductNo, size);
        }

        List<SearchProductItemDto> data = result.stream()
                .map(p -> SearchProductItemDto.builder()
                        .productNo(p.getProductNo())
                        .productName(p.getProductName())
                        .productPrice(p.getProductPrice())
                        .productImg(p.getProductImg())
                        .score(null)
                        .build())
                .toList();

        Long nextLastProductNo = data.isEmpty() ? null : data.get(data.size() - 1).getProductNo();

        boolean hasNext = data.size() == size;

        return new GetSearchProductListResponseDto(data, hasNext, nextLastProductNo, null);
    }

    @Override
    public GetSearchProductListResponseDto getSearchProducts(String keyword, Long lastProductNo, Double lastScore, int size) {
        List<ProductItemProjection> result;
        if (lastProductNo == null || lastScore == null) {
            result = productRepository.searchInitialFt(keyword, size);
        } else {
            result = productRepository.searchNextFt(keyword, lastProductNo, lastScore, size);
        }

        List<SearchProductItemDto> data = result.stream()
                .map(p -> SearchProductItemDto.builder()
                        .productNo(p.getProductNo())
                        .productName(p.getProductName())
                        .productPrice(p.getProductPrice())
                        .productImg(p.getProductImg())
                        .score(p.getScore())
                        .build())
                .toList();

        Long nextLastProductNo = data.isEmpty() ? null : data.get(data.size() - 1).getProductNo();
        Double nextLastScore = data.isEmpty() ? null : data.get(data.size() - 1).getScore();

        boolean hasNext = data.size() == size;

        return new GetSearchProductListResponseDto(data, hasNext, nextLastProductNo, nextLastScore);
    }

    public List<ProductDto> getTopProductsBySaleQuantity(Long categoryNo) {
        List<Object[]> productDataList = productRepository.findTopProductsBySaleQuantity(categoryNo, PageRequest.of(0, 4));

        return productDataList.stream().map(productData -> {
            Product product = (Product) productData[0];
            Double reviewAverage = (Double) productData[1];
            Long reviewCount = (Long) productData[2];

            Long discountedPrice = product.getProductPrice() -
                    (product.getProductPrice() * product.getProductSale() / 100);

            return new ProductDto(
                    product.getProductNo(),
                    product.getProductName(),
                    product.getProductPrice(),
                    product.getProductQuantity(),
                    product.getProductImg(),
                    product.getProductDescript(),
                    product.getProductSale(),
                    discountedPrice,
                    reviewCount,
                    reviewAverage != null ? reviewAverage : 0.0,
                    product.getProductSaleQuantity()
            );
        }).collect(Collectors.toList());
    }

    public List<ProductDto> getTopProductsBySale(Long categoryNo) {
        List<Object[]> productDataList = productRepository.findTopProductsBySale(categoryNo, PageRequest.of(0, 4));

        return productDataList.stream().map(productData -> {
            Product product = (Product) productData[0];
            Double reviewAverage = (Double) productData[1];
            Long reviewCount = (Long) productData[2];

            Long discountedPrice = product.getProductPrice() -
                    (product.getProductPrice() * product.getProductSale() / 100);

            return new ProductDto(
                    product.getProductNo(),
                    product.getProductName(),
                    product.getProductPrice(),
                    product.getProductQuantity(),
                    product.getProductImg(),
                    product.getProductDescript(),
                    product.getProductSale(),
                    discountedPrice,
                    reviewCount,
                    reviewAverage != null ? reviewAverage : 0.0,
                    product.getProductSaleQuantity()
            );
        }).collect(Collectors.toList());
    }
}
