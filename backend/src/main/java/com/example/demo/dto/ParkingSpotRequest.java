package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ParkingSpotRequest {

    @NotNull(message = "ID địa điểm không được để trống")
    private Long locationId;

    @NotBlank(message = "Mã ô đỗ không được để trống")
    @Size(max = 50, message = "Mã ô đỗ tối đa 50 ký tự")
    private String spotCode;

    @NotBlank(message = "Loại xe không được để trống")
    @Size(max = 20, message = "Loại xe tối đa 20 ký tự")
    private String vehicleType;

    @Pattern(regexp = "^(AVAILABLE|MAINTENANCE)$", message = "Trạng thái cấu hình chỉ chấp nhận AVAILABLE hoặc MAINTENANCE")
    private String status;
}