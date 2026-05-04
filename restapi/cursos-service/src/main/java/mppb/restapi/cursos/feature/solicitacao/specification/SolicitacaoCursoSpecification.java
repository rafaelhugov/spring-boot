package mppb.restapi.cursos.feature.solicitacao.specification;

import mppb.restapi.cursos.feature.solicitacao.entity.SolicitacaoCurso;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import jakarta.persistence.criteria.Predicate;

import java.util.ArrayList;
import java.util.List;

@Component
public class SolicitacaoCursoSpecification {

    public Specification<SolicitacaoCurso> comFiltros(String search, Long statusId, Long minStatusId, Long usuarioId, Boolean isAdmin) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Filtro por nome do curso (search)
            if (StringUtils.hasText(search)) {
                predicates.add(builder.like(builder.lower(root.get("nomeCurso")), "%" + search.toLowerCase() + "%"));
            }

            // Filtro por status exato
            if (statusId != null) {
                predicates.add(builder.equal(root.get("status").get("id"), statusId));
            }

            // Filtro por status mínimo
            if (minStatusId != null) {
                predicates.add(builder.greaterThanOrEqualTo(root.get("status").get("id"), minStatusId));
            }

            // Regras de visibilidade (Authorization)
            if (Boolean.TRUE.equals(isAdmin)) {
                // Admin vê todas que NÃO são rascunho (status id >= 2) por padrão
                if (statusId == null && minStatusId == null) {
                    predicates.add(builder.greaterThanOrEqualTo(root.get("status").get("id"), 2L));
                }
                
                // Regra de Segurança Absoluta: Rascunhos (status == 1) SÓ podem ser vistos pelo próprio criador.
                // Portanto, cada linha retornada DEVE ser (status > 1) OU (idServidorInteressado == usuarioId)
                if (usuarioId != null) {
                    Predicate naoERascunho = builder.greaterThan(root.get("status").get("id"), 1L);
                    Predicate eMeuObjeto = builder.equal(root.get("idServidorInteressado"), usuarioId);
                    predicates.add(builder.or(naoERascunho, eMeuObjeto));
                } else {
                    // Sem ID de usuário, bloqueia acesso a qualquer rascunho
                    predicates.add(builder.greaterThan(root.get("status").get("id"), 1L));
                }
            } else if (usuarioId != null) {
                // Usuário comum vê apenas as suas
                predicates.add(builder.equal(root.get("idServidorInteressado"), usuarioId));
            }

            return builder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
