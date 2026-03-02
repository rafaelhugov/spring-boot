package mppb.restapi.cursos.feature.lookup.specification;

import jakarta.persistence.criteria.Predicate;
import mppb.restapi.cursos.feature.cadastro.entities.EntidadeTutora;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Component
public class EntidadeTutoraSpecification {

    public Specification<EntidadeTutora> comFiltros(String nome) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Adiciona filtro de título com ILIKE se presente
            if (StringUtils.hasText(nome)) {
                predicates.add(builder.like(builder.lower(root.get("nome")), "%" + nome.toLowerCase() + "%"));
            }

            // Combina todos os predicates com "AND"
            return builder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
