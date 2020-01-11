package com.makethone.outliers;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeviceData {
    private String hardwareId;
    private String accessToken;
    private Boolean authorized = false;
}
