package com.systemdesign.razorpay.vault.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.LocalDateTime;
import java.time.Year;


public class ExpiryYearValidator implements ConstraintValidator<ExpiryYear, Integer> {
    @Override
    public boolean isValid(Integer inputYear, ConstraintValidatorContext constraintValidatorContext) {
        if(inputYear == null)   return false;

        int currentYear = Year.now().getValue();
        return inputYear >= currentYear;
    }
}
