package mppb.restapi.mobile.feature.notificacao.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import lombok.extern.slf4j.Slf4j;
import mppb.restapi.mobile.feature.notificacao.model.EventoFuncionalNotificacao;
import mppb.restapi.mobile.feature.notificacao.repository.PagamentoNotificacaoControleRepository;
import mppb.restapi.mobile.feature.pagamentos.dto.PagamentoItemDto;
import mppb.restapi.mobile.feature.pagamentos.repository.PagamentosRepository;

@Slf4j
@Service
public class PagamentoNotificacaoDeteccaoService extends NotificacaoDominioDeteccaoSupport {

    private static final String ROTA_PAGAMENTOS = "/pagamentos";
    private static final String EVENTO_PAGAMENTO = "NOVO_PAGAMENTO";

    private final PagamentosRepository pagamentosRepository;

    public PagamentoNotificacaoDeteccaoService(
            PagamentoNotificacaoControleRepository controleRepository,
            TransactionTemplate transactionTemplate,
            PagamentosRepository pagamentosRepository) {
        super(controleRepository, transactionTemplate);
        this.pagamentosRepository = pagamentosRepository;
    }

    public int detectarNovosPagamentos(String matricula) {
        return detectarDominio(matricula, eventosPagamentos(matricula));
    }

    private List<EventoFuncionalNotificacao> eventosPagamentos(String matricula) {
        try {
            String cpf = pagamentosRepository.getCpfByMatricula(matricula);
            if (cpf == null || cpf.isBlank()) {
                return List.of();
            }
            cpf = cpf.replaceAll("\\D", "");
            List<Integer> anos = pagamentosRepository.findAnosComPagamentos(cpf);
            if (anos.isEmpty()) {
                return List.of();
            }
            String exercicio = String.valueOf(anos.get(0));
            return pagamentosRepository.findPagamentosByExercicioAndCpf(exercicio, cpf).stream()
                    .map(pagamento -> eventoPagamento(matricula, pagamento))
                    .toList();
        } catch (RuntimeException e) {
            log.warn("Falha sanitizada ao detectar pagamentos: {}", e.getClass().getSimpleName());
            return List.of();
        }
    }

    private EventoFuncionalNotificacao eventoPagamento(String matricula, PagamentoItemDto pagamento) {
        String exercicio = textoOuPadrao(pagamento.getNuExercicio(), "0000");
        String documento = textoOuPadrao(pagamento.getDocumento(), "sem-documento");
        String data = textoOuPadrao(pagamento.getDtPagamento(), "sem-data").replaceAll("[^0-9]", "");
        String chave = "PAGAMENTO_%s_%s_%s_%s".formatted(matricula, exercicio, documento, data);
        return new EventoFuncionalNotificacao(
                chave,
                matricula,
                EVENTO_PAGAMENTO,
                ROTA_PAGAMENTOS,
                "Novo pagamento disponível",
                "Há um novo pagamento disponível no MPPB+.");
    }
}
