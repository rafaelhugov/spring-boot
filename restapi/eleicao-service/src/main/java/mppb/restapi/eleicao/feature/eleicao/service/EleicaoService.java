package mppb.restapi.eleicao.feature.eleicao.service;

import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import mppb.restapi.common.core.error.BusinessRuleException;
import mppb.restapi.common.core.service.AbstractService;
import mppb.restapi.eleicao.feature.candidato.entity.Candidato;
import mppb.restapi.eleicao.feature.eleicao.dto.ResultadoDto;
import mppb.restapi.eleicao.feature.eleicao.dto.VotoContagemDto;
import mppb.restapi.eleicao.feature.eleicao.entity.Eleicao;
import mppb.restapi.eleicao.feature.eleicao.repository.EleicaoRepository;
import mppb.restapi.eleicao.feature.eleitor.repository.EleitorRepository;
import mppb.restapi.eleicao.feature.voto.entity.Voto;
import mppb.restapi.eleicao.feature.voto.repository.VotoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@Transactional
public class EleicaoService extends AbstractService<Eleicao, Long> {

    private final EleicaoRepository eleicaoRepository;
    private final VotoRepository votoRepository;
    private final EleitorRepository eleitorRepository;

    public EleicaoService(EntityManager entityManager, 
                          EleicaoRepository eleicaoRepository,
                          VotoRepository votoRepository,
                          EleitorRepository eleitorRepository) {
        super(entityManager, eleicaoRepository);
        this.eleicaoRepository = eleicaoRepository;
        this.votoRepository = votoRepository;
        this.eleitorRepository = eleitorRepository;
    }

    @Transactional
    public void encerrarVotacao(Long id) {
        Eleicao eleicao = findById(id)
                .orElseThrow(() -> new BusinessRuleException("Eleição não encontrada"));
        eleicao.setDataFinal(LocalDateTime.now().minusMinutes(1));
        save(eleicao);
    }

    public ResultadoDto calcularResultado(Long id) {
        Eleicao eleicao = findById(id)
                .orElseThrow(() -> new BusinessRuleException("Eleição não encontrada"));

        ResultadoDto resultado = new ResultadoDto();
        resultado.setIdEleicao(eleicao.getId());
        resultado.setTituloEleicao(eleicao.getTitulo());

        // Totais Gerais
        resultado.setTotalEleitoresAptos(eleitorRepository.countByEleicaoId(id));
        resultado.setTotalVotosComputados(votoRepository.countByEleicaoId(id));
        resultado.setTotalVotosBrancos(votoRepository.countByEleicaoIdAndTipo(id, "B"));
        resultado.setTotalVotosNulos(votoRepository.countByEleicaoIdAndTipo(id, "N"));
        
        long validos = votoRepository.countByEleicaoIdAndTipo(id, "V");
        resultado.setTotalVotosValidos(validos);
        resultado.setTotalVotosComputaveis(resultado.getTotalEleitoresAptos() * eleicao.getVotosPorEleitor());

        // Votos por Candidato
        List<Voto> todosVotosNominais = votoRepository.findByEleicaoIdAndTipo(id, "V");
        
        // Mapeia Candidato -> Quantidade de Votos
        Map<Candidato, Long> contagem = todosVotosNominais.stream()
                .flatMap(v -> v.getCandidatos().stream())
                .collect(Collectors.groupingBy(c -> c, Collectors.counting()));

        long totalVotosNominais = contagem.values().stream().mapToLong(Long::longValue).sum();
        resultado.setTotalVotosValidos(totalVotosNominais);

        List<VotoContagemDto> candidatosContagem = eleicao.getCandidatos().stream()
                .map(c -> {
                    Long votos = contagem.getOrDefault(c, 0L);
                    VotoContagemDto dto = new VotoContagemDto();
                    dto.setIdCandidato(c.getId());
                    dto.setNomeCandidato(c.getTitulo() != null ? c.getTitulo() : ""); 
                    dto.setTituloCandidato(c.getTitulo());
                    dto.setIdFoto(c.getIdFoto());
                    dto.setQuantidadeVotos(votos);
                    
                    // Cálculos de percentual
                    if (resultado.getTotalVotosComputados() > 0) {
                        dto.setPercentualTodos((votos.doubleValue() / resultado.getTotalVotosComputados()) * 100);
                    } else {
                        dto.setPercentualTodos(0.0);
                    }
                    
                    if (totalVotosNominais > 0) {
                        dto.setPercentualValidos((votos.doubleValue() / totalVotosNominais) * 100);
                    } else {
                        dto.setPercentualValidos(0.0);
                    }
                    
                    return dto;
                })
                .sorted((c1, c2) -> c2.getQuantidadeVotos().compareTo(c1.getQuantidadeVotos()))
                .collect(Collectors.toList());

        resultado.setCandidatos(candidatosContagem);

        return resultado;
    }
}
