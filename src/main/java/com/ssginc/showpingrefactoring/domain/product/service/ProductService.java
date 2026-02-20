package com.ssginc.showpingrefactoring.domain.product.service;

import com.ssginc.showpingrefactoring.domain.product.dto.response.GetProductListResponseDto;
import com.ssginc.showpingrefactoring.domain.product.dto.response.GetSearchProductListResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.ssginc.showpingrefactoring.domain.product.dto.object.ProductItemDto;
import com.ssginc.showpingrefactoring.domain.product.dto.object.ProductDto;

import java.util.List;

public interface ProductService {

    public Page<ProductDto> getProductsByCategory(Long categoryNo, Pageable pageable);

    public ProductDto getProductById(Long productId);

//    public List<ProductItemDto> getProducts();

//    public Page<ProductItemDto> getProducts(int page, int size);

    public GetProductListResponseDto getProducts(Long lastProductNo, int size);

//    public GetProductListResponseDto getSearchProducts(String keyword, Long lastProductNo, int size);

    public GetSearchProductListResponseDto getSearchProducts(String keyword, Long lastProductNo, Double lastScore, int size);

    public List<ProductDto> getTopProductsBySaleQuantity(Long categoryNo);

    public List<ProductDto> getTopProductsBySale(Long categoryNo);

}
