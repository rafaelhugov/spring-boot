package mppb.restapi.cursos.feature.dashboard.service;

import lombok.RequiredArgsConstructor;
import mppb.restapi.cursos.feature.curso.entity.Curso;
import mppb.restapi.cursos.feature.curso.enums.StatusCursoEnum;
import mppb.restapi.cursos.feature.curso.repository.CursoRepository;
import mppb.restapi.cursos.feature.dashboard.dto.DashboardStatsDto;
import mppb.restapi.cursos.feature.curso.dto.CursoDto;
import mppb.restapi.common.core.converter.DtoConverter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final CursoRepository cursoRepository;
    private final DtoConverter<CursoDto, Curso> cursoConverter;

    @Transactional(readOnly = true)
    public DashboardStatsDto getStats() {
        LocalDateTime now = LocalDateTime.now();
        List<Curso> cursos = cursoRepository.findAll((root, query, cb) -> cb.equal(root.get("status"), StatusCursoEnum.PRONTO_PUBLICACAO));

        long abertos = cursos.stream().filter(c -> 
            c.getInscricaoDataInicial() != null && c.getInscricaoDataFinal() != null &&
            !now.isBefore(c.getInscricaoDataInicial()) && !now.isAfter(c.getInscricaoDataFinal())
        ).count();

        long emAndamento = cursos.stream().filter(c -> 
            c.getRealizacaoDataInicial() != null && c.getRealizacaoDataFinal() != null &&
            !now.isBefore(c.getRealizacaoDataInicial()) && !now.isAfter(c.getRealizacaoDataFinal())
        ).count();

        long vagas = 0; // TODO: Precisamos ver se há um campo de total de vagas no curso. A entidade Curso não possui vagas aparentemente.

        return DashboardStatsDto.builder()
                .cursosAbertos(abertos)
                .cursosEmAndamento(emAndamento)
                .vagasDisponiveis(vagas)
                .build();
    }

    @Transactional(readOnly = true)
    public Page<CursoDto> getCursos(Long modalidadeId, Long publicoAlvoId, String periodo, Pageable pageable) {

        Specification<Curso> spec = (root, query, cb) -> {

            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("status"), StatusCursoEnum.PRONTO_PUBLICACAO));

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
            if (publicoAlvoId != null) {
                predicates.add(cb.equal(root.join("publicosAlvo").get("id"), publicoAlvoId));
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
