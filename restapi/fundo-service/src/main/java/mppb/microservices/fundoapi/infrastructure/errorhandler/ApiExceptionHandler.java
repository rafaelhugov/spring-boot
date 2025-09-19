package mppb.microservices.fundoapi.infrastructure.errorhandler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mppb.microservices.common.error.AppException;
import mppb.microservices.common.error.BusinessRuleException;
import mppb.microservices.common.error.MessageParametersException;
import mppb.microservices.common.error.NotFoundException;
import mppb.microservices.common.error.feign.ClientApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
@RequiredArgsConstructor
public class ApiExceptionHandler {

    private final MessageSource messageSource;

    @Value("${spring.profiles.active:}")
    private String activeProfiles;

    private boolean isProdProfileActive() {
        if (activeProfiles == null || activeProfiles.isEmpty()) {
            return false;
        }
        return Arrays.asList(activeProfiles.split(",")).contains("prod");
    }

    @ExceptionHandler(value = BusinessRuleException.class)
    public ResponseEntity<ProblemDetail> handleBusinessRuleException(BusinessRuleException exception) {
        return createProblemDetail(HttpStatus.CONFLICT, "Regra de negócio violada", exception);
    }

    @ExceptionHandler(value = NoResourceFoundException.class)
    public ResponseEntity<ProblemDetail> handleNoResourceFoundException(NoResourceFoundException exception) {
        //return createProblemDetail(HttpStatus.NOT_FOUND, "Recurso não encontrado", exception);
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        problemDetail.setTitle("Recurso não encontrado");
        problemDetail.setDetail("O endpoint solicitado não existe.");


        addExtraProperties(problemDetail, exception);

        return ResponseEntity.of(problemDetail).build();
    }


    @ExceptionHandler(value = NotFoundException.class)
    public ResponseEntity<ProblemDetail> handleNotFoundException(NotFoundException exception) {
        return createProblemDetail(HttpStatus.NOT_FOUND, "Recurso não encontrado", exception);
    }

    @ExceptionHandler(value = ClientApiException.class)
    public ResponseEntity<ProblemDetail> handleClientApiException(ClientApiException exception) {
        return createProblemDetail(HttpStatus.SERVICE_UNAVAILABLE, "Serviço externo indisponível", exception);
    }

    @ExceptionHandler(value = AppException.class)
    public ResponseEntity<ProblemDetail> handleAppException(AppException exception) {
        return createProblemDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno no servidor", exception);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidationExceptions(MethodArgumentNotValidException exception) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problemDetail.setTitle("Erros de validação");
        problemDetail.setDetail("A requisição contém dados inválidos.");

        List<FieldErrorDetail> fieldErrors = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> new FieldErrorDetail(error.getField(), messageSource.getMessage(error, LocaleContextHolder.getLocale())))
                .collect(Collectors.toList());

        problemDetail.setProperty("fieldErrors", fieldErrors);
        addExtraProperties(problemDetail, exception);

        return ResponseEntity.of(problemDetail).build();
    }

    private ResponseEntity<ProblemDetail> createProblemDetail(HttpStatus status, String title, Exception exception) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(status);
        problemDetail.setTitle(title);
        if(exception instanceof MessageParametersException){
            MessageParametersException msgException = (MessageParametersException) exception;
            String detail = resolveMessage(msgException.getKey(), msgException.getParameters());
            problemDetail.setDetail(detail);
        } else {
            problemDetail.setDetail(exception.getMessage());
        }


        addExtraProperties(problemDetail, exception);
        return ResponseEntity.of(problemDetail).build();
    }

    private void addExtraProperties(ProblemDetail problemDetail, Exception exception) {
        if (exception == null) {
            return;
        }
        problemDetail.setProperty("timestamp", Instant.now());
        if (!isProdProfileActive()) {
            problemDetail.setProperty("trace", getStackTraceAsString(exception));
        }
    }

    private String resolveMessage(String key, Object[] params) {
        try {
            return Optional.ofNullable(key)
                    .map(k -> {
                        try {
                            return messageSource.getMessage(k, params, LocaleContextHolder.getLocale());
                        } catch (Exception e) {
                            log.error(e.getMessage(), e);
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