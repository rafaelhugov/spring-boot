package mppb.restapi.mobile.feature.designacao.mapper;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Component;

import mppb.restapi.mobile.feature.designacao.dto.DesignacaoAtivaItemResponse;
import mppb.restapi.mobile.feature.designacao.dto.DesignacoesAtivasResponse;
import mppb.restapi.mobile.feature.designacao.model.DesignacaoAtivaRow;

@Component
public class DesignacaoMapper {

    public DesignacoesAtivasResponse toResponse(List<DesignacaoAtivaRow> rows) {
        String tipoUsuario = rows.stream()
                .map(this::tipoUsuario)
                .filter(tipo -> !"INDEFINIDO".equals(tipo))
                .findFirst()
                .orElse("INDEFINIDO");

        List<DesignacaoAtivaItemResponse> itens = rows.stream()
                .sorted(Comparator
                        .comparingInt(this::ordemTipo)
                        .thenComparing(DesignacaoAtivaRow::dataInicial,
                                Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::toItem)
                .toList();

        return new DesignacoesAtivasResponse(tipoUsuario, itens);
    }

    public DesignacaoAtivaItemResponse toItem(DesignacaoAtivaRow row) {
        String tipoDescricao = tipoDescricao(row);
        String titulo = titulo(row);
        String subtitulo = subtitulo(row, tipoDescricao);
        boolean publicacaoInicial = isPublicacaoInicial(row);
        String naturezaAto = formatarNaturezaAto(row);

        return new DesignacaoAtivaItemResponse(
                row.id(),
                row.tipoCodigo(),
                tipoDescricao,
                titulo,
                subtitulo,
                firstNonBlank(row.orgaoExecucaoNome(), row.orgaoNome()),
                row.funcaoNome(),
                row.dataInicial(),
                row.dataFinal(),
                publicacaoInicial ? null : row.atoNumero(),
                publicacaoInicial ? null : row.dataPublicacao(),
                naturezaAto);
    }

    public String tipoUsuario(DesignacaoAtivaRow row) {
        if ("M".equalsIgnoreCase(row.tipoCargo())) {
            return "MEMBRO";
        }
        if ("S".equalsIgnoreCase(row.tipoCargo())) {
            return "SERVIDOR";
        }
        return "INDEFINIDO";
    }

    public String tipoDescricao(DesignacaoAtivaRow row) {
        if ("M".equalsIgnoreCase(row.tipoCargo())) {
            return switch (safe(row.tipoCodigo()).toUpperCase()) {
                case "T" -> "Titularidade";
                case "S" -> "Substituição Cumulativa";
                case "F" -> "Função";
                default -> firstNonBlank(row.tipoDescricaoOriginal(), "Designação");
            };
        }

        String codigo = safe(row.tipoCodigo()).toUpperCase();
        if ("L".equals(codigo)) {
            return "Lotação";
        }
        if ("F".equals(codigo)) {
            return "Função";
        }

        String original = firstNonBlank(row.tipoDescricaoOriginal(), row.tipoCargoNome(), row.tipoCodigo());
        String normalizado = safe(original).toLowerCase();
        if (normalizado.contains("lota")) {
            return "Lotação";
        }
        if (normalizado.contains("fun")) {
            return "Função";
        }
        if (normalizado.contains("atribui") || normalizado.contains("exerc")) {
            return "Exercício de atribuições";
        }
        return "Designação temporária";
    }

    private int ordemTipo(DesignacaoAtivaRow row) {
        if ("M".equalsIgnoreCase(row.tipoCargo())) {
            return switch (safe(row.tipoCodigo()).toUpperCase()) {
                case "T" -> 0;
                case "S" -> 1;
                case "F" -> 2;
                default -> 9;
            };
        }
        return switch (tipoDescricao(row)) {
            case "Lotação" -> 0;
            case "Função" -> 1;
            case "Exercício de atribuições" -> 2;
            case "Designação temporária" -> 3;
            default -> 9;
        };
    }

    private String titulo(DesignacaoAtivaRow row) {
        if (isTeletrabalho(row)) {
            return "Teletrabalho";
        }
        if ("F".equalsIgnoreCase(row.tipoCodigo()) && isNotBlank(row.funcaoNome())) {
            return row.funcaoNome();
        }
        if ("S".equalsIgnoreCase(row.tipoCargo()) && "L".equalsIgnoreCase(row.tipoCodigo())) {
            return firstNonBlank(row.orgaoNome(), row.orgaoExecucaoNome(), row.tipoDescricaoOriginal(), "Designação");
        }
        return firstNonBlank(row.orgaoExecucaoNome(), row.orgaoNome(), row.funcaoNome(), "Designação");
    }

    private String subtitulo(DesignacaoAtivaRow row, String tipoDescricao) {
        if (isTeletrabalho(row)) {
            return firstNonBlank(row.orgaoNome(), row.orgaoExecucaoNome());
        }
        if ("Função".equals(tipoDescricao)) {
            String orgao = firstNonBlank(row.orgaoNome(), row.orgaoExecucaoNome());
            if (isNotBlank(orgao) && !orgao.equalsIgnoreCase(titulo(row))) {
                return orgao;
            }
        }
        return null;
    }

    private boolean isTeletrabalho(DesignacaoAtivaRow row) {
        if (row.naturezaAtoId() != null && row.naturezaAtoId() == 2202L) {
            return true;
        }
        String nat = safe(row.naturezaAtoNome()).toUpperCase();
        if (nat.contains("TELETRABALHO") || nat.contains("TRABALHO EM CASA") || nat.contains("HOME OFFICE")) {
            return true;
        }
        String func = safe(row.funcaoNome()).toUpperCase();
        return func.contains("EXERCER ATRIBUIÇÕES") && nat.contains("TELETRABALHO");
    }

    public String formatarNaturezaAto(DesignacaoAtivaRow row) {
        if (isTeletrabalho(row)) {
            return "Teletrabalho";
        }
        String nome = row.naturezaAtoNome();
        if (nome == null || nome.isBlank()) {
            return null;
        }
        String limpo = nome.trim();
        if ("LOTAR SERVIDOR".equalsIgnoreCase(limpo)) {
            return "Lotação";
        }
        return capitalizarTexto(limpo);
    }

    private static String capitalizarTexto(String texto) {
        if (texto == null || texto.isBlank()) {
            return texto;
        }
        String[] partes = texto.toLowerCase().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < partes.length; i++) {
            String p = partes[i];
            if (p.isEmpty()) continue;
            if (i > 0 && (p.equals("de") || p.equals("da") || p.equals("do") || p.equals("dos") || p.equals("das")
                    || p.equals("e") || p.equals("em") || p.equals("para") || p.equals("por"))) {
                sb.append(p);
            } else {
                sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1));
            }
            if (i < partes.length - 1) {
                sb.append(" ");
            }
        }
        return sb.toString();
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (isNotBlank(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private static boolean isNotBlank(String value) {
        return value != null && !value.isBlank();
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static boolean isPublicacaoInicial(DesignacaoAtivaRow row) {
        return row.documentoInicial() || startsWithIni(row.atoNumero());
    }

    private static boolean startsWithIni(String value) {
        return value != null && value.trim().toUpperCase().startsWith("INI_");
    }
}
