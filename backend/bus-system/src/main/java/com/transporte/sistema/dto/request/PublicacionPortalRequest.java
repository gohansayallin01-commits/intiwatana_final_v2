package com.transporte.sistema.dto.request;

import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PublicacionPortalRequest {

    @Size(max = 500)
    private String observacion;
}
