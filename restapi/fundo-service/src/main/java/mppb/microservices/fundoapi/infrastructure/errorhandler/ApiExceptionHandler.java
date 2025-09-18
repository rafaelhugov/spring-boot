package mppb.microservices.fundoapi.infrastructure.errorhandler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mppb.microservices.common.error.AppException;
import mppb.microservices.common.error.BusinessRuleException;
import mppb.microservices.common.error.MessageParametersException;
import mppb.microservices.common.error.NotFoundException;
import mppb.microservices.common.error.feign.ClientApiException;
import mppb.microservices.common.error.message.ErrorMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@ControllerAdvice
@Slf4j
@RequiredArgsConstructor
public class ApiExceptionHandler {

    private static final String EXCEPTION_MESSAGE = "Exception {}, Message: {}";

    private final MessageSource messageSource;

    @Value("${spring.profiles.active:}")
    private String activeProfiles;


    @ExceptionHandler( value= BusinessRuleException.class, produces = "application/json")
    public ResponseEntity<ErrorMessage> handleBusinessRuleException(BusinessRuleException exception) {
        return this.handleExceptionInternal(exception, HttpStatus.CONFLICT, null, null);
    }

    @ExceptionHandler( value = Exception.class, produces = "application/json")
    public ResponseEntity<ErrorMessage> handleGenericException(Exception exception) {
        return this.handleExceptionInternal(exception, HttpStatus.INTERNAL_SERVER_ERROR, "erro.requisicao.invalida", null);
    }

    @ExceptionHandler( value = NotFoundException.class, produces = "application/json")
    public ResponseEntity<ErrorMessage> handleNotFoundException(NotFoundException exception) {
        return this.handleExceptionInternal(exception, HttpStatus.NOT_FOUND, "erro.not.found", null);
    }

    @ExceptionHandler( value = AppException.class, produces = "application/json")
    public ResponseEntity<ErrorMessage> handleNotFoundException(AppException exception) {
        return this.handleExceptionInternal(exception, HttpStatus.INTERNAL_SERVER_ERROR, "erro.sistema.interno", null);
    }

    @ExceptionHandler( value = ClientApiException.class, produces = "application/json")
    public ResponseEntity<ErrorMessage> handleNotFoundException(ClientApiException exception) {
        return this.handleExceptionInternal(exception, HttpStatus.SERVICE_UNAVAILABLE, "msg", null);
    }

    public boolean isProdProfileActive() {
        if (activeProfiles == null || activeProfiles.isEmpty()) {
            return false;
        }
        return Arrays.asList(activeProfiles.split(",")).contains("prod");
    }

    private ResponseEntity<ErrorMessage> handleExceptionInternal(Exception exception, HttpStatus status, String key, Object[] args) {
        log.error(EXCEPTION_MESSAGE, exception.getClass().getName(), exception.getMessage());

        String messageKey = (exception instanceof MessageParametersException mpe)
                ? mpe.getKey()
                : key;

        HttpStatus responseStatus = status;
        String message = null;
        String trace = null;

        if (exception instanceof MessageParametersException mpe) {
            message = resolveMessage(messageKey, mpe.getParameters());
        } else if (exception instanceof ErrorResponse er) {
            responseStatus = HttpStatus.resolve(er.getStatusCode().value());
            message = formatErrorResponseDetails(er.getDetailMessageArguments());
        } else {
            if(!isProdProfileActive()){
                trace = getStackTraceAsString(exception);
            }
        }

        if(!isProdProfileActive() && trace == null && status.equals(HttpStatus.INTERNAL_SERVER_ERROR)){
            trace = getStackTraceAsString(exception);
        }

        ErrorMessage errorMessage = new ErrorMessage(
                Instant.now(),
                responseStatus.value(),
                responseStatus.getReasonPhrase(),
                message,
                trace
        );

        return new ResponseEntity<>(errorMessage, responseStatus);
    }

    private String resolveMessage(String key, Object[] params) {
        try {
            return Optional.ofNullable(key)
                    .map(k -> messageSource.getMessage(k, params, LocaleContextHolder.getLocale()))
                    .orElse("");
        } catch (Exception e) {
            return key;
        }
    }

    private String formatErrorResponseDetails(Object[] details) {
        if(details == null){
            return "";
        }
        return Arrays.stream(details)
                .filter(Objects::nonNull)
                .map(Object::toString)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.joining(". "));
    }

    private String getStackTraceAsString(Throwable exception) {
        StringWriter stringWriter = new StringWriter();
        exception.printStackTrace(new PrintWriter(stringWriter));
        return stringWriter.toString();
    }

}
