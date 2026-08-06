package br.gov.mppb.ncap.feature.servidor;

/**
 * Projection para queries nativas que buscam servidores na tabela corporativa {@code public.servidor}.
 */
public interface ServidorProjection {

    Long getId();

    String getNome();

    String getCargoNome();

    /** "Servidor" ou "Membro" — derivado de {@code recursoshumanos.tipocargo.tipo}. */
    String getTipoCargoNome();

    String getServidorMatricula();

    String getSituacao();

    String getEmail();
}
