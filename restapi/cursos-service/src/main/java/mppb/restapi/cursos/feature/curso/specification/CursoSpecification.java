package mppb.restapi.cursos.feature.curso.specification;

import mppb.restapi.cursos.feature.curso.entity.Curso;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import jakarta.persistence.criteria.Predicate;

import java.util.ArrayList;
import java.util.List;

@Component
public class CursoSpecification {

    public Specification<Curso> comFiltros(String titulo, Long modalidadeId, Long publicoAlvoId, String status, Boolean historico) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Adiciona filtro de modalidadeId se presente
            if (modalidadeId != null) {
                predicates.add(builder.equal(root.get("modalidade").get("id"), modalidadeId));
            }

            // Adiciona filtro de publicoAlvoId se presente
            if (publicoAlvoId != null) {
                // Para relações Many-to-Many, usamos join
                predicates.add(builder.equal(root.join("publicosAlvo").get("id"), publicoAlvoId));
            }

            // Adiciona filtro de título com ILIKE se presente
            if (StringUtils.hasText(titulo)) {
                predicates.add(builder.like(builder.lower(root.get("titulo")), "%" + titulo.toLowerCase() + "%"));
            }
            
            // Adiciona filtro de status se presente
            if (StringUtils.hasText(status)) {
                predicates.add(builder.equal(root.get("status"), mppb.restapi.cursos.feature.curso.enums.StatusCursoEnum.valueOf(status)));
            }

            // Adiciona filtro de histórico
            if (historico != null) {
                java.time.LocalDateTime now = java.time.LocalDateTime.now();
                
                Predicate isFinalizado = builder.equal(root.get("status"), mppb.restapi.cursos.feature.curso.enums.StatusCursoEnum.FINALIZADO);
                Predicate hasDataPassed = builder.and(
                        builder.isNotNull(root.get("realizacaoDataFinal")),
                        builder.lessThan(root.get("realizacaoDataFinal"), now)
                );
                Predicate isInHistorico = builder.or(isFinalizado, hasDataPassed);

                if (historico) {
                    // Histórico: STATUS = FINALIZADO OU Realização Final ultrapassada
                    predicates.add(isInHistorico);
                } else {
                    // Ativos: OPOSTO da condição do histórico
                    predicates.add(builder.not(isInHistorico));
                }
            }

            // Combina todos os predicates com "AND"
            return builder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
