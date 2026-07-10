package com.innowise.paymentservice.application.mapper;

import com.innowise.paymentservice.application.dto.PaymentFilterRequest;
import com.innowise.paymentservice.domain.model.PaymentFilter;
import com.innowise.paymentservice.domain.model.PaymentStatus;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.Named;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface PaymentFilterMapper {

    @Named("stringToPaymentStatus")
    static PaymentStatus stringToPaymentStatus(String status) {
        return status != null ? PaymentStatus.valueOf(status.toUpperCase()) : null;
    }

    @Named("stringsToPaymentStatuses")
    static List<PaymentStatus> stringsToPaymentStatuses(List<String> statuses) {
        return statuses != null 
            ? statuses.stream()
                .map(PaymentFilterMapper::stringToPaymentStatus)
                .toList()
            : null;
    }

    @Mapping(source = "statuses", target = "statuses", qualifiedByName = "stringsToPaymentStatuses")
    PaymentFilter toDomainFilter(PaymentFilterRequest request);
}
