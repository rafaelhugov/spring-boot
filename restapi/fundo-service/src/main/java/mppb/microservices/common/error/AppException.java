package mppb.microservices.common.error;

public class AppException extends MessageParamtetersExceptionImpl {
    private static final long serialVersionUID = 1515675097655844799L;



    public AppException(Exception e, String msg){
        super(e, msg);
    }

    public AppException(String msg){
        super(msg);
    }

    public AppException(Exception e) {
        super(e, e.getMessage());
    }

    public AppException(String key, Throwable causa) {
        super(causa, key);
    }

    public AppException() {
        super();
    }

    public AppException(String msg, String[] args) {
        super(msg, args);
    }

    public AppException(Exception e, String msg, String[] args) {
        super(e, msg, args);
    }
}
