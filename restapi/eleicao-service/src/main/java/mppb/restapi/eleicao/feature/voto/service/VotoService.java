package mppb.restapi.eleicao.feature.voto.service;

import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import mppb.restapi.common.core.error.BusinessRuleException;
import mppb.restapi.common.core.service.AbstractService;
import mppb.restapi.common.feature.autorizacao.entities.Usuario;
import mppb.restapi.eleicao.feature.candidato.entity.Candidato;
import mppb.restapi.eleicao.feature.candidato.repository.CandidatoRepository;
import mppb.restapi.eleicao.feature.eleicao.entity.Eleicao;
import mppb.restapi.eleicao.feature.eleicao.repository.EleicaoRepository;
import mppb.restapi.eleicao.feature.eleitor.entity.Eleitor;
import mppb.restapi.eleicao.feature.eleitor.repository.EleitorRepository;
import mppb.restapi.eleicao.feature.voto.entity.Voto;
import mppb.restapi.eleicao.feature.voto.repository.VotoRepository;
import org.springframework.data.domain.AuditorAware;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@Transactional
public class VotoService extends AbstractService<Voto, Long> {

    private final VotoRepository votoRepository;
    private final EleicaoRepository eleicaoRepository;
    private final CandidatoRepository candidatoRepository;
    private final EleitorRepository eleitorRepository;
    private final AuditorAware<Usuario> auditorAware;

    public VotoService(EntityManager entityManager, 
                       VotoRepository votoRepository, 
                       EleicaoRepository eleicaoRepository,
                       CandidatoRepository candidatoRepository,
                       EleitorRepository eleitorRepository,
                       AuditorAware<Usuario> auditorAware) {
        super(entityManager, votoRepository);
        this.votoRepository = votoRepository;
        this.eleicaoRepository = eleicaoRepository;
        this.candidatoRepository = candidatoRepository;
        this.eleitorRepository = eleitorRepository;
        this.auditorAware = auditorAware;
    }

    public Voto registrarVoto(Long eleicaoId, List<Long> candidatosIds, String tipoVoto, String ip) {
        Eleicao eleicao = eleicaoRepository.findById(eleicaoId)
                .orElseThrow(() -> new BusinessRuleException("Eleição não encontrada."));

        LocalDateTime agora = LocalDateTime.now();
        if (eleicao.getDataInicial() != null && agora.isBefore(eleicao.getDataInicial())) {
            throw new BusinessRuleException("A votação ainda não foi iniciada.");
        }
        if (eleicao.getDataFinal() != null && agora.isAfter(eleicao.getDataFinal())) {
            throw new BusinessRuleException("A votação já foi encerrada.");
        }

        // Obtém o usuário logado
        Usuario usuario = auditorAware.getCurrentAuditor()
                .orElseThrow(() -> new BusinessRuleException("Usuário não autenticado."));

        // Verifica se é um eleitor apto (usando login como chave de busca simplificada)
        // Nota: No sistema real, faria um join mais complexo ou usaria o id_servidor vinculado ao usuario
        Eleitor eleitor = eleitorRepository.findAll().stream() // Simplificação para o exemplo
                .filter(e -> e.getEleicao().getId().equals(eleicaoId) && e.getIdServidor().toString().equals(usuario.getMatricula()))
                .findFirst()
                .orElseThrow(() -> new BusinessRuleException("Você não está na lista de eleitores aptos para esta eleição."));

        if (Boolean.TRUE.equals(eleitor.getVotou())) {
            throw new BusinessRuleException("Seu voto já foi registrado para esta eleição.");
        }

        if (eleicao.getVotosPorEleitor() < candidatosIds.size()) {
            throw new BusinessRuleException("Número de candidatos excede o permitido (" + eleicao.getVotosPorEleitor() + ").");
        }

        Voto voto = new Voto();
        voto.setEleicao(eleicao);
        voto.setTipo(tipoVoto);
        voto.setIp(ip);

        if ("V".equals(tipoVoto)) {
            List<Candidato> candidatos = candidatosIds.stream()
                    .map(id -> candidatoRepository.findById(id)
                            .orElseThrow(() -> new BusinessRuleException("Candidato não encontrado: " + id)))
                    .collect(Collectors.toList());
            voto.setCandidatos(candidatos);
        }

        // Lógica de Hash (Simplificada para manter compatibilidade conceitual)
        voto.setHash(usuario.getLogin() + "|" + eleicaoId + "|" + agora);

        // Marca que o eleitor já votou
        eleitor.setVotou(true);
        eleitorRepository.save(eleitor);

        return votoRepository.save(voto);
    }
}
