package mppb.microservices.common.error;

public class NotFoundException extends AppException{

    public NotFoundException(String msg){
        super(msg);
    }

    public NotFoundException(String msg, String[] args){
        super(msg, args);
    }
}
