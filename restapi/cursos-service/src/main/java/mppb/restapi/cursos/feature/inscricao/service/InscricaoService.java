package mppb.restapi.cursos.feature.inscricao.service;

import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import mppb.restapi.common.core.error.BusinessRuleException;
import mppb.restapi.common.core.error.NotFoundException;
import mppb.restapi.common.core.service.AbstractService;
import mppb.restapi.common.core.util.StringUtil;
import mppb.restapi.common.infrastructure.security.authorization.TokenAuthentication;
import mppb.restapi.common.infrastructure.security.util.SecurityUtils;
import mppb.restapi.cursos.feature.common.projection.ServidorProjection;
import mppb.restapi.cursos.feature.common.service.EmailService;
import mppb.restapi.cursos.feature.common.service.ServidorService;
import mppb.restapi.cursos.feature.curso.entity.Curso;
import mppb.restapi.cursos.feature.curso.repository.CursoRepository;
import mppb.restapi.cursos.feature.curso.service.CursoService;
import mppb.restapi.cursos.feature.frequencia.service.FrequenciaService;
import mppb.restapi.cursos.feature.inscricao.converter.InscricaoConverter;
import mppb.restapi.cursos.feature.inscricao.dto.InscricaoAcessibilidadeDto;
import mppb.restapi.cursos.feature.inscricao.dto.InscricaoDto;
import mppb.restapi.cursos.feature.inscricao.entity.Inscricao;
import mppb.restapi.cursos.feature.inscricao.entity.InscricaoConfirmacaoToken;
import mppb.restapi.cursos.feature.inscricao.enums.SituacaoInscricaoEnum;
import mppb.restapi.cursos.feature.inscricao.repository.InscricaoConfirmacaoTokenRepository;
import mppb.restapi.cursos.feature.inscricao.repository.InscricaoRepository;
import mppb.restapi.cursos.feature.participante.dto.CriarInscricaoDto;
import mppb.restapi.cursos.feature.participante.dto.InscricaoPublicaDto;
import mppb.restapi.cursos.feature.participante.dto.ParticipanteDto;
import mppb.restapi.cursos.feature.participante.dto.ParticipanteVerificacaoDto;
import mppb.restapi.cursos.feature.participante.entity.Participante;
import mppb.restapi.cursos.feature.participante.enums.TipoParticipanteEnum;
import mppb.restapi.cursos.feature.participante.repository.ParticipanteRepository;
import mppb.restapi.cursos.feature.participante.service.ParticipanteService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import mppb.restapi.cursos.feature.inscricao.util.InscricaoExcelExporter;

@Service
@Slf4j
@Transactional
// TODO revisar métodos dessa classe com a ParticipanteService, podem ter regras repetidas ou misturadas
public class InscricaoService extends AbstractService<Inscricao, Long> {

    private final InscricaoRepository inscricaoRepository;
    private final CursoRepository cursoRepository;
    private final ParticipanteService participanteService;
    private final InscricaoConverter inscricaoConverter;
    private final InscricaoConfirmacaoTokenRepository tokenRepository;
    private final CursoService cursoService;
    private final ServidorService servidorService;
    private final ParticipanteRepository participanteRepository;
    private final EmailService emailService;
    private final FrequenciaService frequenciaService;

    public InscricaoService(EntityManager entityManager,
                            InscricaoRepository inscricaoRepository,
                            CursoRepository cursoRepository,
                            ParticipanteService participanteService,
                            InscricaoConverter inscricaoConverter,
                            InscricaoConfirmacaoTokenRepository tokenRepository,
                            CursoService cursoService,
                            ServidorService servidorService,
                            ParticipanteRepository participanteRepository,
                            EmailService emailService,
                            @Lazy FrequenciaService frequenciaService) {
        super(entityManager, inscricaoRepository);
        this.inscricaoRepository = inscricaoRepository;
        this.cursoRepository = cursoRepository;
        this.participanteService = participanteService;
        this.inscricaoConverter = inscricaoConverter;
        this.tokenRepository = tokenRepository;
        this.cursoService = cursoService;
        this.servidorService = servidorService;
        this.participanteRepository = participanteRepository;
        this.emailService = emailService;
        this.frequenciaService = frequenciaService;
    }

    // =================================================================================
    // Métodos Públicos (Orquestradores)
    // =================================================================================

    /**
     * Busca inscrições para o fluxo administrativo (paginado) com filtros.
     */
    @Transactional
    public Page<Inscricao> findAllByCursoId(Long cursoId, String nome, String situacao, Pageable pageable) {
        if (!cursoRepository.existsById(cursoId)) {
            throw new NotFoundException("Curso não encontrado: " + cursoId);
        }

        frequenciaService.consolidarFrequenciasPorCurso(cursoId);

        String nomeFiltro = null;
        if (nome != null && !nome.isBlank()) {
            nomeFiltro = "%" + java.text.Normalizer.normalize(nome, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .replace(" ", "%")
                .toLowerCase() + "%";
        }
        SituacaoInscricaoEnum situacaoEnum = null;
        if (situacao != null && !situacao.isBlank()) {
            try {
                // Tenta converter de código (char) ou nome da enum
                if (situacao.length() == 1) {
                    situacaoEnum = SituacaoInscricaoEnum.fromCodigo(situacao.charAt(0));
                } else {
                    situacaoEnum = SituacaoInscricaoEnum.valueOf(situacao);
                }
            } catch (Exception e) {
                log.warn("Situação inválida fornecida para filtro: {}", situacao);
            }
        }

        // Se o tamanho da página for negativo (Vuetify "Todos" envia -1), usa um valor alto para trazer todos
        if (pageable.isPaged() && pageable.getPageSize() < 0) {
            pageable = PageRequest.of(0, 10000, pageable.getSort());
        }

        Optional<Sort.Order> nomeSort = pageable.getSort().stream()
                .filter(order -> "participante.nome".equals(order.getProperty()))
                .findFirst();
        if (nomeSort.isPresent()) {
            Pageable pageableSemSort = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
            if (nomeSort.get().isDescending()) {
                return inscricaoRepository.findAllByCursoIdWithFiltersOrderByNomeDesc(cursoId, nomeFiltro, situacaoEnum, pageableSemSort);
            }
            return inscricaoRepository.findAllByCursoIdWithFiltersOrderByNomeAsc(cursoId, nomeFiltro, situacaoEnum, pageableSemSort);
        }

        return inscricaoRepository.findAllByCursoIdWithFilters(cursoId, nomeFiltro, situacaoEnum, pageable);
    }

    /**
     * Realiza uma inscrição para o fluxo administrativo (Admin) ou interno (via confirmação).
     * Define o status como INSCRITO imediatamente.
     */
    public InscricaoDto inscrever(Long cursoId, CriarInscricaoDto dto) {
        Curso curso = cursoRepository.findById(cursoId)
                .orElseThrow(() -> new NotFoundException("Curso não encontrado: " + cursoId));

        if (curso.getStatus() != mppb.restapi.cursos.feature.curso.enums.StatusCursoEnum.PUBLICADO) {
            throw new BusinessRuleException("Não é possível inscrever neste curso pois a carga horária de aulas está divergente ou o curso está em construção.");
        }

        validarPublicoAlvo(curso, dto);

        Participante participante = participanteService.findOrCreateParticipante(dto);

        validarInscricaoDuplicada(curso, participante);

        Inscricao novaInscricao = new Inscricao();
        novaInscricao.setCurso(curso);
        novaInscricao.setParticipante(participante);
        novaInscricao.setDataInscricao(LocalDateTime.now());
        novaInscricao.setSituacao(SituacaoInscricaoEnum.INSCRITO); // Efetivado imediatamente
        aplicarDadosAcessibilidade(novaInscricao, dto.getPcd(), dto.getNecessidadeEspecial());

        Inscricao inscricaoSalva = inscricaoRepository.save(novaInscricao);

        // Se o período de inscrição ainda estiver ativo, envia e-mail de confirmação para participante.
        if (cursoService.isPeriodoInscricaoValido(curso))
            emailService.enviarEmailInscricaoRealizada(inscricaoSalva);

        return toDto(inscricaoSalva);
    }

    /**
     * Orquestra a inscrição de um utilizador interno (servidor/membro) autenticado.
     */
    public InscricaoDto realizarInscricaoInterna(UUID hash, InscricaoAcessibilidadeDto dto) {
        // 1. Obtém o LOGIN do utilizador a partir do contexto de segurança
        TokenAuthentication auth = SecurityUtils.getAuthenticationToken()
                .orElseThrow(() -> new BusinessRuleException("Não foi possível identificar o utilizador autenticado."));

        String login = auth.getUsuario().getLogin();
        if (login == null || login.isBlank()) {
            throw new BusinessRuleException("O seu utilizador não possui um login associado.");
        }

        // 2. Usa o LOGIN para buscar o ID do SERVIDOR (RH)
        Integer idServidor = servidorService.findIdServidorByLogin(login)
                .orElseThrow(() -> new BusinessRuleException("Não foi possível encontrar um servidor no RH associado ao seu login."));

        // 3. Valida o curso
        Curso curso = getCursoByHashAndValidarPeriodo(hash);

        // 4. RN015 - Validação de Público-Alvo (a ser implementada)
        // validarPublicoAlvoInterno(idServidor, curso);

        // 5. Prepara o payload para criar/reutilizar o participante
        CriarInscricaoDto inscricaoPayload = new CriarInscricaoDto();
        inscricaoPayload.setTipo(TipoParticipanteEnum.INTERNO.name());
        inscricaoPayload.setIdServidor(idServidor); // Agora usa o ID correto do RH
        if (dto != null) {
            inscricaoPayload.setPcd(dto.getPcd());
            inscricaoPayload.setNecessidadeEspecial(dto.getNecessidadeEspecial());
        }

        // 6. Reutiliza o método 'inscrever' que já trata a duplicidade
        return inscrever(curso.getId(), inscricaoPayload);
    }

    /**
     * Orquestra a inscrição de um utilizador público (externo).
     * Define o status como INSCRITO.
     */
    public InscricaoDto realizarInscricaoPublica(UUID hash, InscricaoPublicaDto dto) {
        Curso curso = getCursoByHashAndValidarPeriodo(hash);

        validarPublicoAlvoExterno(curso);

        // Delega a lógica de criação/busca de participante
        Participante participante = participanteService.findOrCreateParticipanteExterno(dto);

        validarInscricaoDuplicada(curso, participante);

        // TODO: RN015 - Implementar a validação de público-alvo aqui

        Inscricao novaInscricao = new Inscricao();
        novaInscricao.setCurso(curso);
        novaInscricao.setParticipante(participante);
        novaInscricao.setDataInscricao(LocalDateTime.now());
        novaInscricao.setSituacao(SituacaoInscricaoEnum.INSCRITO); // Efetivado imediatamente
        aplicarDadosAcessibilidade(novaInscricao, dto.getPcd(), dto.getNecessidadeEspecial());

        Inscricao inscricaoSalva = inscricaoRepository.save(novaInscricao);

        emailService.enviarEmailInscricaoRealizada(inscricaoSalva);

        return toDto(inscricaoSalva);
    }

    /**
     * Verifica se um CPF pode se inscrever no fluxo público, validando
     * se é interno, se já está inscrito, ou se é um externo existente.
     */
    @Transactional(readOnly = true)
    public ParticipanteVerificacaoDto verificarParticipanteParaInscricao(UUID hash, String cpf) {
        Curso curso = getCursoByHashAndValidarPeriodo(hash);

        // 1. Verificar se já existe participante com este CPF.
        // Há registros legados que possuem o CPF cadastrado, mas não têm o tipo externo consistente.
        // A busca por CPF evita que a verificação diga "não encontrado" e o cadastro falhe logo depois por CPF duplicado.
        Optional<Participante> participanteOpt = participanteRepository.findByCpf(cpf);
        if (participanteOpt.isEmpty()) {
            Optional<ServidorProjection> servidor = servidorService.buscarPorCpf(cpf);
            if (servidor.isPresent()) {
                throw new BusinessRuleException("Este CPF pertence a um membro ou servidor. Por favor, utilize a aba 'Membros e Servidores'.");
            }

            throw new NotFoundException("Participante não encontrado. Prossiga com o cadastro.");
        }

        // 2. Se já existe como participante interno e ainda possui vínculo ativo no RH,
        // deve seguir o fluxo de membros/servidores. Sem vínculo ativo, trata como ex-interno
        // e permite a reutilização pelo fluxo externo.
        Participante participante = participanteOpt.get();
        boolean participanteInterno = participante.getIdServidor() != null || participante.getTipo() == TipoParticipanteEnum.INTERNO;
        if (participanteInterno && servidorService.buscarPorCpf(cpf).isPresent()) {
            throw new BusinessRuleException("Este CPF pertence a um membro ou servidor. Por favor, utilize a aba 'Membros e Servidores'.");
        }

        // 3. É um externo existente. Verificar se ele já está inscrito NESTE curso.
        if (inscricaoRepository.existsByCursoAndParticipante(curso, participante)) {
            throw new BusinessRuleException("Este participante já está inscrito neste curso.");
        }

        // 4. É um externo existente e NÃO está inscrito. Retorna os dados para reutilização.
        return new ParticipanteVerificacaoDto(
                participante.getId(),
                StringUtil.mascararEmail(participante.getEmail()) // Delega o mascaramento
        );
    }

    /**
     * Valida um token de confirmação legado e efetiva a inscrição (muda status para INSCRITO).
     */
    public InscricaoDto confirmarInscricao(String tokenStr) {
        InscricaoConfirmacaoToken token = tokenRepository.findByToken(tokenStr)
                .orElseThrow(() -> new BusinessRuleException("Este link de confirmação é inválido ou não existe."));

        if (token.getDataConfirmacao() != null) {
            throw new BusinessRuleException("Este link de confirmação já foi utilizado.");
        }

        if (token.getDataExpiracao().isBefore(LocalDateTime.now())) {
            throw new BusinessRuleException("Este link de confirmação expirou. Por favor, realize a inscrição novamente.");
        }

        token.setDataConfirmacao(LocalDateTime.now());

        Inscricao inscricao = token.getInscricao();
        inscricao.setSituacao(SituacaoInscricaoEnum.INSCRITO);

        emailService.enviarEmailInscricaoRealizada(inscricao);

        return toDto(inscricao);
    }

    /**
     * Atualiza a situação de uma inscrição (fluxo administrativo).
     */
    public InscricaoDto atualizarSituacao(Long cursoId, Long inscricaoId, String novaSituacaoCodigoStr, Integer frequencia) {
        Inscricao inscricao = inscricaoRepository.findByIdAndCursoId(inscricaoId, cursoId)
                .orElseThrow(() -> new NotFoundException("Inscrição não encontrada para este curso."));

        SituacaoInscricaoEnum situacaoAnterior = inscricao.getSituacao();

        try {
            Character codigo = novaSituacaoCodigoStr.charAt(0);
            SituacaoInscricaoEnum novaSituacao = SituacaoInscricaoEnum.fromCodigo(codigo);
            inscricao.setSituacao(novaSituacao);
            
            if (frequencia != null) {
                inscricao.setFrequencia(frequencia);
            }

            // Se a situação mudou para INSCRITO, envia e-mail de confirmação
            if (novaSituacao == SituacaoInscricaoEnum.INSCRITO && situacaoAnterior != SituacaoInscricaoEnum.INSCRITO) {
                emailService.enviarEmailInscricaoRealizada(inscricao);
            }

        } catch (Exception e) {
            throw new BusinessRuleException("Código de situação inválido: " + novaSituacaoCodigoStr);
        }

        return toDto(inscricao);
    }

    /**
     * Remove uma inscrição (fluxo administrativo).
     */
    public void remover(Long cursoId, Long inscricaoId) {
        if (!inscricaoRepository.existsByIdAndCursoId(inscricaoId, cursoId)) {
            throw new NotFoundException("Inscrição não encontrada para este curso.");
        }
        inscricaoRepository.deleteById(inscricaoId);
    }

    /**
     * Busca todas as inscrições do participante interno (membro/servidor) logado.
     * @return Lista de inscrições.
     */
    @Transactional(readOnly = true)
    public List<InscricaoDto> findMeusCursos() {
        // 1. Obtém o LOGIN do utilizador a partir do contexto de segurança
        TokenAuthentication auth = SecurityUtils.getAuthenticationToken()
                .orElseThrow(() -> new BusinessRuleException("Não foi possível identificar o utilizador autenticado."));

        String login = auth.getUsuario().getLogin();
        
        // 2. Identifica o ID do Servidor pelo Login
        Integer idServidor = servidorService.findIdServidorByLogin(login)
                .orElseThrow(() -> new BusinessRuleException("Não foi possível identificar seu cadastro de servidor para o login: " + login));

        // 3. Busca o CPF do servidor para garantir uma busca resiliente (fallback)
        String cpf = servidorService.buscarServidorPorId(idServidor)
                .map(mppb.restapi.cursos.feature.common.projection.ServidorProjection::getCpf)
                .map(c -> c.replaceAll("\\D", ""))
                .orElse(null);

        // 4. Busca as inscrições usando ID do Servidor ou CPF
        return inscricaoRepository.findByParticipanteIdServidorOrCpf(idServidor, cpf).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Gera um arquivo Excel com a lista de inscritos filtrada.
     */
    @Transactional
    public ByteArrayInputStream exportarParaExcel(Long cursoId, String nome, String situacao) throws IOException {
        // Busca sem paginação (Pageable.unpaged()) para exportar todos os registros filtrados
        Page<Inscricao> page = findAllByCursoId(cursoId, nome, situacao, Pageable.unpaged());
        
        List<InscricaoDto> dtos = page.getContent().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
                
        return InscricaoExcelExporter.exportToExcel(dtos);
    }

    // =================================================================================
    // Métodos Auxiliares (Privados)
    // =================================================================================

    /**
     * Converte uma entidade Inscricao para um DTO enriquecido.
     */
    @Transactional(readOnly = true)
    public InscricaoDto toDto(Inscricao inscricao) {
        InscricaoDto dto = inscricaoConverter.toDto(inscricao);
        ParticipanteDto enrichedParticipanteDto = participanteService.toEnrichedDto(inscricao.getParticipante());
        dto.setParticipante(enrichedParticipanteDto);
        return dto;
    }

    /**
     * Centraliza a busca do curso pelo hash E a validação do período de inscrição.
     */
    private Curso getCursoByHashAndValidarPeriodo(UUID hash) {
        Curso curso = cursoRepository.findByHashInscricao(hash)
                .orElseThrow(() -> new NotFoundException("Curso não encontrado ou o link de inscrição é inválido."));

        if (curso.getStatus() != mppb.restapi.cursos.feature.curso.enums.StatusCursoEnum.PUBLICADO) {
            throw new BusinessRuleException("Não é possível inscrever neste curso pois a carga horária de aulas está divergente ou o curso está em construção.");
        }

        if (!cursoService.isPeriodoInscricaoValido(curso)) {
            throw new BusinessRuleException("O período de inscrição para este curso não está ativo.");
        }
        return curso;
    }

    /**
     * Centraliza a verificação de inscrição duplicada.
     */
    private void validarInscricaoDuplicada(Curso curso, Participante participante) {
        if (inscricaoRepository.existsByCursoAndParticipante(curso, participante)) {
            throw new BusinessRuleException("Este participante já está inscrito neste curso.");
        }
    }

    private void aplicarDadosAcessibilidade(Inscricao inscricao, Boolean pcd, String necessidadeEspecial) {
        if (pcd == null) {
            throw new BusinessRuleException("Informe se o participante é Pessoa com Deficiência (PcD).");
        }

        if (Boolean.TRUE.equals(pcd) && (necessidadeEspecial == null || necessidadeEspecial.isBlank())) {
            throw new BusinessRuleException("Informe a necessidade especial ou recurso de acessibilidade necessário.");
        }

        inscricao.setPcd(pcd);
        inscricao.setNecessidadeEspecial(Boolean.TRUE.equals(pcd) ? necessidadeEspecial.trim() : null);
    }

    /**
     * Garante que o participante pertence a um dos públicos-alvo definidos para o curso.
     */
    private void validarPublicoAlvo(Curso curso, CriarInscricaoDto dto) {
        if (TipoParticipanteEnum.EXTERNO.name().equals(dto.getTipo())) {
            validarPublicoAlvoExterno(curso);
            return;
        }

        if (!TipoParticipanteEnum.INTERNO.name().equals(dto.getTipo())) {
            throw new BusinessRuleException("Tipo de participante inválido: " + dto.getTipo());
        }

        if (dto.getIdServidor() == null) {
            throw new BusinessRuleException("O ID do servidor é obrigatório para participantes internos.");
        }

        ServidorProjection servidor = servidorService.buscarServidorPorId(dto.getIdServidor())
                .orElseThrow(() -> new BusinessRuleException("Não foi possível localizar o cadastro do servidor no RH."));

        if (!"ATIVO".equalsIgnoreCase(servidor.getSituacao())) {
            throw new BusinessRuleException("Este participante não pode ser inscrito porque sua matrícula não está ativa.");
        }

        if (!servidorAtendePublicoAlvoInterno(curso, servidor)) {
            throw new BusinessRuleException("Este participante não pode ser inscrito porque não faz parte do público-alvo definido para o curso.");
        }
    }

    private void validarPublicoAlvoExterno(Curso curso) {
        boolean permiteExterno = curso.getPublicosAlvo() != null && curso.getPublicosAlvo().stream()
                .anyMatch(publico -> "E".equalsIgnoreCase(publico.getInternoExterno()));

        if (!permiteExterno) {
            throw new BusinessRuleException("Este participante não pode ser inscrito porque o curso não permite público externo.");
        }
    }

    private boolean servidorAtendePublicoAlvoInterno(Curso curso, ServidorProjection servidor) {
        if (curso.getPublicosAlvo() == null || curso.getPublicosAlvo().isEmpty()) {
            return false;
        }

        Set<String> publicosInternos = curso.getPublicosAlvo().stream()
                .filter(publico -> "I".equalsIgnoreCase(publico.getInternoExterno()))
                .map(publico -> normalizar(publico.getNome()))
                .collect(Collectors.toSet());

        if (publicosInternos.isEmpty()) {
            return false;
        }

        String tipoCargo = normalizar(servidor.getTipoCargoNome());
        String cargo = normalizar(servidor.getCargoNome());

        return (publicosInternos.contains("MEMBROS") && tipoCargo.contains("MEMBRO"))
                || (publicosInternos.contains("SERVIDORES") && tipoCargo.contains("SERVIDOR"))
                || (publicosInternos.contains("ASSESSORES") && cargo.contains("ASSESSOR"));
    }

    private String normalizar(String valor) {
        if (valor == null) {
            return "";
        }
        return Normalizer.normalize(valor, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .toUpperCase(Locale.ROOT)
                .trim();
    }

}
