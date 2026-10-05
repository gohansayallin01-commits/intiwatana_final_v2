package com.transporte.sistema.dto.response;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImagenPortalResponse {

    private Long id;
    private String seccion;
    private String url;
    private String miniaturaUrl;
    private String mime;
    private Long pesoBytes;
    private Integer ancho;
    private Integer alto;
    private String checksum;
    private String textoAlt;
    private String autor;
    private String fuente;
    private String licencia;
    private String urlOrigen;
    private Boolean autorizacionComercial;
    private String subidoPor;
}
