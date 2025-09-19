package mppb.microservices.common.error;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class BusinessRuleException extends AppException {

    public BusinessRuleException(String msg){
        super(msg);
    }

    public BusinessRuleException(String msg, String[] args){
        super(msg, args);
    }


    public BusinessRuleException(Throwable causa, String key,
                             Object... parameters) {
        super(key, causa, parameters);

    }

}
