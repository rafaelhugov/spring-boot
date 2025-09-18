package mppb.microservices.common.error;

public interface MessageParametersException {
    public String getKey();
    public Object[] getParameters();
}
