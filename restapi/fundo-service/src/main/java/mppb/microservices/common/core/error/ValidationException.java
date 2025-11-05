package mppb.microservices.common.core.error;

public class ValidationException extends BusinessRuleException{

    public ValidationException(String msg){
        super(msg);
    }

    public ValidationException(String msg, String[] args){
        super(msg, args);
    }

    public ValidationException(Throwable causa, String key) {
        super(causa, key);
    }

    public ValidationException(Throwable causa, String key,
                               Object... parameters) {
        super(causa, key, parameters);

    }

}
