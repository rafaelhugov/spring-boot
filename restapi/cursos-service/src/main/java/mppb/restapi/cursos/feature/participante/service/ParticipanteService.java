package mppb.restapi.cursos.feature.participante.service;

import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import mppb.restapi.common.core.error.BusinessRuleException;
import mppb.restapi.common.core.error.NotFoundException;
import mppb.restapi.common.core.service.AbstractService;
import mppb.restapi.common.core.util.StringUtil;
import mppb.restapi.cursos.feature.common.projection.ServidorProjection;
import mppb.restapi.cursos.feature.common.service.ServidorService;
import mppb.restapi.cursos.feature.participante.converter.ParticipanteConverter;
import mppb.restapi.cursos.feature.participante.dto.CriarInscricaoDto;
import mppb.restapi.cursos.feature.participante.dto.InscricaoPublicaDto;
import mppb.restapi.cursos.feature.participante.dto.ParticipanteDto;
import mppb.restapi.cursos.feature.participante.dto.ParticipanteSearchResultDto;
import mppb.restapi.cursos.feature.participante.dto.ParticipanteVerificacaoDto;
import mppb.restapi.cursos.feature.participante.entity.Participante;
import mppb.restapi.cursos.feature.participante.enums.TipoParticipanteEnum;
import mppb.restapi.cursos.feature.participante.repository.ParticipanteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Transactional
// TODO revisar métodos dessa classe com a InscricaoService, podem ter regras repetidas ou misturadas
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
                dto.setEmail(servidor.getEmail());
            });
        }
        return dto;
    }

    /**
     * Encontra um participante existente ou cria um novo com base nos dados fornecidos.
     * Esta é a lógica central para evitar a duplicação de participantes.
     * (Usado pelo fluxo de Admin e Interno).
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
                    .orElseGet(() -> createAndSaveInterno(dto.getIdServidor())); // RENOMEADO
        } else { // EXTERNO
            // Esta lógica de 'participanteId' vs 'create' é usada pelo fluxo de Admin
            if (dto.getParticipanteId() != null) {
                return participanteRepository.findById(dto.getParticipanteId())
                        .orElseThrow(() -> new BusinessRuleException("Participante externo não encontrado: " + dto.getParticipanteId()));
            } else {
                return createAndSaveExterno(dto); // RENOMEADO
            }
        }
    }

    /**
     * Centraliza a lógica de criação de participante externo vinda do fluxo de inscrição pública.
     *
     * @param dto O DTO vindo do formulário de inscrição pública.
     * @return A entidade Participante (existente ou recém-criada).
     */
    public Participante findOrCreateParticipanteExterno(InscricaoPublicaDto dto) {
        // 1. Reutilizar participante se o ID foi fornecido
        if (dto.getParticipanteId() != null) {
            return this.findById(dto.getParticipanteId())
                    .orElseThrow(() -> new NotFoundException("Participante não encontrado para reutilização."));
        }

        // 2. Validar e converter o DTO
        if (!StringUtils.hasText(dto.getNome()) || !StringUtils.hasText(dto.getEmail())) {
            throw new BusinessRuleException("Nome e E-mail são obrigatórios para um novo cadastro.");
        }

        CriarInscricaoDto payload = new CriarInscricaoDto();
        payload.setTipo(TipoParticipanteEnum.EXTERNO.name());
        payload.setNome(dto.getNome());
        payload.setCpf(dto.getCpf().replaceAll("\\D", ""));
        payload.setEmail(dto.getEmail());
        payload.setTelefone(dto.getTelefone() != null ? dto.getTelefone().replaceAll("\\D", "") : null);

        // 3. Reutiliza a lógica de criação/validação de CPF duplicado
        return createAndSaveExterno(payload); 
    }

    private Participante createAndSaveInterno(Integer idServidor) {
        Participante p = new Participante();
        p.setIdServidor(idServidor);
        p.setTipo(TipoParticipanteEnum.INTERNO);
        return repository.save(p);
    }

    private Participante createAndSaveExterno(CriarInscricaoDto dto) {
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