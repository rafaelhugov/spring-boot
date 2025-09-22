package mppb.microservices.fundoapi.infrastructure.client.bc.dto;

import java.time.LocalDate;

public record BcValorSelicDto (
     String data,
     String valor
){}