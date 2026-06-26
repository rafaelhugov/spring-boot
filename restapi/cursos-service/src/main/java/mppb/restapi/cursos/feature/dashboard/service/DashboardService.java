package mppb.restapi.cursos.feature.dashboard.service;

import lombok.RequiredArgsConstructor;
import mppb.restapi.cursos.feature.curso.entity.Curso;
import mppb.restapi.cursos.feature.curso.enums.StatusCursoEnum;
import mppb.restapi.cursos.feature.curso.repository.CursoRepository;
import mppb.restapi.cursos.feature.dashboard.dto.DashboardStatsDto;
import mppb.restapi.cursos.feature.curso.dto.CursoDto;
import mppb.restapi.common.core.converter.DtoConverter;
import mppb.restapi.common.infrastructure.security.authorization.TokenAuthentication;
import mppb.restapi.common.infrastructure.security.util.SecurityUtils;
import mppb.restapi.cursos.feature.common.service.ServidorService;
import mppb.restapi.cursos.feature.inscricao.entity.Inscricao;
import mppb.restapi.cursos.feature.inscricao.enums.SituacaoInscricaoEnum;
import mppb.restapi.cursos.feature.inscricao.repository.InscricaoRepository;
import mppb.restapi.common.core.error.BusinessRuleException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final CursoRepository cursoRepository;
    private final DtoConverter<CursoDto, Curso> cursoConverter;
    private final InscricaoRepository inscricaoRepository;
    private final ServidorService servidorService;

    @Transactional(readOnly = true)
    public DashboardStatsDto getStats() {
        Optional<TokenAuthentication> authOpt = SecurityUtils.getAuthenticationToken();
        
        if (authOpt.isEmpty()) {
            return DashboardStatsDto.builder().build(); // Retorna vazio se não logado
        }
        
        String login = authOpt.get().getUsuario().getLogin();
        
        Integer idServidor = null;
        String cpf = null;
        try {
            idServidor = servidorService.findIdServidorByLogin(login).orElse(null);
            if (idServidor != null) {
                cpf = servidorService.buscarServidorPorId(idServidor)
                        .map(mppb.restapi.cursos.feature.common.projection.ServidorProjection::getCpf)
                        .map(c -> c.replaceAll("\\D", ""))
                        .orElse(null);
            }
        } catch (Exception e) {
            // Ignora erro caso não ache servidor para não quebrar o dashboard
        }
        
        List<Inscricao> minhasInscricoes = new ArrayList<>();
        if (idServidor != null || cpf != null) {
            minhasInscricoes = inscricaoRepository.findByParticipanteIdServidorOrCpf(idServidor, cpf);
        }

        LocalDateTime now = LocalDateTime.now();

        long minhasInscricoesTotal = minhasInscricoes.size();

        long cursosConcluidos = minhasInscricoes.stream()
            .filter(i -> i.getSituacao() == SituacaoInscricaoEnum.APROVADO)
            .count();

        long cursosEmAndamento = minhasInscricoes.stream().filter(i -> {
            boolean isInscrito = i.getSituacao() == SituacaoInscricaoEnum.INSCRITO || i.getSituacao() == SituacaoInscricaoEnum.APROVADO;
            Curso c = i.getCurso();
            boolean isRunning = c.getRealizacaoDataInicial() != null && c.getRealizacaoDataFinal() != null &&
                    !now.isBefore(c.getRealizacaoDataInicial()) && !now.isAfter(c.getRealizacaoDataFinal());
            return isInscrito && isRunning;
        }).count();

        long certificadosDisponiveis = cursosConcluidos;

        long horasCapacitacao = minhasInscricoes.stream()
                .filter(i -> i.getSituacao() == SituacaoInscricaoEnum.APROVADO)
                .mapToLong(i -> {
                    try {
                        return i.getCurso().getCargaHorariaMinutos() / 60;
                    } catch (Exception e) {
                        return 0;
                    }
                })
                .sum();

        long aguardandoInicio = minhasInscricoes.stream().filter(i -> {
            if (i.getSituacao() != SituacaoInscricaoEnum.INSCRITO) return false;
            Curso c = i.getCurso();
            return c.getRealizacaoDataInicial() != null && now.isBefore(c.getRealizacaoDataInicial());
        }).count();

        long taxaConclusao = minhasInscricoesTotal > 0 ? (cursosConcluidos * 100) / minhasInscricoesTotal : 0;

        long pendencias = minhasInscricoes.stream()
            .filter(i -> i.getSituacao() == SituacaoInscricaoEnum.PENDENTE_CONFIRMACAO)
            .count();

        // Calcular próximo curso (INSCRITO e data inicial futura)
        String proximoCursoNome = null;
        Optional<Inscricao> proximoCursoOpt = minhasInscricoes.stream()
            .filter(i -> i.getSituacao() == SituacaoInscricaoEnum.INSCRITO && i.getCurso().getRealizacaoDataInicial() != null && now.isBefore(i.getCurso().getRealizacaoDataInicial()))
            .min((i1, i2) -> i1.getCurso().getRealizacaoDataInicial().compareTo(i2.getCurso().getRealizacaoDataInicial()));
        
        if (proximoCursoOpt.isPresent()) {
            proximoCursoNome = proximoCursoOpt.get().getCurso().getTitulo();
        }

        // Construir Próximas Ações
        List<mppb.restapi.cursos.feature.dashboard.dto.ProximaAcaoDto> proximasAcoes = new ArrayList<>();
        
        minhasInscricoes.forEach(i -> {
            Curso c = i.getCurso();
            if (i.getSituacao() == SituacaoInscricaoEnum.PENDENTE_CONFIRMACAO) {
                proximasAcoes.add(new mppb.restapi.cursos.feature.dashboard.dto.ProximaAcaoDto(
                    c.getTitulo(), "Confirmação pendente", "WARNING"
                ));
            }
            if (i.getSituacao() == SituacaoInscricaoEnum.INSCRITO && c.getRealizacaoDataInicial() != null && now.isBefore(c.getRealizacaoDataInicial()) && now.plusDays(7).isAfter(c.getRealizacaoDataInicial())) {
                proximasAcoes.add(new mppb.restapi.cursos.feature.dashboard.dto.ProximaAcaoDto(
                    c.getTitulo(), "Início em breve", "INFO"
                ));
            }
            if (i.getSituacao() == SituacaoInscricaoEnum.APROVADO) {
                // Simplificação: apenas como exemplo de 'próximas ações'. Em um caso real,
                // talvez quiséssemos filtrar certificados recém emitidos não baixados.
                // Mas vamos deixar apenas pendentes e início em breve para não poluir.
            }
        });

        return DashboardStatsDto.builder()
                .minhasInscricoes(minhasInscricoesTotal)
                .cursosConcluidos(cursosConcluidos)
                .cursosEmAndamento(cursosEmAndamento)
                .certificadosDisponiveis(certificadosDisponiveis)
                .horasCapacitacao(horasCapacitacao)
                .aguardandoInicio(aguardandoInicio)
                .taxaConclusao(taxaConclusao)
                .proximoCurso(proximoCursoNome)
                .pendencias(pendencias)
                .proximasAcoes(proximasAcoes)
                .build();
    }

    @Transactional(readOnly = true)
    public Page<CursoDto> getCursos(Long modalidadeId, Long publicoAlvoId, String periodo, Pageable pageable) {

        Specification<Curso> spec = (root, query, cb) -> {

            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("status"), StatusCursoEnum.PUBLICADO));

            LocalDateTime now = LocalDateTime.now();

            // Regras base para um curso ser considerado "Disponível" na vitrine
            predicates.add(cb.isNotNull(root.get("inscricaoDataInicial")));
            predicates.add(cb.isNotNull(root.get("inscricaoDataFinal")));
            predicates.add(cb.greaterThanOrEqualTo(root.get("inscricaoDataFinal"), now));
            predicates.add(cb.or(
                    cb.isNull(root.get("realizacaoDataFinal")),
                    cb.greaterThanOrEqualTo(root.get("realizacaoDataFinal"), now)
            ));

            if (modalidadeId != null) {
                predicates.add(cb.equal(root.get("modalidade").get("id"), modalidadeId));
            }

            jakarta.persistence.criteria.Join<Curso, mppb.restapi.cursos.feature.lookup.entities.PublicoAlvo> publicosAlvoJoin = root.join("publicosAlvo", jakarta.persistence.criteria.JoinType.INNER);
            query.distinct(true);

            if (publicoAlvoId != null) {
                predicates.add(cb.equal(publicosAlvoJoin.get("id"), publicoAlvoId));
            }

            Optional<TokenAuthentication> authOpt = SecurityUtils.getAuthenticationToken();
            if (authOpt.isPresent() && authOpt.get().getAcessoData() != null && authOpt.get().getAcessoData().getPerfis() != null) {
                List<mppb.restapi.common.infrastructure.client.authapi.dto.PerfilAutorizadoResponseDto> perfis = authOpt.get().getAcessoData().getPerfis();
                boolean isAdmin = perfis.stream().anyMatch(p -> p.getNome() != null && p.getNome().toLowerCase().contains("administrador"));

                if (!isAdmin) {
                    boolean isMembro = perfis.stream().anyMatch(p -> p.getNome() != null && p.getNome().toLowerCase().contains("membro"));
                    boolean isServidor = perfis.stream().anyMatch(p -> p.getNome() != null && p.getNome().toLowerCase().contains("servidor"));
                    boolean isAssessor = perfis.stream().anyMatch(p -> p.getNome() != null && p.getNome().toLowerCase().contains("assessor"));

                    List<Predicate> publicoPredicates = new ArrayList<>();

                    if (isMembro) {
                        publicoPredicates.add(cb.like(cb.lower(publicosAlvoJoin.get("nome")), "%membro%"));
                    }
                    if (isServidor) {
                        publicoPredicates.add(cb.like(cb.lower(publicosAlvoJoin.get("nome")), "%servidor%"));
                    }
                    if (isAssessor) {
                        publicoPredicates.add(cb.like(cb.lower(publicosAlvoJoin.get("nome")), "%assessor%"));
                    }

                    // Sempre permitir ver os cursos direcionados ao público externo/geral
                    publicoPredicates.add(cb.like(cb.lower(publicosAlvoJoin.get("nome")), "%externo%"));
                    publicoPredicates.add(cb.like(cb.lower(publicosAlvoJoin.get("nome")), "%geral%"));
                    publicoPredicates.add(cb.like(cb.lower(publicosAlvoJoin.get("nome")), "%todos%"));

                    if (!publicoPredicates.isEmpty()) {
                        predicates.add(cb.or(publicoPredicates.toArray(new Predicate[0])));
                    }
                }
            } else {
                // Usuário não autenticado ou sem perfil mapeado, apenas cursos externos
                predicates.add(cb.or(
                    cb.like(cb.lower(publicosAlvoJoin.get("nome")), "%externo%"),
                    cb.like(cb.lower(publicosAlvoJoin.get("nome")), "%geral%"),
                    cb.like(cb.lower(publicosAlvoJoin.get("nome")), "%todos%")
                ));
            }
            
            // Filtros específicos de período aplicados de forma aditiva sobre as regras base
            if ("INSCRICOES_ABERTAS".equals(periodo)) {
                predicates.add(cb.lessThanOrEqualTo(root.get("inscricaoDataInicial"), now));
            } else if ("EM_ANDAMENTO".equals(periodo)) {
                predicates.add(cb.isNotNull(root.get("realizacaoDataInicial")));
                predicates.add(cb.lessThanOrEqualTo(root.get("realizacaoDataInicial"), now));
            } else if ("PROXIMOS".equals(periodo)) {
                predicates.add(cb.greaterThan(root.get("inscricaoDataInicial"), now));
            }

            query.orderBy(cb.asc(root.get("inscricaoDataInicial")));
            
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return cursoRepository.findAll(spec, pageable).map(cursoConverter::toDto);
    }
}
