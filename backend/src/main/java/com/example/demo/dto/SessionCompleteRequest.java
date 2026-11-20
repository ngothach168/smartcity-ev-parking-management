package com.example.demo.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class SessionCompleteRequest {

    @DecimalMin(value = "0.01", message = "Lượng điện tiêu thụ (kWh) phải lớn hơn 0")
    @Digits(integer = 6, fraction = 2, message = "Lượng điện tiêu thụ tối đa 6 chữ số nguyên và 2 chữ số thập phân")
    private BigDecimal kwhConsumed;
}