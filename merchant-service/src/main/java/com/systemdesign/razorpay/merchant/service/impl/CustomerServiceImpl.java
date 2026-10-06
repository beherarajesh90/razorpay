package com.systemdesign.razorpay.merchant.service.impl;

import com.systemdesign.razorpay.common.exception.ResourceNotFoundException;
import com.systemdesign.razorpay.merchant.entity.Customer;
import com.systemdesign.razorpay.merchant.entity.Merchant;
import com.systemdesign.razorpay.merchant.repository.CustomerRepository;
import com.systemdesign.razorpay.merchant.repository.MerchantRepository;
import com.systemdesign.razorpay.merchant.service.CustomerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;
    private final MerchantRepository merchantRepository;

    @Override
    public UUID findOrCreate(UUID merchantId, String email, String name, String phone) {
        if(email==null || email.isEmpty()){
            return null;
        }

        return customerRepository.findByMerchant_IdAndEmail(merchantId, email)
                .map(Customer::getId)
                .orElseGet(() -> createNewCustomer(merchantId, email, name, phone));
    }

    private UUID createNewCustomer(UUID merchantId, String email, String name, String phone) {
        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new ResourceNotFoundException("Merchant", merchantId));

        Customer customer = Customer.builder()
                .merchant(merchant)
                .name(name)
                .email(email)
                .phone(phone)
                .build();

        customer = customerRepository.save(customer);
        log.info("Customer created via findOrCreate id={} merchantId={} email={}",
                customer.getId(), merchantId, email);
        return customer.getId();
    }

}
