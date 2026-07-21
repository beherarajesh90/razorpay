package com.systemdesign.razorpay.merchant.mapper;

import com.systemdesign.razorpay.merchant.dto.request.MerchantSignupRequest;
import com.systemdesign.razorpay.merchant.dto.response.MerchantResponse;
import com.systemdesign.razorpay.merchant.entity.Merchant;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface MerchantMapper {

    Merchant toEntityFromSignUpRequest(MerchantSignupRequest request);

    MerchantResponse toResponse(Merchant merchant);
}
