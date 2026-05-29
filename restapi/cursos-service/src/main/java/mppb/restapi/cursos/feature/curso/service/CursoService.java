package mppb.restapi.cursos.feature.curso.service;

import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import mppb.restapi.common.core.error.BusinessRuleException;
import mppb.restapi.common.core.error.NotFoundException;
import mppb.restapi.common.core.service.AbstractService;
import mppb.restapi.cursos.feature.curso.converter.CursoConverter;
import mppb.restapi.cursos.feature.curso.converter.CursoMinistranteConverter;
import mppb.restapi.cursos.feature.curso.dto.*;
import mppb.restapi.cursos.feature.curso.entity.Curso;
import mppb.restapi.cursos.feature.curso.entity.CursoMinistrante;
import mppb.restapi.cursos.feature.curso.repository.CursoMinistranteRepository;
import mppb.restapi.cursos.feature.curso.repository.CursoRepository;
import mppb.restapi.cursos.feature.curso.specification.CursoSpecification;
import mppb.restapi.cursos.feature.cadastro.entities.EntidadeTutora;
import mppb.restapi.cursos.feature.lookup.entities.Modalidade;
import mppb.restapi.cursos.feature.lookup.entities.PapelMinistrante;
import mppb.restapi.cursos.feature.lookup.entities.PublicoAlvo;
import mppb.restapi.cursos.feature.lookup.entities.TipoEvento;
import mppb.restapi.cursos.feature.lookup.repository.PapelMinistranteRepository;
import mppb.restapi.cursos.feature.ministrante.entity.Ministrante;
import mppb.restapi.cursos.feature.ministrante.service.MinistranteService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;


@Service
@Slf4j
@Transactional
public class CursoService extends AbstractService<Curso, Long> {

    // Repositórios e Specifications
    private final CursoRepository cursoRepository;
    private final CursoMinistranteRepository cursoMinistranteRepository;
    private final PapelMinistranteRepository papelMinistranteRepository;
    private final CursoSpecification cursoSpecification;

    // Services
    private final MinistranteService ministranteService;

    // Converters
    private final CursoMinistranteConverter cursoMinistranteConverter;
    private final CursoConverter cursoConverter;


    public CursoService(EntityManager entityManager, CursoRepository cursoRepository, CursoMinistranteRepository cursoMinistranteRepository, PapelMinistranteRepository papelMinistranteRepository, CursoSpecification cursoSpecification, MinistranteService ministranteService, CursoMinistranteConverter cursoMinistranteConverter, CursoConverter cursoConverter) {
        super(entityManager, cursoRepository);
        this.cursoRepository = cursoRepository;
        this.cursoMinistranteRepository = cursoMinistranteRepository;
        this.papelMinistranteRepository = papelMinistranteRepository;
        this.cursoSpecification = cursoSpecification;
        this.ministranteService = ministranteService;
        this.cursoMinistranteConverter = cursoMinistranteConverter;
        this.cursoConverter = cursoConverter; // Injetando o novo converter
    }

    /**
     * Busca cursos usando filtros e uma paginação com ordenação padrão
     */
    public Page<Curso> findAll(String titulo, Long modalidadeId, Long publicoAlvoId, String status, Pageable pageable) {
        Specification<Curso> filterSpec = cursoSpecification.comFiltros(titulo, modalidadeId, publicoAlvoId, status);
        Pageable pageableComOrdenacao = criarPageableComOrdenacaoPadrao(pageable);

        return cursoRepository.findAll(filterSpec, pageableComOrdenacao);
    }

    /**
     * Cria um novo objeto Pageable que inclui a regra de negócio de ordenação padrão.
     * @param pageable O Pageable original recebido na requisição.
     * @return Um novo Pageable com a ordenação aplicada.
     */
    private Pageable criarPageableComOrdenacaoPadrao(Pageable pageable) {
        Sort defaultSort = Sort.by(
                Sort.Order.desc("dataModificacao"),
                Sort.Order.desc("dataCriacao")
        );

        Sort finalSort = pageable.getSort().isSorted()
                ? pageable.getSort().and(defaultSort)
                : defaultSort;

        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), finalSort);
    }

    /**
     * Busca um curso por seu ID e retorna um DTO enriquecido com a lista de ministrantes.
     * Este método substitui o uso direto de findById(id) no controller quando os detalhes completos são necessários.
     *
     * @param cursoId O ID do curso a ser buscado.
     * @return Um CursoDto completo com todos os detalhes, incluindo ministrantes.
     * @throws NotFoundException se o curso não for encontrado.
     */
    @Transactional(readOnly = true)
    public CursoDto findDetalhesById(Long cursoId) {
        // 1. Busca a entidade principal (Curso)
        Curso curso = cursoRepository.findById(cursoId)
                .orElseThrow(() -> new NotFoundException("Curso não encontrado com o id: " + cursoId));

        // 2. Busca a lista de ministrantes associados de forma explícita
        List<CursoMinistranteDto> ministrantesDto = findAllMinistrantesByCursoId(cursoId);

        // 3. Converte a entidade Curso para o seu DTO principal
        CursoDto cursoDto = cursoConverter.toDto(curso);

        // 4. Popula a lista de ministrantes no DTO
        cursoDto.setMinistrantes(ministrantesDto);

        return cursoDto;
    }

    @Override
    public Curso save(Curso curso) {
        if (curso.getHashInscricao() == null) {
            curso.setHashInscricao(UUID.randomUUID());
        }
        // A lógica de gerenciamento de ministrantes foi removida daqui, pois agora é tratada em endpoints específicos.
        return cursoRepository.saveAndFlush(curso);
    }

    public CursoDto update(Long cursoId, CursoDto dto) {
        Curso curso = cursoRepository.findById(cursoId)
                .orElseThrow(() -> new NotFoundException("Curso não encontrado com o id: " + cursoId));

        curso.setTitulo(dto.getTitulo());
        curso.setDescricao(dto.getDescricao());
        curso.setInscricaoDataInicial(dto.getInscricaoDataInicial());
        curso.setInscricaoDataFinal(dto.getInscricaoDataFinal());
        curso.setRealizacaoDataInicial(dto.getRealizacaoDataInicial());
        curso.setRealizacaoDataFinal(dto.getRealizacaoDataFinal());
        curso.setCargaHoraria(dto.getCargaHoraria());
        curso.setPercentualChAprovacao(dto.getPercentualChAprovacao());
        curso.setDetalhaCargaHoraria(dto.getDetalhaCargaHoraria());
        curso.setLinkProgramacao(dto.getLinkProgramacao());
        curso.setProgressaoFuncional(dto.getProgressaoFuncional());

        curso.setTipoEvento(getRequiredReference(TipoEvento.class, dto.getTipoEvento().getId(), "Tipo de evento"));
        curso.setEntidadeTutora(getRequiredReference(EntidadeTutora.class, dto.getEntidadeTutora().getId(), "Entidade tutora"));
        curso.setModalidade(getRequiredReference(Modalidade.class, dto.getModalidade().getId(), "Modalidade"));
        curso.setPublicosAlvo(toPublicosAlvoReferences(dto));

        Curso saved = cursoRepository.saveAndFlush(curso);
        return cursoConverter.toDto(saved);
    }

    private Set<PublicoAlvo> toPublicosAlvoReferences(CursoDto dto) {
        if (dto.getPublicosAlvo() == null || dto.getPublicosAlvo().isEmpty()) {
            throw new BusinessRuleException("Ao menos um público-alvo deve ser selecionado.");
        }

        return dto.getPublicosAlvo().stream()
                .map(publicoAlvo -> getRequiredReference(PublicoAlvo.class, publicoAlvo.getId(), "Público-alvo"))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private <T> T getRequiredReference(Class<T> entityClass, Long id, String fieldName) {
        if (Objects.isNull(id)) {
            throw new BusinessRuleException(fieldName + " é obrigatório.");
        }
        return entityManager.getReference(entityClass, id);
    }

    @Override
    @Transactional
    public void deleteById(Long cursoId) {
        // 1. Valida se o curso existe antes de prosseguir.
        // Isso também garante que não estamos tentando operar com um ID inválido.
        if (!cursoRepository.existsById(cursoId)) {
            throw new NotFoundException("Não é possível deletar. Curso não encontrado com o id: " + cursoId);
        }

        // 2. Busca todas as associações de ministrantes para este curso.
        List<CursoMinistrante> associacoes = cursoMinistranteRepository.findByCursoId(cursoId);

        // 3. Se existirem associações, deleta todas elas.
        // Isso "limpa" as referências na tabela cursoministrante.
        if (!associacoes.isEmpty()) {
            cursoMinistranteRepository.deleteAll(associacoes);
        }

        // 4. Agora que as dependências foram removidas, deleta o curso principal.
        // Esta chamada é do repositório base do AbstractService.
        super.deleteById(cursoId);
    }


    // --- MÉTODOS PARA GESTÃO DE MINISTRANTES ---

    @Transactional(readOnly = true)
    public List<CursoMinistranteDto> findAllMinistrantesByCursoId(Long cursoId) {
        if (!cursoRepository.existsById(cursoId)) {
            throw new NotFoundException("Curso não encontrado: " + cursoId);
        }

        List<CursoMinistrante> associacoes = cursoMinistranteRepository.findByCursoId(cursoId);

        return associacoes.stream()
                .map(associacao -> ministranteService.toEnrichedDto(associacao))
                .collect(Collectors.toList());
    }

    public CursoMinistranteDto addMinistranteToCurso(Long cursoId, AssociarMinistranteDto dto) {
        Curso curso = cursoRepository.findById(cursoId)
                .orElseThrow(() -> new NotFoundException("Curso não encontrado: " + cursoId));

        // --- LÓGICA DO PAPEL REINTEGRADA ---
        PapelMinistrante papel = papelMinistranteRepository.findById(dto.getPapelId())
                .orElseThrow(() -> new NotFoundException("Papel de ministrante não encontrado: " + dto.getPapelId()));

        Ministrante ministranteParaAssociar = ministranteService.findOrCreateMinistrante(dto);

        if (cursoMinistranteRepository.existsByCursoAndMinistrante(curso, ministranteParaAssociar)) {
            throw new BusinessRuleException("Este ministrante já está associado a este curso.");
        }

        CursoMinistrante novaAssociacao = new CursoMinistrante();
        novaAssociacao.setCurso(curso);
        novaAssociacao.setMinistrante(ministranteParaAssociar);
        novaAssociacao.setPapel(papel); // O papel agora é associado corretamente

        CursoMinistrante associacaoSalva = cursoMinistranteRepository.save(novaAssociacao);

        return ministranteService.toEnrichedDto(associacaoSalva);
    }

    public void removeMinistranteFromCurso(Long cursoId, Long associacaoId) {
        CursoMinistrante associacao = cursoMinistranteRepository.findByIdAndCursoId(associacaoId, cursoId)
                .orElseThrow(() -> new NotFoundException("Associação de ministrante não encontrada para este curso."));

        cursoMinistranteRepository.delete(associacao);
    }


    /**
     * Atualiza o papel de um ministrante em uma associação existente com um curso.
     *
     * @param cursoId O ID do curso, para garantir que a associação pertence ao curso correto.
     * @param associacaoId O ID da associação (CursoMinistrante) a ser atualizada.
     * @param dto O DTO contendo o ID do novo papel a ser atribuído.
     * @return O DTO da associação atualizada e enriquecida.
     * @throws NotFoundException se o curso, a associação ou o novo papel não forem encontrados.
     */
    public CursoMinistranteDto atualizarPapelMinistrante(Long cursoId, Long associacaoId, AtualizarPapelMinistranteDto dto) {
        // 1. Busca a associação, validando se ela pertence ao curso informado para garantir a integridade.
        CursoMinistrante associacao = cursoMinistranteRepository.findByIdAndCursoId(associacaoId, cursoId)
                .orElseThrow(() -> new NotFoundException(
                        String.format("Associação de ministrante com ID %d não encontrada para o curso com ID %d.", associacaoId, cursoId)
                ));

        // 2. Busca a nova entidade de Papel.
        PapelMinistrante novoPapel = papelMinistranteRepository.findById(dto.getPapelId())
                .orElseThrow(() -> new NotFoundException("Papel de ministrante não encontrado: " + dto.getPapelId()));

        // 3. Atualiza o papel na associação.
        associacao.setPapel(novoPapel);

        // 4. A persistência é gerenciada pela transação (@Transactional), então um save() explícito não é necessário
        //    para a atualização. O Hibernate detecta a mudança e a salva no banco de dados.
        //    Apenas retornamos o DTO da entidade atualizada em memória.
        return ministranteService.toEnrichedDto(associacao);
    }


    /**
     * Busca os detalhes públicos de um curso pelo seu hash de inscrição.
     * @param hash O UUID do hash de inscrição.
     * @return Um DTO com os detalhes públicos do curso.
     */
    @Transactional(readOnly = true)
    public CursoPublicoDto findByHashInscricao(UUID hash) {
        Curso curso = cursoRepository.findByHashInscricao(hash)
                .orElseThrow(() -> new NotFoundException("Curso não encontrado ou o link de inscrição é inválido."));

        // Mapeamento manual para o DTO público
        CursoPublicoDto dto = new CursoPublicoDto();
        dto.setTitulo(curso.getTitulo());
        dto.setDescricao(curso.getDescricao());
        dto.setInscricaoDataInicial(curso.getInscricaoDataInicial());
        dto.setInscricaoDataFinal(curso.getInscricaoDataFinal());
        dto.setRealizacaoDataInicial(curso.getRealizacaoDataInicial());
        dto.setRealizacaoDataFinal(curso.getRealizacaoDataFinal());
        dto.setCargaHoraria(curso.getCargaHoraria());
        dto.setLinkProgramacao(curso.getLinkProgramacao());
        if (curso.getModalidade() != null) {
            dto.setModalidade(curso.getModalidade().getDescricao());
        }

        // AJUSTE: O campo internoExterno foi removido para seguir o padrão de produção.
        // TODO: Definir nova regra para identificar se um curso aceita inscrições externas baseado no público-alvo.
        boolean aceitaExterno = true;
        dto.setAceitaInscricaoExterna(aceitaExterno);

        return dto;
    }


    @Transactional(readOnly = true)
    public boolean isPeriodoInscricaoValido(Curso curso) {
        if (curso.getInscricaoDataInicial() == null || curso.getInscricaoDataFinal() == null) {
            // Se não há datas definidas, consideramos o período como inválido para inscrição.
            return false;
        }
        LocalDateTime agora = LocalDateTime.now();
        return !agora.isBefore(curso.getInscricaoDataInicial()) && !agora.isAfter(curso.getInscricaoDataFinal());
    }

    // TODO Método placeholder para implementação futura
    @Transactional(readOnly = true)
    public boolean isPublicoAlvoValido(String email, Curso curso) {
        // LÓGICA FUTURA:
        // 1. Verificar se o curso tem um público-alvo restrito (Membros/Servidores).
        // 2. Se sim, consultar a base do RH com o e-mail/CPF para validar o vínculo.
        // 3. Retornar true ou false com base na validação.
        System.out.println("Validando público-alvo para: " + email); // Log de placeholder
        return true; // Por enquanto, sempre retorna válido.
    }
}
