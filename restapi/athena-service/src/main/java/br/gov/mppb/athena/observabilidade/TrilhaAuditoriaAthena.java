package br.gov.mppb.athena.observabilidade;

import br.gov.mppb.athena.dominio.IdentificadorProjeto;
import br.gov.mppb.athena.dominio.IdentificadorUsuario;
import java.time.OffsetDateTime;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class TrilhaAuditoriaAthena {

    private static final Pattern CPF = Pattern.compile("\\b\\d{3}\\.?\\d{3}\\.?\\d{3}-?\\d{2}\\b");
    private static final Pattern CHAVE_SENSIVEL = Pattern.compile(".*(cpf|conteudo|segredo|senha|token).*");

    private TrilhaAuditoriaAthena() {
    }

    public static Set<AcaoAuditavel> acoesObrigatorias() {
        return EnumSet.allOf(AcaoAuditavel.class);
    }

    public static EventoAuditoria registrar(
            IdentificadorUsuario ator,
            OffsetDateTime ocorridoEm,
            IdentificadorProjeto projeto,
            RecursoAuditavel recursoAfetado,
            AcaoAuditavel acao,
            ResultadoEventoAuditavel resultado,
            Map<String, String> detalhes
    ) {
        return new EventoAuditoria(
                ator,
                ocorridoEm,
                projeto,
                recursoAfetado,
                acao,
                resultado,
                detalhesSeguros(detalhes),
                PoliticaRetencaoAuditavel.PENDENTE_DEFINICAO_INSTITUCIONAL
        );
    }

    public static Map<String, String> detalhesSeguros(Map<String, String> detalhes) {
        if (detalhes == null || detalhes.isEmpty()) {
            return Map.of();
        }
        return detalhes.entrySet()
                .stream()
                .filter(entrada -> chavePermitida(entrada.getKey()))
                .collect(Collectors.toUnmodifiableMap(
                        entrada -> entrada.getKey().strip(),
                        entrada -> mascararCpf(valorSeguro(entrada.getValue()))
                ));
    }

    private static boolean chavePermitida(String chave) {
        return chave != null && !chave.isBlank() && !CHAVE_SENSIVEL.matcher(chave.toLowerCase()).matches();
    }

    private static String valorSeguro(String valor) {
        if (valor == null || valor.isBlank()) {
            return "";
        }
        return valor.strip();
    }

    private static String mascararCpf(String valor) {
        return CPF.matcher(valor).replaceAll("<CPF_REMOVIDO>");
    }
}
