package br.gov.mppb.athena.seguranca;

import br.gov.mppb.athena.dominio.PapelProjeto;
import br.gov.mppb.athena.dominio.ParticipanteProjeto;
import br.gov.mppb.athena.dominio.ProjetoInstitucional;
import br.gov.mppb.athena.dominio.UsuarioInstitucional;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;

public final class PoliticaSegurancaAthena {

    public static final String LOTACAO_ADMINISTRATIVA = "DITEC";
    public static final String AUTORIZACAO_ADMINISTRATIVA = "athena.administrar";

    private PoliticaSegurancaAthena() {
    }

    public static ResultadoAutorizacao autorizar(
            ProjetoInstitucional projeto,
            ContextoAutorizacaoInstitucional contexto,
            OperacaoProjeto operacao
    ) {
        if (projeto == null) {
            throw new IllegalArgumentException("projeto deve ser informado.");
        }
        if (operacao == null) {
            throw new IllegalArgumentException("operacao deve ser informada.");
        }
        if (contexto == null || !contexto.autenticado()) {
            return ResultadoAutorizacao.negar("identidade institucional autenticada deve ser informada.");
        }
        if (operacao == OperacaoProjeto.ADMINISTRAR) {
            return autorizarAdministracao(contexto);
        }

        UsuarioInstitucional usuario = contexto.usuario();
        if (contexto.possuiAutorizacao(autorizacaoEspecifica(projeto, operacao))) {
            return ResultadoAutorizacao.permitir("autorizacao institucional especifica no projeto.");
        }

        return projeto.participantes().stream()
                .filter(participante -> participante.usuario().identificador().equals(usuario.identificador()))
                .findFirst()
                .map(participante -> autorizarPorPapel(participante, operacao))
                .orElseGet(() -> ResultadoAutorizacao.negar("usuario sem vinculo, papel ou permissao especifica no projeto."));
    }

    public static String autorizacaoEspecifica(ProjetoInstitucional projeto, OperacaoProjeto operacao) {
        if (projeto == null) {
            throw new IllegalArgumentException("projeto deve ser informado.");
        }
        if (operacao == null) {
            throw new IllegalArgumentException("operacao deve ser informada.");
        }
        return "ATHENA_PROJETO_" + projeto.identificador().valor() + "_" + operacao.name();
    }

    public static ResultadoAutorizacao autorizarPapelVinculado(
            ProjetoInstitucional projeto,
            ContextoAutorizacaoInstitucional contexto,
            PapelProjeto papelExigido
    ) {
        if (projeto == null) {
            throw new IllegalArgumentException("projeto deve ser informado.");
        }
        if (papelExigido == null) {
            throw new IllegalArgumentException("papel exigido deve ser informado.");
        }
        if (contexto == null || !contexto.autenticado()) {
            return ResultadoAutorizacao.negar("identidade institucional autenticada deve ser informada.");
        }

        return projeto.participantes().stream()
                .filter(participante -> participante.usuario().identificador()
                        .equals(contexto.usuario().identificador()))
                .filter(participante -> participante.papel() == papelExigido)
                .findFirst()
                .map(participante -> ResultadoAutorizacao.permitir(
                        "operacao autorizada pelo vinculo " + papelExigido.name().toLowerCase(Locale.ROOT) + "."))
                .orElseGet(() -> ResultadoAutorizacao.negar(
                        "operacao exige vinculo ativo como " + papelExigido.name().toLowerCase(Locale.ROOT) + "."));
    }

    private static ResultadoAutorizacao autorizarAdministracao(ContextoAutorizacaoInstitucional contexto) {
        boolean lotadoNaDitec = LOTACAO_ADMINISTRATIVA.equalsIgnoreCase(contexto.usuario().unidade().trim());
        boolean autorizado = contexto.possuiAutorizacao(AUTORIZACAO_ADMINISTRATIVA);
        if (lotadoNaDitec && autorizado) {
            return ResultadoAutorizacao.permitir("administracao autorizada por lotacao aplicavel e autorizacao institucional especifica.");
        }
        return ResultadoAutorizacao.negar("administracao exige lotacao DITEC e autorizacao institucional especifica.");
    }

    private static ResultadoAutorizacao autorizarPorPapel(ParticipanteProjeto participante, OperacaoProjeto operacao) {
        Set<OperacaoProjeto> operacoesPermitidas = switch (participante.papel()) {
            case RESPONSAVEL_TECNICO -> EnumSet.of(
                    OperacaoProjeto.CONSULTAR,
                    OperacaoProjeto.AVALIAR,
                    OperacaoProjeto.COMENTAR,
                    OperacaoProjeto.CONFIRMAR
            );
            case AVALIADOR -> EnumSet.of(
                    OperacaoProjeto.CONSULTAR,
                    OperacaoProjeto.AVALIAR,
                    OperacaoProjeto.COMENTAR
            );
            case PARTICIPANTE -> EnumSet.of(OperacaoProjeto.CONSULTAR);
        };

        if (operacoesPermitidas.contains(operacao)) {
            return ResultadoAutorizacao.permitir("operacao autorizada pelo papel " + participante.papel().name().toLowerCase(Locale.ROOT) + ".");
        }
        return ResultadoAutorizacao.negar("papel do usuario nao autoriza a operacao solicitada no projeto.");
    }
}
