package com.systemdesign.razorpay.merchant.service;

import com.systemdesign.razorpay.merchant.dto.request.LoginRequest;
import com.systemdesign.razorpay.merchant.dto.request.MerchantSignupRequest;
import com.systemdesign.razorpay.merchant.dto.response.LoginResponse;
import com.systemdesign.razorpay.merchant.dto.response.MerchantResponse;
import jakarta.validation.Valid;
import org.jspecify.annotations.Nullable;

public interface AuthService {
    MerchantResponse signup(@Valid MerchantSignupRequest request);

    LoginResponse login(LoginRequest request);
}
