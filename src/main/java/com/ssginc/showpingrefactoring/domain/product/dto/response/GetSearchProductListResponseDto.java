package com.ssginc.showpingrefactoring.domain.product.dto.response;

import com.ssginc.showpingrefactoring.domain.product.dto.object.SearchProductItemDto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class GetSearchProductListResponseDto {

    private List<SearchProductItemDto> data;

    private boolean hasNext;

    private Long nextLastProductNo;

    private Double nextLastScore;

}
