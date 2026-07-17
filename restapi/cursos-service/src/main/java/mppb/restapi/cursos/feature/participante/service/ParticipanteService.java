package mppb.restapi.cursos.feature.participante.service;

import jakarta.persistence.EntityManager;
import mppb.restapi.common.core.error.BusinessRuleException;
import mppb.restapi.common.core.error.NotFoundException;
import mppb.restapi.common.core.service.AbstractService;
import mppb.restapi.cursos.feature.common.projection.ServidorProjection;
import mppb.restapi.cursos.feature.common.service.ServidorService;
import mppb.restapi.cursos.feature.participante.converter.ParticipanteConverter;
import mppb.restapi.cursos.feature.participante.dto.CriarInscricaoDto;
import mppb.restapi.cursos.feature.participante.dto.InscricaoPublicaDto;
import mppb.restapi.cursos.feature.participante.dto.ParticipanteDto;
import mppb.restapi.cursos.feature.participante.dto.ParticipanteSearchResultDto;
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

        List<ParticipanteSearchResultDto> internos = servidores.stream()
                .map(this::convertServidorToSearchResult)
                .collect(Collectors.toList());

        // Deduplicação: Remove da lista de externos quem já foi encontrado como interno (pelo nome)
        List<String> nomesInternos = internos.stream()
                .map(i -> i.getNome().toLowerCase())
                .collect(Collectors.toList());

        List<ParticipanteSearchResultDto> externos = participantesExternos.stream()
                .filter(ext -> !nomesInternos.contains(ext.getNome().toLowerCase()))
                .map(this::convertParticipanteToSearchResult)
                .collect(Collectors.toList());

        return Stream.concat(internos.stream(), externos.stream()).collect(Collectors.toList());
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
                dto.setCargo(servidor.getCargoNome()); // Nome real do cargo
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
        String cpfLimpo = dto.getCpf() != null ? dto.getCpf().replaceAll("\\D", "") : null;

        // 1. Reutilizar participante se o ID foi fornecido
        if (dto.getParticipanteId() != null) {
            Participante participante = this.findById(dto.getParticipanteId())
                    .orElseThrow(() -> new NotFoundException("Participante não encontrado para reutilização."));

            if (cpfLimpo != null && participante.getCpf() != null && !cpfLimpo.equals(participante.getCpf())) {
                throw new BusinessRuleException("O CPF informado não corresponde ao participante encontrado.");
            }

            return reutilizarParticipanteExternoExistente(participante, dto, cpfLimpo);
        }

        // 2. Validar e converter o DTO
        if (!StringUtils.hasText(dto.getNome()) || !StringUtils.hasText(dto.getEmail())) {
            throw new BusinessRuleException("Nome e E-mail são obrigatórios para um novo cadastro.");
        }

        if (!StringUtils.hasText(cpfLimpo)) {
            throw new BusinessRuleException("O CPF é obrigatório para participantes externos.");
        }

        Optional<Participante> participanteExistente = participanteRepository.findByCpf(cpfLimpo);
        if (participanteExistente.isPresent()) {
            return reutilizarParticipanteExternoExistente(participanteExistente.get(), dto, cpfLimpo);
        }

        CriarInscricaoDto payload = new CriarInscricaoDto();
        payload.setTipo(TipoParticipanteEnum.EXTERNO.name());
        payload.setNome(dto.getNome());
        payload.setCpf(cpfLimpo);
        payload.setEmail(dto.getEmail());
        payload.setTelefone(dto.getTelefone() != null ? dto.getTelefone().replaceAll("\\D", "") : null);

        // 3. Reutiliza a lógica de criação/validação de CPF duplicado
        return createAndSaveExterno(payload); 
    }

    private Participante createAndSaveInterno(Integer idServidor) {
        Participante p = new Participante();
        p.setIdServidor(idServidor);
        p.setTipo(TipoParticipanteEnum.INTERNO);

        // Denormalização: Busca dados do RH para permitir ordenação e busca local
        servidorService.buscarServidorPorId(idServidor).ifPresent(servidor -> {
            p.setNome(servidor.getNome());
            p.setCpf(servidor.getCpf() != null ? servidor.getCpf().replaceAll("\\D", "") : null);
            p.setEmail(servidor.getEmail());
        });

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

    private Participante reutilizarParticipanteExternoExistente(Participante participante, InscricaoPublicaDto dto, String cpfLimpo) {
        String cpfParaConsulta = StringUtils.hasText(cpfLimpo) ? cpfLimpo : participante.getCpf();
        boolean participanteInterno = participante.getIdServidor() != null || participante.getTipo() == TipoParticipanteEnum.INTERNO;

        if (participanteInterno && possuiVinculoAtivoNoRh(cpfParaConsulta)) {
            throw new BusinessRuleException("Este CPF pertence a um membro ou servidor. Por favor, utilize a aba 'Membros e Servidores'.");
        }

        if (participanteInterno) {
            participante.setIdServidor(null);
            participante.setTipo(TipoParticipanteEnum.EXTERNO);
        }

        if (participante.getTipo() == null) {
            participante.setTipo(TipoParticipanteEnum.EXTERNO);
        }

        if (!StringUtils.hasText(participante.getCpf())) {
            participante.setCpf(cpfLimpo);
        }

        if (!StringUtils.hasText(participante.getNome())) {
            participante.setNome(dto.getNome());
        }

        if (!StringUtils.hasText(participante.getEmail())) {
            participante.setEmail(dto.getEmail());
        }

        if (!StringUtils.hasText(participante.getTelefone()) && StringUtils.hasText(dto.getTelefone())) {
            participante.setTelefone(dto.getTelefone().replaceAll("\\D", ""));
        }

        return repository.save(participante);
    }

    private boolean possuiVinculoAtivoNoRh(String cpf) {
        return StringUtils.hasText(cpf) && servidorService.buscarPorCpf(cpf).isPresent();
    }

    /**
     * Busca de participante pelo CPF (Interno ou Externo).
     * @param cpf O CPF a ser procurado.
     * @return A entidade Participante encontrada.
     * @throws NotFoundException se o participante não for encontrado em nenhuma base.
     */
    @Transactional
    public Participante identificarParticipantePorCpf(String cpf) {
        String cpfLimpo = cpf.replaceAll("\\D", "");

        // 1. Tenta achar servidor interno primeiro
        Optional<ServidorProjection> servidor = servidorService.buscarPorCpf(cpfLimpo);
        if (servidor.isPresent()) {
            return participanteRepository.findByIdServidor(servidor.get().getId())
                    .orElseGet(() -> createAndSaveInterno(servidor.get().getId()));
        }

        // 2. Tenta achar externo
        return participanteRepository.findByCpfAndTipo(cpfLimpo, TipoParticipanteEnum.EXTERNO)
                .orElseThrow(() -> new NotFoundException("Participante não encontrado com este CPF."));
    }

    // --- Métodos de conversão para o DTO de busca ---

    private ParticipanteSearchResultDto convertServidorToSearchResult(ServidorProjection servidor) {
        return ParticipanteSearchResultDto.builder()
                .idServidor(servidor.getId())
                .nome(servidor.getNome())
                .email(servidor.getEmail())
                .telefone(servidor.getTelefone())
                .cargo(servidor.getCargoNome()) // Nome real do cargo (ex: Assessor Voluntário)
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
