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
}
