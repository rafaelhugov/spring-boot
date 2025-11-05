package mppb.microservices.common.core.error;

public interface MessageParametersException {
    public String getKey();
    public Object[] getParameters();
}
