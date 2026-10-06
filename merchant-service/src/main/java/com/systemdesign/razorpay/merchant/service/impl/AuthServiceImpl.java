package com.systemdesign.razorpay.merchant.service.impl;

import com.systemdesign.razorpay.common.enums.MerchantStatus;
import com.systemdesign.razorpay.common.enums.UserRole;
import com.systemdesign.razorpay.common.exception.DuplicateResourceException;
import com.systemdesign.razorpay.merchant.dto.request.LoginRequest;
import com.systemdesign.razorpay.merchant.dto.request.MerchantSignupRequest;
import com.systemdesign.razorpay.merchant.dto.response.LoginResponse;
import com.systemdesign.razorpay.merchant.dto.response.MerchantResponse;
import com.systemdesign.razorpay.merchant.entity.AppUser;
import com.systemdesign.razorpay.merchant.entity.Merchant;
import com.systemdesign.razorpay.merchant.mapper.MerchantMapper;
import com.systemdesign.razorpay.merchant.repository.AppUserRepository;
import com.systemdesign.razorpay.merchant.repository.MerchantRepository;
import com.systemdesign.razorpay.merchant.security.JwtUtil;
import com.systemdesign.razorpay.merchant.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final MerchantRepository merchantRepository;
    private final AppUserRepository appUserRepository;
    private final MerchantMapper merchantMapper;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public MerchantResponse signup(MerchantSignupRequest request) {
        if (merchantRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("DUPLICATE_MERCHANT_EMAIL",
                    "Merchant with email already exists: " + request.email());
        }

        Merchant merchant = merchantMapper.toEntityFromSignUpRequest(request);
        merchant.setStatus(MerchantStatus.PENDING_KYC);
        merchant = merchantRepository.save(merchant);

        AppUser appUser = AppUser.builder()
                .email(request.email())
                .merchant(merchant)
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(UserRole.OWNER)
                .build();
        appUserRepository.save(appUser);

        return merchantMapper.toResponse(merchant);
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.email(), request.password()));
        AppUser appUser = (AppUser) authentication.getPrincipal();
        String accessToken = jwtUtil.generateAccessToken(request.email(), appUser.getMerchant().getId(),appUser.getRole());
        return new LoginResponse(accessToken);
    }
}
