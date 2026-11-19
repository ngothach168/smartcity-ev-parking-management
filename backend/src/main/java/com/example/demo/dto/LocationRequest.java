package com.example.demo.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class LocationRequest {

    @NotBlank(message = "Mã địa điểm không được để trống")
    @Size(max = 50, message = "Mã địa điểm tối đa 50 ký tự")
    private String locationCode;

    @NotBlank(message = "Tên địa điểm không được để trống")
    @Size(max = 150, message = "Tên địa điểm tối đa 150 ký tự")
    private String name;

    @NotBlank(message = "Địa chỉ không được để trống")
    @Size(max = 255, message = "Địa chỉ tối đa 255 ký tự")
    private String address;

    @NotNull(message = "Vĩ độ (latitude) không được để trống")
    @DecimalMin(value = "-90.0", message = "Vĩ độ phải từ -90 đến 90")
    @DecimalMax(value = "90.0", message = "Vĩ độ phải từ -90 đến 90")
    @Digits(integer = 2, fraction = 8, message = "Vĩ độ tối đa 2 chữ số phần nguyên và 8 chữ số thập phân")
    private BigDecimal latitude;

    @NotNull(message = "Kinh độ (longitude) không được để trống")
    @DecimalMin(value = "-180.0", message = "Kinh độ phải từ -180 đến 180")
    @DecimalMax(value = "180.0", message = "Kinh độ phải từ -180 đến 180")
    @Digits(integer = 3, fraction = 8, message = "Kinh độ tối đa 3 chữ số phần nguyên và 8 chữ số thập phân")
    private BigDecimal longitude;
}