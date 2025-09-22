package br.gov.mppb.ceaf_backend.service;

import br.gov.mppb.ceaf_backend.service.dto.ServidorDTO;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ServidorService {

    @PersistenceContext
    private EntityManager entityManager;

    @SuppressWarnings("unchecked")
    public List<ServidorDTO> buscarServidoresPorNome(String nome) {
        // CONSULTA SQL ATUALIZADA CONFORME SOLICITADO
        String sql = "select distinct tipocargo_nome, servidor_id, servidor_matricula, servidor_nome, cargo_nome " +
                "from diariooficial.mapa_por_membro_vw " +
                "where lower(servidor_nome) like lower(:nome) limit 10";

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("nome", "%" + nome + "%");

        List<Object[]> resultados = query.getResultList();

        // MAPEAMENTO ATUALIZADO PARA A NOVA ORDEM DE COLUNAS
        return resultados.stream()
                .map(r -> new ServidorDTO(
                        (String) r[0],    // tipocargo_nome
                        (Integer) r[1],   // servidor_id
                        (String) r[2],    // servidor_matricula
                        (String) r[3],    // servidor_nome
                        (String) r[4]     // cargo_nome
                ))
                .collect(Collectors.toList());
    }


    public Optional<ServidorDTO> buscarServidorPorId(Integer id) {
        String sql = "select distinct tipocargo_nome, servidor_id, servidor_matricula, servidor_nome, cargo_nome " +
                "from diariooficial.mapa_por_membro_vw " +
                "where servidor_id = :id limit 1";

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("id", id);

        try {
            Object[] resultado = (Object[]) query.getSingleResult();
            ServidorDTO dto = new ServidorDTO((String) resultado[0], (Integer) resultado[1], (String) resultado[2], (String) resultado[3], (String) resultado[4]);
            return Optional.of(dto);
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }
}