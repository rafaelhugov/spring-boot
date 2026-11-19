package mppb.restapi.mobile.feature.mensagem.service;

import java.util.List;
import java.util.Locale;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import mppb.restapi.mobile.feature.mensagem.dto.AcompanhamentoMensagemResponse;
import mppb.restapi.mobile.feature.mensagem.dto.DestinatarioStatusResponse;
import mppb.restapi.mobile.feature.mensagem.dto.MensagemEnviadaResponse;
import mppb.restapi.mobile.feature.mensagem.dto.MensagensEnviadasPageResponse;
import mppb.restapi.mobile.feature.mensagem.entity.MensagemAnexo;
import mppb.restapi.mobile.feature.mensagem.entity.MensagemAviso;
import mppb.restapi.mobile.feature.mensagem.entity.MensagemDestinatario;
import mppb.restapi.mobile.feature.mensagem.enums.StatusDestinatario;
import mppb.restapi.mobile.feature.mensagem.repository.MensagemAnexoRepository;
import mppb.restapi.mobile.feature.mensagem.repository.MensagemAvisoRepository;
import mppb.restapi.mobile.feature.mensagem.repository.MensagemDestinatarioRepository;

@Service
@RequiredArgsConstructor
public class MensagemAcompanhamentoService {

    private final MensagemAvisoRepository mensagemAvisoRepository;
    private final MensagemDestinatarioRepository mensagemDestinatarioRepository;
    private final MensagemAnexoRepository mensagemAnexoRepository;
    private final MensagemEnvioService mensagemEnvioService;

    @Transactional(readOnly = true)
    public MensagensEnviadasPageResponse listarEnviadas(boolean arquivada, int pagina, int tamanho) {
        Pageable pageable = PageRequest.of(Math.max(0, pagina), Math.max(1, Math.min(tamanho, 50)));
        Page<MensagemAviso> page = mensagemAvisoRepository
                .findByArquivadaRemetenteOrderByDataRegistroDesc(arquivada, pageable);

        List<MensagemEnviadaResponse> itens = page.getContent().stream()
                .map(aviso -> {
                    List<MensagemAnexo> anexos = mensagemAnexoRepository.findByMensagemIdOrderByIdAsc(aviso.getId());
                    return mensagemEnvioService.montarResumo(aviso, anexos);
                })
                .toList();

        return new MensagensEnviadasPageResponse(
                itens,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }

    @Transactional(readOnly = true)
    public AcompanhamentoMensagemResponse detalhar(
            Long id,
            StatusDestinatario status,
            String busca,
            int pagina,
            int tamanho) {

        MensagemAviso aviso = mensagemAvisoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Mensagem não encontrada."));

        List<MensagemAnexo> anexos = mensagemAnexoRepository.findByMensagemIdOrderByIdAsc(aviso.getId());
        MensagemEnviadaResponse resumo = mensagemEnvioService.montarResumo(aviso, anexos);

        String buscaPattern = (busca == null || busca.isBlank())
                ? null
                : "%" + busca.trim().toLowerCase(Locale.ROOT) + "%";

        Pageable pageable = PageRequest.of(Math.max(0, pagina), Math.max(1, Math.min(tamanho, 100)));
        Page<MensagemDestinatario> page = mensagemDestinatarioRepository
                .filtrarAcompanhamento(id, status, buscaPattern, pageable);

        List<DestinatarioStatusResponse> destinatarios = page.getContent().stream()
                .map(DestinatarioStatusResponse::from)
                .toList();

        return new AcompanhamentoMensagemResponse(
                resumo,
                destinatarios,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }
}
