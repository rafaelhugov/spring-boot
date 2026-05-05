package mppb.restapi.cursos.feature.inscricao.service;

import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import mppb.restapi.common.core.error.BusinessRuleException;
import mppb.restapi.common.core.error.NotFoundException;
import mppb.restapi.common.core.service.AbstractService;
import mppb.restapi.common.core.util.StringUtil;
import mppb.restapi.common.feature.configuracao.service.ConfiguracaoService;
import mppb.restapi.common.infrastructure.security.authorization.TokenAuthentication;
import mppb.restapi.common.infrastructure.security.util.SecurityUtils;
import mppb.restapi.cursos.feature.common.projection.ServidorProjection;
import mppb.restapi.cursos.feature.common.service.EmailService;
import mppb.restapi.cursos.feature.common.service.ServidorService;
import mppb.restapi.cursos.feature.curso.entity.Curso;
import mppb.restapi.cursos.feature.curso.repository.CursoRepository;
import mppb.restapi.cursos.feature.curso.service.CursoService;
import mppb.restapi.cursos.feature.inscricao.converter.InscricaoConverter;
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
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

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
    private final ConfiguracaoService configuracaoService;

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
                            ConfiguracaoService configuracaoService) {
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
        this.configuracaoService = configuracaoService;
    }

    // =================================================================================
    // Métodos Públicos (Orquestradores)
    // =================================================================================

    /**
     * Busca inscrições para o fluxo administrativo (paginado) com filtros.
     */
    @Transactional(readOnly = true)
    public Page<Inscricao> findAllByCursoId(Long cursoId, String nome, String situacao, Pageable pageable) {
        if (!cursoRepository.existsById(cursoId)) {
            throw new NotFoundException("Curso não encontrado: " + cursoId);
        }

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

        return inscricaoRepository.findAllByCursoIdWithFilters(cursoId, nomeFiltro, situacaoEnum, pageable);
    }

    /**
     * Realiza uma inscrição para o fluxo administrativo (Admin) ou interno (via confirmação).
     * Define o status como INSCRITO imediatamente.
     */
    public InscricaoDto inscrever(Long cursoId, CriarInscricaoDto dto) {
        Curso curso = cursoRepository.findById(cursoId)
                .orElseThrow(() -> new NotFoundException("Curso não encontrado: " + cursoId));

        Participante participante = participanteService.findOrCreateParticipante(dto);

        validarInscricaoDuplicada(curso, participante);

        Inscricao novaInscricao = new Inscricao();
        novaInscricao.setCurso(curso);
        novaInscricao.setParticipante(participante);
        novaInscricao.setDataInscricao(LocalDateTime.now());
        novaInscricao.setSituacao(SituacaoInscricaoEnum.INSCRITO); // Efetivado imediatamente

        Inscricao inscricaoSalva = inscricaoRepository.save(novaInscricao);

        // Se o período de inscrição ainda estiver ativo, envia e-mail de confirmação para participante.
        if (cursoService.isPeriodoInscricaoValido(curso))
            emailService.enviarEmailInscricaoRealizada(inscricaoSalva);

        return toDto(inscricaoSalva);
    }

    /**
     * Orquestra a inscrição de um utilizador interno (servidor/membro) autenticado.
     */
    public InscricaoDto realizarInscricaoInterna(UUID hash) {
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

        // 6. Reutiliza o método 'inscrever' que já trata a duplicidade
        return inscrever(curso.getId(), inscricaoPayload);
    }

    /**
     * Orquestra a inscrição de um utilizador público (externo).
     * Define o status como PENDENTE_CONFIRMACAO.
     */
    public InscricaoDto realizarInscricaoPublica(UUID hash, InscricaoPublicaDto dto) {
        Curso curso = getCursoByHashAndValidarPeriodo(hash);

        // Delega a lógica de criação/busca de participante
        Participante participante = participanteService.findOrCreateParticipanteExterno(dto);

        // --- LÓGICA DE REENVIO ---
        Optional<Inscricao> inscricaoExistenteOpt = inscricaoRepository.findByCursoAndParticipante(curso, participante);

        if (inscricaoExistenteOpt.isPresent()) {
            Inscricao inscricao = inscricaoExistenteOpt.get();

            // Se a inscrição estiver PENDENTE, tratamos como um pedido de REENVIO.
            if (inscricao.getSituacao() == SituacaoInscricaoEnum.PENDENTE_CONFIRMACAO) {
                log.info("Inscrição pendente (ID: {}) encontrada. Reenviando e-mail de confirmação.", inscricao.getId());
                gerarEEnviarEmailConfirmacao(inscricao); // Reenvia o e-mail
                return toDto(inscricao); // Retorna sucesso
            } else {
                // Se for INSCRITO, APROVADO, etc., então é um erro de duplicidade.
                throw new BusinessRuleException("Este participante já está inscrito neste curso.");
            }
        }
        // --- FIM DA LÓGICA DE REENVIO ---

        // TODO: RN015 - Implementar a validação de público-alvo aqui

        Inscricao inscricaoSalva = criarInscricaoPendente(curso, participante);
        gerarEEnviarEmailConfirmacao(inscricaoSalva);

        return toDto(inscricaoSalva);
    }

    /**
     * Verifica se um CPF pode se inscrever no fluxo público, validando
     * se é interno, se já está inscrito, ou se é um externo existente.
     */
    @Transactional(readOnly = true)
    public ParticipanteVerificacaoDto verificarParticipanteParaInscricao(UUID hash, String cpf) {
        Curso curso = getCursoByHashAndValidarPeriodo(hash);

        // 1. (RN017) Verificar se o CPF é de um servidor interno
        Optional<ServidorProjection> servidor = servidorService.buscarPorCpf(cpf);
        if (servidor.isPresent()) {
            throw new BusinessRuleException("Este CPF pertence a um membro ou servidor. Por favor, utilize a aba 'Membros e Servidores'.");
        }

        // 2. Verificar se é um participante externo existente
        Optional<Participante> participanteOpt = participanteRepository.findByCpfAndTipo(cpf, TipoParticipanteEnum.EXTERNO);
        if (participanteOpt.isEmpty()) {
            throw new NotFoundException("Participante não encontrado. Prossiga com o cadastro.");
        }

        // 3. É um externo existente. Verificar se ele já está inscrito NESTE curso.
        Participante participante = participanteOpt.get();
        Optional<Inscricao> inscricaoExistente = inscricaoRepository.findByCursoAndParticipante(curso, participante);
        if (inscricaoExistente.isPresent()) {
            Inscricao inscricao = inscricaoExistente.get();
            // Se a inscrição estiver PENDENTE, não é um erro. O fluxo deve continuar
            // para que o utilizador possa receber um novo e-mail (ao submeter).
            if (inscricao.getSituacao() != SituacaoInscricaoEnum.PENDENTE_CONFIRMACAO) {
                throw new BusinessRuleException("Este participante já está inscrito neste curso.");
            }
            // Se estiver PENDENTE, o fluxo continua e retorna o DTO de verificação
        }

        // 4. É um externo existente e NÃO está inscrito. Retorna os dados para reutilização.
        return new ParticipanteVerificacaoDto(
                participante.getId(),
                StringUtil.mascararEmail(participante.getEmail()) // Delega o mascaramento
        );
    }

    /**
     * Valida um token de confirmação e efetiva a inscrição (muda status para INSCRITO).
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

    /**
     * Cria e persiste uma nova inscrição com o status PENDENTE_CONFIRMACAO.
     */
    private Inscricao criarInscricaoPendente(Curso curso, Participante participante) {
        Inscricao novaInscricao = new Inscricao();
        novaInscricao.setCurso(curso);
        novaInscricao.setParticipante(participante);
        novaInscricao.setDataInscricao(LocalDateTime.now());
        novaInscricao.setSituacao(SituacaoInscricaoEnum.PENDENTE_CONFIRMACAO);
        return inscricaoRepository.save(novaInscricao);
    }

    /**
     * Gera, salva e envia o token de confirmação por e-mail.
     */
    private void gerarEEnviarEmailConfirmacao(Inscricao inscricao) {
        // 1. Apaga tokens antigos para esta inscrição
        tokenRepository.findByInscricaoId(inscricao.getId()).ifPresent(tokenAntigo -> {
            tokenRepository.delete(tokenAntigo);
            log.info("Token antigo da inscrição {} foi invalidado.", inscricao.getId());
        });

        // 2. Cria o novo token
        Integer validadeMinutos = configuracaoService.getAsInteger("cursos.inscricao.token-validade-minutos");
        InscricaoConfirmacaoToken token = new InscricaoConfirmacaoToken(inscricao, validadeMinutos);
        tokenRepository.save(token);

        // 3. Envia o e-mail
        emailService.enviarEmailConfirmacao(inscricao, token);
    }
}