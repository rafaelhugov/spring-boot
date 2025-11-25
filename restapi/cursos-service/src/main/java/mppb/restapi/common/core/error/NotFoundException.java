package mppb.restapi.common.core.error;

public class NotFoundException extends AppException {

    public NotFoundException(String msg){
        super(msg);
    }

    public NotFoundException(String msg, String[] args){
        super(msg, args);
    }

    public NotFoundException(Throwable causa, String key) {
        super(causa, key);
    }

    public NotFoundException(Throwable causa) {
        super(causa);
    }

    public NotFoundException(Throwable causa, String key,
                            Object... parameters) {
        super(key, causa, parameters);

    }

}
