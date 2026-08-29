package br.gov.mppb.athena.integracao;

import br.gov.mppb.athena.seguranca.DadosParticipanteCorporativo;
import java.util.List;
import java.util.Locale;

public final class FonteCorporativaParticipantesInstitucional implements FonteCorporativaParticipantes {

    private final List<DadosParticipanteCorporativo> registrosInstitucionais;

    public FonteCorporativaParticipantesInstitucional(List<DadosParticipanteCorporativo> registrosInstitucionais) {
        if (registrosInstitucionais == null) {
            throw new IllegalArgumentException("registros institucionais devem ser informados.");
        }
        this.registrosInstitucionais = List.copyOf(registrosInstitucionais);
    }

    public static FonteCorporativaParticipantesInstitucional cadastroGovernadoPadrao() {
        return new FonteCorporativaParticipantesInstitucional(List.of(
                new DadosParticipanteCorporativo(
                        "Usuario Institucional",
                        "usuario-avaliador",
                        "Servidor",
                        "Analista ministerial",
                        "avaliador@mppb.mp.br",
                        "avaliador",
                        "DITEC",
                        null
                ),
                new DadosParticipanteCorporativo(
                        "Ana Institucional",
                        "123456",
                        "Servidora",
                        "Analista ministerial",
                        "ana.institucional@mppb.mp.br",
                        "ana.institucional",
                        "DITEC",
                        null
                )
        ));
    }

    @Override
    public List<DadosParticipanteCorporativo> pesquisar(ConsultaParticipantesCorporativos consulta) {
        if (consulta == null) {
            throw new IllegalArgumentException("consulta corporativa de participantes deve ser informada.");
        }
        return registrosInstitucionais.stream()
                .filter(participante -> atendeConsulta(participante, consulta))
                .map(this::somenteDadosInstitucionaisPermitidos)
                .toList();
    }

    private boolean atendeConsulta(DadosParticipanteCorporativo participante, ConsultaParticipantesCorporativos consulta) {
        return contem(participante.nome(), consulta.termo())
                || contem(participante.login(), consulta.termo())
                || contem(participante.email(), consulta.termo())
                || contem(participante.matricula(), consulta.matricula())
                || contem(participante.unidadeOuLotacao(), consulta.unidadeOuLotacao());
    }

    private DadosParticipanteCorporativo somenteDadosInstitucionaisPermitidos(DadosParticipanteCorporativo participante) {
        return new DadosParticipanteCorporativo(
                participante.nome(),
                participante.matricula(),
                participante.vinculo(),
                participante.cargoOuFuncao(),
                participante.email(),
                participante.login(),
                participante.unidadeOuLotacao(),
                null
        );
    }

    private static boolean contem(String valor, String filtro) {
        if (filtro == null) {
            return false;
        }
        return normalizar(valor).contains(normalizar(filtro));
    }

    private static String normalizar(String valor) {
        if (valor == null) {
            return "";
        }
        return valor.toLowerCase(Locale.ROOT);
    }
}
