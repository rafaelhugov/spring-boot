package mppb.restapi.common.core.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import mppb.restapi.common.core.error.AppException;
import mppb.restapi.common.core.error.BusinessRuleException;
import mppb.restapi.common.core.error.MessageParametersException;
import mppb.restapi.common.core.error.NotFoundException;
import mppb.restapi.common.core.error.feign.ClientApiException;
import mppb.restapi.common.infrastructure.env.RuntimeEnv;
import org.modelmapper.MappingException;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.web.server.ResponseStatusException;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
@RequiredArgsConstructor
public class ApiExceptionHandler {

    private final String ERRORS_PROPERTY = "errors";

    private final String TRACE_PROPERTY = "trace";

    private final String TIMESTAMP_PROPERTY = "timestamp";

    private final MessageSource messageSource;

    private final RuntimeEnv runtimeEnv;

    @ExceptionHandler(value = BusinessRuleException.class)
    public ResponseEntity<ProblemDetail> handleBusinessRuleException(BusinessRuleException exception) {
        return createProblemDetail(HttpStatus.UNPROCESSABLE_ENTITY, "Regra de negócio violada", exception, null);
    }

    @ExceptionHandler(value = NoResourceFoundException.class)
    public ResponseEntity<ProblemDetail> handleNoResourceFoundException(NoResourceFoundException exception) {
        return createProblemDetail(HttpStatus.NOT_FOUND, "Recurso não encontrado", exception, null);
    }

    @ExceptionHandler(value = NotFoundException.class)
    public ResponseEntity<ProblemDetail> handleNotFoundException(NotFoundException exception) {
        return createProblemDetail(HttpStatus.NOT_FOUND, "Recurso não encontrado", exception, null);
    }

    @ExceptionHandler(value = ResponseStatusException.class)
    public ResponseEntity<ProblemDetail> handleResponseStatusException(ResponseStatusException exception) {
        HttpStatus status = HttpStatus.valueOf(exception.getStatusCode().value());
        String title = exception.getReason() != null ? exception.getReason() : status.getReasonPhrase();
        return createProblemDetail(status, title, exception, title);
    }

    @ExceptionHandler(value = {AccessDeniedException.class, AuthorizationDeniedException.class})
    public ResponseEntity<ProblemDetail> handleAccessDeniedException(Exception exception) {
        return createProblemDetail(HttpStatus.FORBIDDEN, "Acesso negado", exception, "Acesso negado.");
    }

    @ExceptionHandler(value = feign.RetryableException.class)
    public ResponseEntity<ProblemDetail> handleRetryableException(feign.RetryableException exception) {
        return createProblemDetail(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Serviço externo indisponível",
                exception,
                "O serviço dependente não pôde ser contatado. Por favor, tente novamente mais tarde."
        );
    }

    @ExceptionHandler(value = ClientApiException.class)
    public ResponseEntity<ProblemDetail> handleClientApiException(ClientApiException exception) {
        return createProblemDetail(HttpStatus.BAD_GATEWAY, "Erro em serviço externo", exception, null);
    }



    @ExceptionHandler(value = MappingException.class)
    public ResponseEntity<ProblemDetail> handleMappingException(MappingException exception) {
        return createProblemDetail(HttpStatus.BAD_REQUEST, "Erro no mapeamento dos dados", exception, null );
    }

    @ExceptionHandler(value = PropertyReferenceException.class)
    public ResponseEntity<ProblemDetail> handlePropertyReferenceException(PropertyReferenceException exception) {
        return createProblemDetail(
                HttpStatus.BAD_REQUEST,
                "Parâmetro de ordenação inválido",
                exception,
                String.format("A propriedade '%s' não existe no tipo '%s'.", exception.getPropertyName(), exception.getType().getType().getSimpleName())
        );
    }

    @ExceptionHandler(value = AppException.class)
    public ResponseEntity<ProblemDetail> handleAppException(AppException exception) {
        return createProblemDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Erro genérico da aplicação", exception,
                "Ocorreu um erro inesperado no sistema. Por favor, tente novamente mais tarde.");
    }

    @ExceptionHandler(value = Exception.class)
    public ResponseEntity<ProblemDetail> handleGenericException(Exception exception, HttpServletResponse response) {
        // Se o Content-Type já foi definido como PDF, não podemos retornar ProblemDetail via ResponseEntity.
        // Gravamos o JSON diretamente no response stream para não causar HttpMessageNotWritableException.
        String contentType = response.getContentType();
        if (contentType != null && contentType.contains("application/pdf")) {
            log.error("Erro durante geração de PDF. Respondendo com JSON direto.", exception);
            response.reset();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.setContentType("application/problem+json;charset=UTF-8");
            try {
                response.getWriter().write("{\"status\":500,\"title\":\"Erro interno no servidor\",\"detail\":\"Ocorreu um erro inesperado no sistema. Por favor, tente novamente mais tarde.\"}");
                response.getWriter().flush();
            } catch (IOException e) {
                log.error("Falha ao escrever resposta de erro JSON", e);
            }
            return null;
        }
        return createProblemDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno no servidor", exception, "Ocorreu um erro inesperado no sistema. Por favor, tente novamente mais tarde.");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidationExceptions(MethodArgumentNotValidException exception) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problemDetail.setTitle("Erros de validação");
        problemDetail.setDetail("A requisição contém dados inválidos.");

        List<Map<String, String>> errors = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> Map.of(
                        "field", error.getField(),
                        "msg", messageSource.getMessage(error, LocaleContextHolder.getLocale())
                ))
                .collect(Collectors.toList());

        problemDetail.setProperty(ERRORS_PROPERTY, errors); // Alterado de "fieldErrors" para "errors"
        addExtraProperties(problemDetail, exception, false);

        return ResponseEntity.of(problemDetail).build();
    }

    private ResponseEntity<ProblemDetail> createProblemDetail(HttpStatus status, String title, Exception exception, String detail) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(status);
        problemDetail.setTitle(title);

        if(detail != null ){
            problemDetail.setDetail(detail);
        } else if (exception instanceof MessageParametersException) {
            MessageParametersException msgException = (MessageParametersException) exception;
            String detailException = resolveMessage(msgException.getKey(), msgException.getParameters());
            problemDetail.setDetail(detailException);
        } else if (exception instanceof AppException) {
            problemDetail.setDetail(exception.getMessage());
        } else {
            problemDetail.setDetail(title);
        }

        List errors = new ArrayList();
        if(problemDetail.getDetail() != null){
            errors =  List.of(Map.of(
                    "msg", problemDetail.getDetail())
            );
        }

        problemDetail.setProperty(ERRORS_PROPERTY, errors);

        addExtraProperties(problemDetail, exception);
        return ResponseEntity.of(problemDetail).build();
    }

    private void addExtraProperties(ProblemDetail problemDetail, Exception exception) {
        addExtraProperties(problemDetail, exception, true);
    }

    private void addExtraProperties(ProblemDetail problemDetail, Exception exception, boolean includeTrace) {
        if (exception == null) {
            return;
        }
        problemDetail.setProperty(TIMESTAMP_PROPERTY, Instant.now());
        if (includeTrace && runtimeEnv.shouldIncludeTrace()) {
            problemDetail.setProperty(TRACE_PROPERTY, getStackTraceAsString(exception));
        }
    }

    private String resolveMessage(String key, Object[] params) {
        try {
            return Optional.ofNullable(key)
                    .map(k -> {
                        try {
                            return messageSource.getMessage(k, params, LocaleContextHolder.getLocale());
                        } catch (Exception e) {
                           // log.error(e.getMessage(), e);
                            return messageSource.getMessage("msg", new Object[]{key}, LocaleContextHolder.getLocale());
                        }
                    })
                    .orElse("");
        } catch (Exception e) {
            return key;
        }
    }

    private String getStackTraceAsString(Throwable exception) {
        StringWriter stringWriter = new StringWriter();
        exception.printStackTrace(new PrintWriter(stringWriter));
        return stringWriter.toString();
    }

    private static class FieldErrorDetail {
        public String field;
        public String message;
        public FieldErrorDetail(String field, String message) {
            this.field = field;
            this.message = message;
        }
    }
}
