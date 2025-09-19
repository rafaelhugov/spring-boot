//package mppb.microservices.common.error.message;
//
//import io.swagger.v3.oas.annotations.media.Schema;
//import lombok.AllArgsConstructor;
//import lombok.Getter;
//import lombok.NoArgsConstructor;
//import lombok.Setter;
//
//import java.time.Instant;
//
//@Setter
//@Getter
//@NoArgsConstructor
//@AllArgsConstructor
//@Schema(description = "Objeto que representa um erro em uma solicitação API")
//public class ErrorMessage {
//
//    @Schema(description = "Timestamp da ocorrência do erro")
//    private Instant timestamp;
//
//    @Schema(description = "Código de status da resposta", example = "404")
//    private Integer status;
//
//    @Schema(description = "Tipo de erro")
//    private String error;
//
//    @Schema(description = "Mensagem detalhada do erro")
//    private String message;
//
//    @Schema(description = "Stacktrace")
//    private String trace;
//
//}
