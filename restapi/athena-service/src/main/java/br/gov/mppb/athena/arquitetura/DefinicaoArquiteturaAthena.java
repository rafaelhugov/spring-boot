package br.gov.mppb.athena.arquitetura;

import java.util.List;
import java.util.Set;

public final class DefinicaoArquiteturaAthena {

    public static final String FRONTEND_TECNOLOGIA = "Vue 3";
    public static final String FRONTEND_DIRETORIO_LOGICO = "frontend";
    public static final String BACKEND_TECNOLOGIA = "Java Spring Boot";
    public static final String BACKEND_DIRETORIO_LOGICO = "restapi/athena-service";

    private static final List<ResponsabilidadeCamada> RESPONSABILIDADES = List.of(
            new ResponsabilidadeCamada(
                    CamadaAthena.API,
                    "br.gov.mppb.athena.api",
                    Set.of(
                            "expor contratos REST para consumo da interface Vue 3",
                            "receber e devolver representacoes de recursos autorizados",
                            "delegar regras de negocio para a camada de aplicacao"
                    )
            ),
            new ResponsabilidadeCamada(
                    CamadaAthena.APLICACAO,
                    "br.gov.mppb.athena.aplicacao",
                    Set.of(
                            "orquestrar casos de uso do Athena",
                            "coordenar dominio, persistencia, integracoes e seguranca",
                            "manter transacoes sem acoplar a interface web"
                    )
            ),
            new ResponsabilidadeCamada(
                    CamadaAthena.DOMINIO,
                    "br.gov.mppb.athena.dominio",
                    Set.of(
                            "concentrar regras de dominio do Athena",
                            "modelar conceitos e invariantes institucionais",
                            "permanecer independente de API, persistencia e frontend"
                    )
            ),
            new ResponsabilidadeCamada(
                    CamadaAthena.PERSISTENCIA,
                    "br.gov.mppb.athena.persistencia",
                    Set.of(
                            "persistir dados no esquema athena",
                            "reconstruir estado a partir de registros armazenados",
                            "adaptar entidades de armazenamento para o dominio"
                    )
            ),
            new ResponsabilidadeCamada(
                    CamadaAthena.INTEGRACOES,
                    "br.gov.mppb.athena.integracoes",
                    Set.of(
                            "integrar fontes e servicos institucionais externos",
                            "isolar contratos externos do dominio",
                            "entregar dados externos tratados para a aplicacao"
                    )
            ),
            new ResponsabilidadeCamada(
                    CamadaAthena.SEGURANCA,
                    "br.gov.mppb.athena.seguranca",
                    Set.of(
                            "concentrar autenticacao institucional",
                            "validar autorizacao por recurso e operacao",
                            "proteger dados sensiveis antes de expor respostas"
                    )
            )
    );

    private static final Set<DependenciaCamada> DEPENDENCIAS_PERMITIDAS = Set.of(
            new DependenciaCamada(CamadaAthena.API, CamadaAthena.APLICACAO),
            new DependenciaCamada(CamadaAthena.API, CamadaAthena.SEGURANCA),
            new DependenciaCamada(CamadaAthena.APLICACAO, CamadaAthena.DOMINIO),
            new DependenciaCamada(CamadaAthena.APLICACAO, CamadaAthena.PERSISTENCIA),
            new DependenciaCamada(CamadaAthena.APLICACAO, CamadaAthena.INTEGRACOES),
            new DependenciaCamada(CamadaAthena.APLICACAO, CamadaAthena.SEGURANCA),
            new DependenciaCamada(CamadaAthena.PERSISTENCIA, CamadaAthena.DOMINIO),
            new DependenciaCamada(CamadaAthena.INTEGRACOES, CamadaAthena.DOMINIO),
            new DependenciaCamada(CamadaAthena.SEGURANCA, CamadaAthena.DOMINIO)
    );

    private DefinicaoArquiteturaAthena() {
    }

    public static List<ResponsabilidadeCamada> responsabilidades() {
        return RESPONSABILIDADES;
    }

    public static Set<DependenciaCamada> dependenciasPermitidas() {
        return DEPENDENCIAS_PERMITIDAS;
    }
}
