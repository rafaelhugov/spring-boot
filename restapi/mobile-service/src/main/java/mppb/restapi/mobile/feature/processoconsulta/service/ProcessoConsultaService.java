package mppb.restapi.mobile.feature.processoconsulta.service;

import mppb.restapi.mobile.feature.processoconsulta.dto.ConsultaProcessoFilterDTO;
import mppb.restapi.mobile.feature.processoconsulta.dto.ConsultaProcessoResponseDTO;
import mppb.restapi.mobile.feature.processoconsulta.dto.ProcessoFiltroOptionDTO;
import mppb.restapi.mobile.feature.processoconsulta.dto.ProcessoFiltrosDTO;
import mppb.restapi.mobile.feature.processoconsulta.repository.ProtocoloConsultaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
public class ProcessoConsultaService {

    private static final Set<String> STATUS_VALIDOS = Set.of("T", "A", "I");
    private static final Set<String> TIPOS_ATIVIDADE_VALIDOS = Set.of("T", "R", "A");
    private static final int TAMANHO_MAXIMO_TEXTO = 100;

    @Autowired
    private ProtocoloConsultaRepository repository;

    public ProcessoFiltrosDTO listarFiltros() {
        return new ProcessoFiltrosDTO(
                repository.listarComarcas(),
                List.of(
                        new ProcessoFiltroOptionDTO("200910004", "Inquérito Civil"),
                        new ProcessoFiltroOptionDTO("200910002", "Notícia de Fato"),
                        new ProcessoFiltroOptionDTO("200910005", "Procedimento Administrativo"),
                        new ProcessoFiltroOptionDTO("200910003", "Procedimento Preparatório"),
                        new ProcessoFiltroOptionDTO("200000279", "Inquérito Policial"),
                        new ProcessoFiltroOptionDTO("200001733", "Procedimento Investigatório do MP"),
                        new ProcessoFiltroOptionDTO("-1", "Processo Judicial"),
                        new ProcessoFiltroOptionDTO("-2", "Administrativo")),
                List.of(
                        new ProcessoFiltroOptionDTO("T", "Termos de Ajustamento de Conduta firmados"),
                        new ProcessoFiltroOptionDTO("R", "Recomendações expedidas"),
                        new ProcessoFiltroOptionDTO("A", "Audiências Públicas realizadas")),
                List.of(
                        new ProcessoFiltroOptionDTO("T", "Todos"),
                        new ProcessoFiltroOptionDTO("A", "Ativo"),
                        new ProcessoFiltroOptionDTO("I", "Inativo")));
    }

    public List<ConsultaProcessoResponseDTO> consultarProcessos(ConsultaProcessoFilterDTO filtro) {
        validarENormalizar(filtro);
        if (!possuiFiltroMinimo(filtro)) {
            throw new IllegalArgumentException(
                    "Informe um número de processo, CPF, CNPJ ou nome com tamanho válido. "
                            + "Status, classe, comarca e tipo de atividade são apenas filtros complementares.");
        }
        return repository.consultarProcessos(filtro);
    }

    private boolean possuiFiltroMinimo(ConsultaProcessoFilterDTO filtro) {
        return preenchido(filtro.getNumeroProcesso())
                || preenchido(filtro.getNumeroOrigem())
                || preenchido(filtro.getNumeroCnmp())
                || preenchido(filtro.getCpf())
                || preenchido(filtro.getCnpj())
                || preenchido(filtro.getInteressado())
                || preenchido(filtro.getNomePromotor());
    }

    private void validarENormalizar(ConsultaProcessoFilterDTO filtro) {
        if (filtro == null) {
            throw new IllegalArgumentException("Filtros de pesquisa não informados.");
        }

        filtro.setNumeroProcesso(normalizar(filtro.getNumeroProcesso()));
        filtro.setNumeroOrigem(normalizar(filtro.getNumeroOrigem()));
        filtro.setNumeroCnmp(normalizar(filtro.getNumeroCnmp()));
        filtro.setCpf(somenteDigitos(filtro.getCpf()));
        filtro.setCnpj(somenteDigitos(filtro.getCnpj()));
        filtro.setInteressado(normalizar(filtro.getInteressado()));
        filtro.setNomePromotor(normalizar(filtro.getNomePromotor()));
        filtro.setStatus(normalizarMaiusculo(filtro.getStatus()));
        filtro.setIdTipoAtividade(normalizarMaiusculo(filtro.getIdTipoAtividade()));

        validarNumeroParcial("Número do processo", filtro.getNumeroProcesso());
        validarNumeroParcial("Número de origem", filtro.getNumeroOrigem());
        validarNumeroParcial("Número do CNMP", filtro.getNumeroCnmp());
        validarNome("Nome do interessado", filtro.getInteressado());
        validarNome("Nome do promotor", filtro.getNomePromotor());

        if (preenchido(filtro.getCpf()) && !documentoValido(filtro.getCpf(), 11)) {
            throw new IllegalArgumentException("CPF inválido.");
        }
        if (preenchido(filtro.getCnpj()) && !documentoValido(filtro.getCnpj(), 14)) {
            throw new IllegalArgumentException("CNPJ inválido.");
        }
        if (preenchido(filtro.getStatus()) && !STATUS_VALIDOS.contains(filtro.getStatus())) {
            throw new IllegalArgumentException("Status inválido.");
        }
        if (preenchido(filtro.getIdTipoAtividade())
                && !TIPOS_ATIVIDADE_VALIDOS.contains(filtro.getIdTipoAtividade())) {
            throw new IllegalArgumentException("Tipo de atividade inválido.");
        }
        if (filtro.getIdComarca() != null && filtro.getIdComarca() <= 0) {
            throw new IllegalArgumentException("Comarca inválida.");
        }
        if (filtro.getIdClasse() != null
                && filtro.getIdClasse() <= 0
                && filtro.getIdClasse() != -1L
                && filtro.getIdClasse() != -2L) {
            throw new IllegalArgumentException("Classe inválida.");
        }
    }

    private void validarNumeroParcial(String campo, String valor) {
        if (!preenchido(valor)) {
            return;
        }
        if (valor.length() > TAMANHO_MAXIMO_TEXTO || !valor.matches("[0-9./-]+")) {
            throw new IllegalArgumentException(campo + " inválido.");
        }
        if (somenteDigitos(valor).length() < 4) {
            throw new IllegalArgumentException(campo + " deve conter ao menos 4 dígitos.");
        }
    }

    private void validarNome(String campo, String valor) {
        if (!preenchido(valor)) {
            return;
        }
        long caracteresSignificativos = valor.codePoints().filter(Character::isLetterOrDigit).count();
        if (valor.length() > TAMANHO_MAXIMO_TEXTO || caracteresSignificativos < 3) {
            throw new IllegalArgumentException(campo + " deve conter entre 3 e 100 caracteres válidos.");
        }
    }

    private boolean documentoValido(String documento, int tamanho) {
        if (documento.length() != tamanho || documento.chars().distinct().count() == 1) {
            return false;
        }
        int[] pesosPrimeiro = tamanho == 11
                ? new int[]{10, 9, 8, 7, 6, 5, 4, 3, 2}
                : new int[]{5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
        int primeiro = digitoVerificador(documento, pesosPrimeiro);
        int[] pesosSegundo = tamanho == 11
                ? new int[]{11, 10, 9, 8, 7, 6, 5, 4, 3, 2}
                : new int[]{6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
        int segundo = digitoVerificador(documento.substring(0, tamanho - 2) + primeiro, pesosSegundo);
        return documento.endsWith("" + primeiro + segundo);
    }

    private int digitoVerificador(String base, int[] pesos) {
        int soma = 0;
        for (int i = 0; i < pesos.length; i++) {
            soma += Character.digit(base.charAt(i), 10) * pesos[i];
        }
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }

    private String somenteDigitos(String valor) {
        String normalizado = normalizar(valor);
        return normalizado == null ? null : normalizado.replaceAll("[^0-9]", "");
    }

    private String normalizar(String valor) {
        if (valor == null) {
            return null;
        }
        String normalizado = valor.trim();
        return normalizado.isEmpty() ? null : normalizado;
    }

    private String normalizarMaiusculo(String valor) {
        String normalizado = normalizar(valor);
        return normalizado == null ? null : normalizado.toUpperCase(java.util.Locale.ROOT);
    }

    private boolean preenchido(String valor) {
        return valor != null && !valor.isBlank();
    }
}
