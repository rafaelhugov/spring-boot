package mppb.restapi.cursos.feature.participante.service;

import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import mppb.restapi.common.core.error.BusinessRuleException;
import mppb.restapi.common.core.service.AbstractService;
import mppb.restapi.cursos.feature.common.projection.ServidorProjection;
import mppb.restapi.cursos.feature.common.service.ServidorService;
import mppb.restapi.cursos.feature.participante.converter.ParticipanteConverter;
import mppb.restapi.cursos.feature.participante.dto.CriarInscricaoDto;
import mppb.restapi.cursos.feature.participante.dto.ParticipanteDto;
import mppb.restapi.cursos.feature.participante.dto.ParticipanteSearchResultDto;
import mppb.restapi.cursos.feature.participante.entity.Participante;
import mppb.restapi.cursos.feature.participante.enums.TipoParticipanteEnum;
import mppb.restapi.cursos.feature.participante.repository.ParticipanteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Transactional
public class ParticipanteService extends AbstractService<Participante, Long> {

    private final ParticipanteRepository participanteRepository;
    private final ServidorService servidorService;
    private final ParticipanteConverter participanteConverter;

    public ParticipanteService(EntityManager entityManager,
                               ParticipanteRepository participanteRepository,
                               ServidorService servidorService,
                               ParticipanteConverter participanteConverter) {
        super(entityManager, participanteRepository);
        this.participanteRepository = participanteRepository;
        this.servidorService = servidorService;
        this.participanteConverter = participanteConverter;
    }


    /**
     * Busca por participantes mesclando resultados de servidores internos (RH)
     * e participantes externos já cadastrados na base de cursos.
     */
    @Transactional(readOnly = true)
    public List<ParticipanteSearchResultDto> searchParticipantes(String searchTerm) {
        List<ServidorProjection> servidores = servidorService.buscarServidoresPorNome(searchTerm);
        List<Participante> participantesExternos = participanteRepository
                .findAllByTipoAndNomeContainingIgnoreCase(TipoParticipanteEnum.EXTERNO, searchTerm);

        Stream<ParticipanteSearchResultDto> internosStream = servidores.stream().map(this::convertServidorToSearchResult);
        Stream<ParticipanteSearchResultDto> externosStream = participantesExternos.stream().map(this::convertParticipanteToSearchResult);

        return Stream.concat(internosStream, externosStream).collect(Collectors.toList());
    }

    /**
     * Converte uma entidade Participante para um DTO enriquecido, buscando dados
     * do serviço de RH para participantes internos.
     */
    @Transactional(readOnly = true)
    public ParticipanteDto toEnrichedDto(Participante participante) {
        ParticipanteDto dto = participanteConverter.toDto(participante);
        if (participante.getTipo() == TipoParticipanteEnum.INTERNO && participante.getIdServidor() != null) {
            servidorService.buscarServidorPorId(participante.getIdServidor()).ifPresent(servidor -> {
                dto.setNome(servidor.getNome());
                dto.setCargo(servidor.getTipoCargoNome());
            });
        }
        return dto;
    }

    /**
     * Encontra um participante existente ou cria um novo com base nos dados fornecidos.
     * Esta é a lógica central para evitar a duplicação de participantes.
     *
     * @param dto O DTO contendo os dados do participante a ser inscrito.
     * @return A entidade Participante (existente ou recém-criada).
     */
    public Participante findOrCreateParticipante(CriarInscricaoDto dto) {
        if (TipoParticipanteEnum.INTERNO.name().equals(dto.getTipo())) {
            if (dto.getIdServidor() == null) {
                throw new BusinessRuleException("O ID do servidor é obrigatório para participantes internos.");
            }
            return participanteRepository.findByIdServidor(dto.getIdServidor())
                    .orElseGet(() -> createAndSaveInternal(dto.getIdServidor()));
        } else { // EXTERNO
            if (dto.getParticipanteId() != null) {
                return participanteRepository.findById(dto.getParticipanteId())
                        .orElseThrow(() -> new BusinessRuleException("Participante externo não encontrado: " + dto.getParticipanteId()));
            } else {
                return createAndSaveExternal(dto);
            }
        }
    }

    private Participante createAndSaveInternal(Integer idServidor) {
        Participante p = new Participante();
        p.setIdServidor(idServidor);
        p.setTipo(TipoParticipanteEnum.INTERNO);
        return repository.save(p);
    }

    private Participante createAndSaveExternal(CriarInscricaoDto dto) {
        if (dto.getCpf() == null || dto.getCpf().isBlank()) {
            throw new BusinessRuleException("O CPF é obrigatório para participantes externos.");
        }
        if (participanteRepository.existsByCpf(dto.getCpf())) {
            throw new BusinessRuleException("Já existe um participante cadastrado com este CPF.");
        }
        Participante p = new Participante();
        p.setNome(dto.getNome());
        p.setCpf(dto.getCpf());
        p.setEmail(dto.getEmail());
        p.setTelefone(dto.getTelefone());
        p.setTipo(TipoParticipanteEnum.EXTERNO);
        return repository.save(p);
    }

    // --- Métodos de conversão para o DTO de busca ---

    private ParticipanteSearchResultDto convertServidorToSearchResult(ServidorProjection servidor) {
        return ParticipanteSearchResultDto.builder()
                .idServidor(servidor.getId())
                .nome(servidor.getNome())
                .cargo(servidor.getTipoCargoNome())
                .tipo(TipoParticipanteEnum.INTERNO.name())
                .servidorMatricula(servidor.getServidorMatricula())
                .situacao(servidor.getSituacao())
                .build();
    }

    private ParticipanteSearchResultDto convertParticipanteToSearchResult(Participante participante) {
        return ParticipanteSearchResultDto.builder()
                .participanteId(participante.getId())
                .nome(participante.getNome())
                .cpf(participante.getCpf())
                .email(participante.getEmail())
                .telefone(participante.getTelefone())
                .tipo(TipoParticipanteEnum.EXTERNO.name())
                .build();
    }
}
