package com.innowise.paymentservice.application.mapper;

import com.innowise.paymentservice.application.dto.CreatePaymentRequestDto;
import com.innowise.paymentservice.application.dto.PaymentResponseDto;
import com.innowise.paymentservice.domain.model.Payment;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface PaymentMapper {

    Payment toEntity(CreatePaymentRequestDto dto);

    PaymentResponseDto toDto(Payment entity);
}
