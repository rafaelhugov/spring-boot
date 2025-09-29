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

    public Specification<Curso> comFiltros(String titulo, Long modalidadeId, Long publicoAlvoId) {
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

            // Combina todos os predicates com "AND"
            return builder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
