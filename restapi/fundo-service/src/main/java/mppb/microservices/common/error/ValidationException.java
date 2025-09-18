package mppb.microservices.common.error;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class ValidationException extends AppException {

    public ValidationException(String msg){
        super(msg);
    }

    public ValidationException(String msg, String[] args){
        super(msg, args);
    }
}
