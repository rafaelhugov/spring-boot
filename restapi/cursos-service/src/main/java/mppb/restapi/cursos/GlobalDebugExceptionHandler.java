package mppb.restapi.cursos;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalDebugExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(GlobalDebugExceptionHandler.class);

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleAllExceptions(Exception ex) {
        logger.error("==== CAUGHT EXCEPTION GLOBALLY BEFORE REACHING CONTROLLER ==================");
        logger.error("Message: ", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Erro detalhado do servidor: " + ex.getMessage());
    }
}
