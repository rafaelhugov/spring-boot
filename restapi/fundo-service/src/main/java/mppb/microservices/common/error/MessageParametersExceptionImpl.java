package mppb.microservices.common.error;


import java.io.Serializable;

public class MessageParametersExceptionImpl extends RuntimeException implements Serializable, MessageParametersException {

    /**
     * N�mero serial da classe.
     */
    private static final long serialVersionUID = -7119888141628071840L;

    /**
     * Par�metros da mensagem de erro.
     */
    private Object[] parametros;

    /**
     * Mensagem de erro.
     */
    private String mensagem;

    /**
     * Causa da message.
     */
    private Throwable causa;

    /**
     * Construtor default.
     */
    public MessageParametersExceptionImpl() {
        super();
    }

    /**
     * Construtor com apenas a chave da mensagem de erro.
     * @param key A chave da mensagem.
     */
    public MessageParametersExceptionImpl(String key) {
        this(null, key, new Object[] {});
    }

    /**
     * Constr�i um PrincipalException com a chave da mensagem e a causa.
     * @param causa A causa do erro.
     * @param key A chave da mensagem.
     */
    public MessageParametersExceptionImpl(Throwable causa, String key) {
        this(causa, key, new Object[] {});
    }

    public MessageParametersExceptionImpl(Throwable causa, String key, Exception e) {
        this(causa, key, new Object[] {}, e);
    }

    /**
     * Constr�i um PrincipalException com a chave da mensagem e a causa.
     * @param causa A causa do erro.
     */
    public MessageParametersExceptionImpl(Throwable causa) {
        this(causa, null, new Object[] {});
    }

    /**
     * Constr�i um PrincipalException com a chave da mensagem e seus
     * par�metros.
     * @param causa A causa do erro.
     * @param key A chave da mensagem.
     * @param parameters Os par�metros da mensagem.
     */
    public MessageParametersExceptionImpl(Throwable causa, String key,
                                          Object... parameters) {
        super(key, causa);
        this.causa = causa;
        this.mensagem = key;
        this.parametros = parameters;
    }

    /**
     * Representa��o String da exce��o.
     * @return A representa��o string da exce��o.
     * @see java.lang.Throwable#toString()
     */
    @Override
    public String toString() {
        String retorno = getMensagem() == null ? "" : getMensagem();

        if(parametros != null){
            for (Object obj : parametros) {
                retorno += "|" + obj.toString();
            }
        }

        return retorno;
    }

    /**
     * Constr�i um PrincipalException com a chave da mensagem e seus
     * par�metros.
     * @param key A chave da mensagem.
     * @param parametros Os par�metros da mensagem.
     */
    public MessageParametersExceptionImpl(String key, Object... parametros) {
        this(null, key, parametros);
    }

    /**
     * Retorna os par�mentros para a mensagem de erro.
     * @return Os par�metros para a mensagem de erro.
     */
    public Object[] getParametros() {
        return parametros;
    }

    /**
     * Retorna a mensagem de erro.
     * @see java.lang.Throwable#getMessage()
     * @return A mensagem de erro.
     */
    public String getMensagem() {
        return mensagem;
    }

    public String getMessage(){
        return getMensagem();
        //return toString();
    }

    /**
     * Retorna a causa da message.
     * @return A causa.
     */
    public Throwable getCausa() {
        return causa;
    }

    /**
     * Modifica a causa do message.
     * @param causa A causa da message.
     */
    public void setCausa(Throwable causa) {
        this.causa = causa;
    }

    /**
     * Modifica os par�metros da message.
     * @param parametros Os par�metros.
     */
    public void setParametros(Object[] parametros) {
        this.parametros = parametros;
    }

    /**
     * Modifica a mensagem da message.
     * @param mensagem A mensagem da message.
     */
    public void setMensagem(String mensagem) {
        this.mensagem = mensagem;
    }

    public String getKey() {
        return mensagem;
    }

    public Object[] getParameters() {
        return getParametros();
    }
}
