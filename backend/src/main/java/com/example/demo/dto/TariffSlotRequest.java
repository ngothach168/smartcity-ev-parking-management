package com.example.demo.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalTime;

@Getter
@Setter
public class TariffSlotRequest {

    @NotNull(message = "Giờ bắt đầu không được để trống")
    private LocalTime startTime;

    @NotNull(message = "Giờ kết thúc không được để trống")
    private LocalTime endTime;

    @NotNull(message = "Giá cơ bản không được để trống")
    @DecimalMin(value = "0.00", message = "Giá cơ bản phải lớn hơn hoặc bằng 0")
    @Digits(integer = 8, fraction = 2, message = "Giá cơ bản tối đa 8 chữ số nguyên và 2 chữ số thập phân")
    private BigDecimal basePrice;

    @DecimalMin(value = "0.01", message = "Hệ số giá phải lớn hơn 0")
    @Digits(integer = 1, fraction = 2, message = "Hệ số giá tối đa 9.99")
    private BigDecimal multiplier;
}