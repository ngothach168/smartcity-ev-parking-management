package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EvChargerRequest {

    @NotNull(message = "ID địa điểm không được để trống")
    private Long locationId;

    @NotBlank(message = "Mã trụ sạc không được để trống")
    @Size(max = 50, message = "Mã trụ sạc tối đa 50 ký tự")
    private String chargerCode;

    @NotBlank(message = "Loại cổng sạc không được để trống")
    @Size(max = 20, message = "Loại cổng sạc tối đa 20 ký tự")
    private String portType;

    @Pattern(regexp = "^(AVAILABLE|MAINTENANCE)$", message = "Trạng thái cấu hình chỉ chấp nhận AVAILABLE hoặc MAINTENANCE")
    private String status;
}