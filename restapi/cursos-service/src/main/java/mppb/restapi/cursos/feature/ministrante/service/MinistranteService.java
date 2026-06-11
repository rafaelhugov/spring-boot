package mppb.restapi.cursos.feature.ministrante.service;

import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import mppb.restapi.common.core.error.BusinessRuleException;
import mppb.restapi.common.core.error.NotFoundException;
import mppb.restapi.common.core.service.AbstractService;
import mppb.restapi.cursos.feature.common.projection.ServidorProjection;
import mppb.restapi.cursos.feature.common.service.ServidorService;
import mppb.restapi.cursos.feature.curso.converter.CursoMinistranteConverter;
import mppb.restapi.cursos.feature.curso.dto.AssociarMinistranteDto;
import mppb.restapi.cursos.feature.curso.dto.CursoMinistranteDto;
import mppb.restapi.cursos.feature.curso.entity.CursoMinistrante;
import mppb.restapi.cursos.feature.ministrante.converter.MinistranteConverter;
import mppb.restapi.cursos.feature.ministrante.dto.MinistranteDto;
import mppb.restapi.cursos.feature.ministrante.dto.MinistranteSearchResultDto;
import mppb.restapi.cursos.feature.ministrante.entity.Ministrante;
import mppb.restapi.cursos.feature.ministrante.enums.TipoMinistranteEnum;
import mppb.restapi.cursos.feature.ministrante.repository.MinistranteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Slf4j
@Transactional
public class MinistranteService extends AbstractService<Ministrante, Long> {

    private final MinistranteRepository ministranteRepository;
    private final MinistranteConverter ministranteConverter;
    private final ServidorService servidorService;
    private final CursoMinistranteConverter cursoMinistranteConverter;

    public MinistranteService(EntityManager entityManager, MinistranteRepository ministranteRepository,
            MinistranteConverter ministranteConverter, ServidorService servidorService,
            CursoMinistranteConverter cursoMinistranteConverter) {
        super(entityManager, ministranteRepository);
        this.ministranteRepository = ministranteRepository;
        this.ministranteConverter = ministranteConverter;
        this.servidorService = servidorService;
        this.cursoMinistranteConverter = cursoMinistranteConverter;
    }

    @Transactional(readOnly = true)
    public List<MinistranteSearchResultDto> searchMinistrantes(String searchTerm) {
        // CORREÇÃO: Garante que ambas as fontes de dados são consultadas.
        List<ServidorProjection> servidores = servidorService.buscarServidoresPorNome(searchTerm);
        List<Ministrante> ministrantesExternos = ministranteRepository
                .findAllByTipoAndNomeContainingIgnoreCase(TipoMinistranteEnum.EXTERNO, searchTerm);

        // CORREÇÃO: Garante que os resultados são mesclados.
        Stream<MinistranteSearchResultDto> internosStream = servidores.stream()
                .map(this::convertServidorToSearchResult);
        Stream<MinistranteSearchResultDto> externosStream = ministrantesExternos.stream()
                .map(this::convertMinistranteToSearchResult);

        return Stream.concat(internosStream, externosStream).collect(Collectors.toList());
    }

    public Ministrante findOrCreateMinistrante(AssociarMinistranteDto dto) {

        Ministrante ministrante;

        if ("INTERNO".equals(dto.getTipo())) {
            if (dto.getIdServidor() == null) {
                throw new BusinessRuleException("O ID do servidor é obrigatório para ministrantes internos.");
            }
            ministrante = ministranteRepository.findByIdServidor(dto.getIdServidor())
                    .orElseGet(() -> buildInternal(dto.getIdServidor()));

        } else if ("EXTERNO".equals(dto.getTipo())) {
            String cleanCpf = dto.getCpf() != null ? dto.getCpf().replaceAll("\\D", "") : null;
            if (cleanCpf != null && cleanCpf.isBlank()) {
                cleanCpf = null;
            }
            
            if (dto.getMinistranteId() != null) {
                ministrante = ministranteRepository.findById(dto.getMinistranteId())
                        .orElseThrow(() -> new NotFoundException(
                                "Ministrante externo não encontrado: " + dto.getMinistranteId()));
            } else if (cleanCpf != null) {
                ministrante = ministranteRepository.findByCpf(cleanCpf)
                        .orElseGet(() -> buildExternal(dto));
            } else {
                ministrante = buildExternal(dto);
            }
        } else {
            throw new BusinessRuleException("Tipo de ministrante inválido: " + dto.getTipo());
        }

        if (dto.getIdFoto() != null) {
            ministrante.setIdFoto(dto.getIdFoto());
        }
        if (dto.getIdCurriculo() != null) {
            ministrante.setIdCurriculo(dto.getIdCurriculo());
        }

        if (dto.getEmail() != null) {
            ministrante.setEmail(dto.getEmail());
        }
        if (dto.getTelefone() != null) {
            ministrante.setTelefone(dto.getTelefone());
        }

        return ministranteRepository.save(ministrante);
    }

    @Transactional(readOnly = true)
    public CursoMinistranteDto toEnrichedDto(CursoMinistrante associacao) {
        MinistranteDto ministranteDto = toEnrichedDto(associacao.getMinistrante());
        CursoMinistranteDto dto = cursoMinistranteConverter.toDto(associacao);
        dto.setMinistrante(ministranteDto);
        return dto;
    }

    @Transactional(readOnly = true)
    public MinistranteDto toEnrichedDto(Ministrante ministrante) {
        MinistranteDto dto = ministranteConverter.toDto(ministrante);
        if (ministrante.getTipo() == TipoMinistranteEnum.INTERNO && ministrante.getIdServidor() != null) {
            servidorService.buscarServidorPorId(ministrante.getIdServidor()).ifPresent(servidor -> {
                dto.setNome(servidor.getNome());
                dto.setCargo(servidor.getTipoCargoNome());
            });
        }
        return dto;
    }


    private Ministrante buildInternal(Integer idServidor) {
        if (ministranteRepository.existsByIdServidor(idServidor)) {
            throw new BusinessRuleException("Já existe um ministrante cadastrado para este servidor.");
        }
        Ministrante ministrante = new Ministrante();
        ministrante.setIdServidor(idServidor);
        ministrante.setTipo(TipoMinistranteEnum.INTERNO);
        return ministrante;
    }


    private Ministrante buildExternal(AssociarMinistranteDto dto) {
        String cleanCpf = dto.getCpf() != null ? dto.getCpf().replaceAll("\\D", "") : null;
        if (cleanCpf != null && cleanCpf.isBlank()) {
            cleanCpf = null;
        }

        // A busca por CPF já foi feita no findOrCreateMinistrante via orElseGet,
        // portanto chegando aqui o CPF definitivamente não existe ainda.
        Ministrante ministrante = new Ministrante();
        ministrante.setNome(dto.getNome() != null ? dto.getNome().toUpperCase() : null);
        ministrante.setCpf(cleanCpf);
        ministrante.setNomeOrgaoExterno(dto.getNomeOrgaoExterno());
        ministrante.setTipo(TipoMinistranteEnum.EXTERNO);
        return ministrante;
    }

    private MinistranteSearchResultDto convertServidorToSearchResult(ServidorProjection servidor) {
        return MinistranteSearchResultDto.builder()
                .idServidor(servidor.getId())
                .nome(servidor.getNome())
                .cargo(servidor.getTipoCargoNome())
                .tipo("INTERNO")
                .servidorMatricula(servidor.getServidorMatricula())
                .situacao(servidor.getSituacao())
                .email(servidor.getEmail())
                .telefone(servidor.getTelefone())
                .build();
    }

    private MinistranteSearchResultDto convertMinistranteToSearchResult(Ministrante ministrante) {
        return MinistranteSearchResultDto.builder()
                .ministranteId(ministrante.getId())
                .nome(ministrante.getNome())
                .cpf(ministrante.getCpf())
                .nomeOrgaoExterno(ministrante.getNomeOrgaoExterno())
                .tipo("EXTERNO")
                .idFoto(ministrante.getIdFoto())
                .idCurriculo(ministrante.getIdCurriculo())
                .build();
    }
}
