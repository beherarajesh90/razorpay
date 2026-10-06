package com.systemdesign.razorpay.audit;

import com.systemdesign.razorpay.common.context.MerchantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.AuditorAware;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Optional;

@Component("auditorAwareImpl")
@RequiredArgsConstructor
public class AuditorAwareImpl implements AuditorAware {

    private final MerchantContext merchantContext;

    @Override
    public Optional getCurrentAuditor() {

        try{
            String keyId = merchantContext.getKeyId();
            if (keyId!=null && !keyId.isBlank()){
                return Optional.of(keyId);
            }

            if(merchantContext.getMerchantId()!=null){
                return Optional.of("merchant_id: "+merchantContext);
            }
        } catch (Exception e){
            //ignore the exception in case of system since we are not using merchant context
        }

        return Optional.of("SYSTEM");
    }
}
