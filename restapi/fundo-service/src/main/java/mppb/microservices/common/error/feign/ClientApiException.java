package mppb.microservices.common.error.feign;

import lombok.NoArgsConstructor;
import mppb.microservices.common.error.AppException;

@NoArgsConstructor
public class ClientApiException extends AppException {



    private int status;

    public ClientApiException(String msg){
        super(msg);
    }

    public ClientApiException(int status,String msg){
        super(msg);
        this.status = status;
    }

    public ClientApiException(int status, String msg, String[] args ){
        super(msg, args);
        this.status = status;
    }

    public ClientApiException(int status, String msg, String[] args, Exception e){
        super(e, msg, args);
        this.status = status;
    }
}