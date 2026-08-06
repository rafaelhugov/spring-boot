package br.gov.mppb.ncap.feature.inspecao;

import jakarta.persistence.EntityManager;
import mppb.restapi.common.core.error.BusinessRuleException;
import mppb.restapi.common.core.service.AbstractService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;
import mppb.restapi.common.infrastructure.security.util.SecurityUtils;
import mppb.restapi.common.infrastructure.security.authorization.TokenAuthentication;
import mppb.restapi.common.infrastructure.client.authapi.dto.UsuarioAutorizadoResponseDto;
import br.gov.mppb.ncap.feature.entidade.Entidade;
import java.util.List;


@Service
@Transactional
public class InspecaoService extends AbstractService<Inspecao, Long> {

    private final InspecaoRepository repository;
    private final EntityManager entityManager;
    private final RespostaInspecaoRepository respostaRepository;
    private final br.gov.mppb.ncap.feature.formtemplate.FormTemplateRepository formTemplateRepository;
    private final br.gov.mppb.ncap.feature.formtemplate.converter.FormTemplateConverter formTemplateConverter;

    public InspecaoService(EntityManager entityManager, 
                           InspecaoRepository repository, 
                           RespostaInspecaoRepository respostaRepository,
                           br.gov.mppb.ncap.feature.formtemplate.FormTemplateRepository formTemplateRepository,
                           br.gov.mppb.ncap.feature.formtemplate.converter.FormTemplateConverter formTemplateConverter) {
        super(entityManager, repository);
        this.repository = repository;
        this.entityManager = entityManager;
        this.respostaRepository = respostaRepository;
        this.formTemplateRepository = formTemplateRepository;
        this.formTemplateConverter = formTemplateConverter;
    }


    public Page<Inspecao> findAll(String search, Pageable pageable) {
        if (search != null && !search.trim().isEmpty()) {
            return repository.findByEntidadeNomecnmpContainingIgnoreCase(search, pageable);
        }
        return repository.findAll(pageable);
    }

    public Page<Inspecao> findByPromotor(Long promotorId, Pageable pageable) {
        return repository.findByIdPromotorresponsavel(promotorId, pageable);
    }

    public Optional<Inspecao> findByToken(String token) {
        return repository.findByTokenacesso(token);
    }

    @Override
    @Transactional
    public Inspecao save(Inspecao entity) {
        boolean isNew = entity.getId() == null;

        if (isNew) {
            // RN04 — Impede duplicidade de agendamento por unidade/semestre
            validarDuplicidade(entity);

            // RN01 — Valida tipo de visita conforme semestre (Res. 279/2023)
            validarTipoVisitaPorSemestre(entity);

            if (entity.getStatus() == null) {
                entity.setStatus(InspecaoStatus.AGENDADA);
            }

            // Gera token UUID v4 seguro para acesso externo da delegacia
            if (entity.getTokenacesso() == null) {
                entity.setTokenacesso(UUID.randomUUID().toString());
                entity.setTokenexpiracao(Instant.now().plus(15, ChronoUnit.DAYS));
            }

            // Define o ID do servidor criador a partir do contexto de segurança
            if (entity.getIdServidorCriador() == null) {
                Long idCriador = SecurityUtils.getAuthenticationToken()
                        .map(TokenAuthentication::getUsuario)
                        .map(UsuarioAutorizadoResponseDto::getId)
                        .orElse(null);
                if (idCriador == null) {
                    // Fallback para testes ou modo insecure se ID for nulo
                    idCriador = 1L;
                }
                entity.setIdServidorCriador(idCriador);
            }

            // Dynamic Form Template Association
            if (entity.getEntidade() != null && entity.getEntidade().getId() != null) {
                Entidade entidade = entityManager.find(Entidade.class, entity.getEntidade().getId());
                entity.setEntidade(entidade);
                
                if (entidade != null && entidade.getTipoentidade() != null) {
                    try {
                        Long idTemplate = ((Number) entityManager.createNativeQuery(
                                "SELECT id_formtemplate FROM ncap.formtemplates " +
                                "WHERE tipoentidade = :tipo AND ativo = true " +
                                "ORDER BY criadoem DESC LIMIT 1")
                                .setParameter("tipo", entidade.getTipoentidade())
                                .getSingleResult()).longValue();
                        entity.setIdFormtemplate(idTemplate);
                    } catch (jakarta.persistence.NoResultException ignored) {
                        // Safe ignore if form templates aren't configured yet
                    }
                }
            }
        } else {
            // Em atualizações, revalida o tipo de visita se os dados relevantes foram alterados
            validarTipoVisitaPorSemestre(entity);
        }

        Inspecao salva = super.save(entity);

        if (isNew) {
            inicializarRespostasFormulario(salva.getId(), salva.getIdFormtemplate());

            registrarAuditoria(salva.getId(),
                "Inspeção criada e agendada para " + salva.getDataagendamento()
                + " | Semestre: " + salva.getSemestre()
                + " | Tipo: " + salva.getTipoVisita());
        }

        return salva;
    }

    @Transactional
    public Inspecao gerarNovoToken(Long id) {
        Inspecao inspecao = findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Inspeção não encontrada com ID: " + id));

        String tokenAntigo = inspecao.getTokenacesso();
        inspecao.setTokenacesso(UUID.randomUUID().toString());
        inspecao.setTokenexpiracao(Instant.now().plus(7, ChronoUnit.DAYS));

        Inspecao salva = super.save(inspecao);
        registrarAuditoria(salva.getId(),
            "Token de segurança renovado. Token anterior revogado: " + tokenAntigo);

        return salva;
    }

    @Transactional
    public Inspecao remarcar(Long id, LocalDate novaData) {
        Inspecao inspecao = findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Inspeção não encontrada com ID: " + id));

        LocalDate dataAntiga = inspecao.getDataagendamento();
        inspecao.setDataagendamento(novaData);

        // Validação da janela semestral (Res. 279/2023 CNMP)
        int mes = novaData.getMonthValue();
        String alertaSla = "";
        boolean foraDaJanela;

        if (inspecao.getSemestre().endsWith(".1")) {
            foraDaJanela = mes < 1 || mes > 4;
            if (foraDaJanela) alertaSla = " [ALERTA SLA: Fora da janela do 1º Semestre (Jan-Abr)]";
        } else {
            foraDaJanela = mes < 7 || mes > 10;
            if (foraDaJanela) alertaSla = " [ALERTA SLA: Fora da janela do 2º Semestre (Jul-Out)]";
        }

        Inspecao salva = super.save(inspecao);
        registrarAuditoria(salva.getId(),
            "Inspeção remarcada de " + dataAntiga + " para " + novaData + alertaSla);

        return salva;
    }

    // ------------------------------------------------------------------
    // Validações de Regras de Negócio (RN)
    // ------------------------------------------------------------------

    /**
     * RN04 — Impede criação de novo agendamento se já existir inspeção ativa
     * para a mesma unidade no mesmo semestre.
     */
    private void validarDuplicidade(Inspecao entity) {
        Long idEntidade = entity.getEntidade() != null ? entity.getEntidade().getId() : null;
        if (idEntidade == null || entity.getSemestre() == null) return;

        if (repository.existsInspecaoAtivaParaEntidadeESemestre(idEntidade, entity.getSemestre())) {
            throw new BusinessRuleException(
                "Já existe um agendamento de inspeção em andamento ou concluído para esta entidade " +
                "no semestre " + entity.getSemestre() + ".");
        }
    }

    /**
     * RN01 — Valida tipo de visita conforme semestre (Res. CNMP 279/2023):
     * <ul>
     *   <li>1º semestre (Jan–Abr): apenas PRESENCIAL permitido.</li>
     *   <li>2º semestre (Jul–Out): PRESENCIAL ou VIRTUAL; se VIRTUAL, justificativa é obrigatória.</li>
     * </ul>
     */
    private void validarTipoVisitaPorSemestre(Inspecao entity) {
        if (entity.getTipoVisita() == null || entity.getSemestre() == null) return;

        boolean isPrimeiroSemestre = entity.getSemestre().endsWith(".1");

        if (isPrimeiroSemestre && entity.getTipoVisita() == TipoVisita.VIRTUAL) {
            throw new BusinessRuleException(
                "Visitas do 1º semestre devem ser presenciais, conforme Resolução CNMP nº 279/2023.");
        }

        if (entity.getTipoVisita() == TipoVisita.VIRTUAL) {
            if (entity.getJustificativaRemota() == null) {
                throw new BusinessRuleException(
                    "Visitas virtuais exigem a seleção de uma justificativa, conforme Resolução CNMP nº 279/2023.");
            }
            if (entity.getJustificativaDetalhe() == null || entity.getJustificativaDetalhe().isBlank()) {
                throw new BusinessRuleException(
                    "Visitas virtuais exigem o detalhamento da justificativa, conforme Resolução CNMP nº 279/2023.");
            }
        }
    }

    // ------------------------------------------------------------------

    private void registrarAuditoria(Long idInspecao, String acao) {
        String usuario = "sistema";
        try {
            var auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getName() != null) {
                usuario = auth.getName();
            }
        } catch (Exception ignored) {}

        entityManager.createNativeQuery(
                "INSERT INTO ncap.auditoria (id_inspecao, usuario, acao) VALUES (:id, :usuario, :acao)")
                .setParameter("id", idInspecao)
                .setParameter("usuario", usuario)
                .setParameter("acao", acao)
                .executeUpdate();
    }

    private void inicializarRespostasFormulario(Long idInspecao, Long idTemplate) {
        if (idTemplate == null) return;

        try {
            @SuppressWarnings("unchecked")
            java.util.List<Number> questionIds = entityManager.createNativeQuery(
                    "SELECT p.id_pergunta FROM ncap.perguntas p " +
                    "JOIN ncap.blocos b ON p.id_bloco = b.id_bloco " +
                    "JOIN ncap.secoes s ON b.id_secao = s.id_secao " +
                    "WHERE s.id_template = :idTemplate")
                    .setParameter("idTemplate", idTemplate)
                    .getResultList();

            for (Number qId : questionIds) {
                entityManager.createNativeQuery(
                        "INSERT INTO ncap.respostasinspecao (id_inspecao, id_pergunta) " +
                        "VALUES (:idInspecao, :idPergunta)")
                        .setParameter("idInspecao", idInspecao)
                        .setParameter("idPergunta", qId.longValue())
                        .executeUpdate();
            }
        } catch (Exception ignored) {
            // Keep fail-safe
        }
    }

    public List<RespostaInspecao> obterRespostasInspecao(Long idInspecao) {
        if (!repository.existsById(idInspecao)) {
            throw new IllegalArgumentException("Inspeção não encontrada com ID: " + idInspecao);
        }
        return respostaRepository.findByInspecaoId(idInspecao);
    }

    @Transactional
    public List<RespostaInspecao> salvarRespostas(Long idInspecao, List<RespostaInspecao> respostas) {
        Inspecao inspecao = repository.findById(idInspecao)
                .orElseThrow(() -> new IllegalArgumentException("Inspeção não encontrada com ID: " + idInspecao));

        // Carrega o mapa de editabilidade das perguntas
        java.util.Map<Long, Boolean> perguntaIdToEditableMap = new java.util.HashMap<>();
        if (inspecao.getIdFormtemplate() != null) {
            formTemplateRepository.findById(inspecao.getIdFormtemplate()).ifPresent(template -> {
                br.gov.mppb.ncap.feature.formtemplate.dto.FormTemplateDto templateDto = formTemplateConverter.toDto(template);
                if (templateDto.getSecoes() != null) {
                    templateDto.getSecoes().forEach(secao -> {
                        if (secao.getBlocos() != null) {
                            secao.getBlocos().forEach(bloco -> {
                                if (bloco.getPerguntas() != null) {
                                    bloco.getPerguntas().forEach(pergunta -> {
                                        perguntaIdToEditableMap.put(pergunta.getId(), pergunta.getEditable());
                                    });
                                }
                            });
                        }
                    });
                }
            });
        }

        for (RespostaInspecao resp : respostas) {
            resp.setInspecao(inspecao);

            if (resp.getPergunta() != null) {
                Long perguntaId = resp.getPergunta().getId();
                Boolean editable = perguntaIdToEditableMap.get(perguntaId);

                // Se a pergunta for marcada como não editável, valida se houve tentativa de alteração
                if (editable != null && !editable) {
                    Optional<RespostaInspecao> existenteOpt = respostaRepository.findByInspecaoIdAndPerguntaId(idInspecao, perguntaId);
                    String databaseValue = existenteOpt.map(RespostaInspecao::getValorresposta).orElse(null);
                    String sentValue = resp.getValorresposta();

                    String normDb = (databaseValue == null || databaseValue.trim().isEmpty()) ? null : databaseValue.trim();
                    String normSent = (sentValue == null || sentValue.trim().isEmpty()) ? null : sentValue.trim();

                    if (!java.util.Objects.equals(normDb, normSent)) {
                        throw new BusinessRuleException(
                            "Não é permitido alterar a resposta da pergunta (ID: " + perguntaId + ") para o seu perfil de usuário."
                        );
                    }
                }

                if (resp.getId() == null) {
                    respostaRepository.findByInspecaoIdAndPerguntaId(idInspecao, perguntaId)
                            .ifPresent(existente -> resp.setId(existente.getId()));
                }
            }

            respostaRepository.save(resp);
        }

        registrarAuditoria(idInspecao, "Respostas do formulário salvas/atualizadas.");

        return respostaRepository.findByInspecaoId(idInspecao);
    }
}
