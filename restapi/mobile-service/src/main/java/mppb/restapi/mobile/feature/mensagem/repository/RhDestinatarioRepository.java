package mppb.restapi.mobile.feature.mensagem.repository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class RhDestinatarioRepository {

    public record DestinatarioRh(
            String matricula,
            String nome,
            String setorNome,
            String cargoNome,
            String tipoCargo) {
    }

    public record OpcaoGrupoRh(
            String id,
            String nome,
            long quantidadeAtivos) {
    }

    private static final String BASE_SELECT = """
            select distinct on (s.matricula)
                s.matricula,
                coalesce(p.nome, s.matricula) as nome,
                st.nome as setor_nome,
                f.nome as cargo_nome,
                tc.tipo as tipo_cargo
            from servidor s
            left join pessoa p on p.id = s.id_pessoa and p.excluido = false
            left join recursoshumanos.funcao f on f.id = s.id_cargo
            left join recursoshumanos.tipocargo tc on tc.id = f.id_tipocargo
            left join setor st on st.id = s.id_setor and st.excluido = false
            left join orgao o on o.id_pessoa = st.id_orgao and o.excluido = false
            where s.excluido = false
              and s.ativo = 'A'
            """;

    private static final String ORDER_DISTINCT = """
            order by s.matricula, s.dataatualizacao desc nulls last, s.dataregistro desc nulls last, s.id desc
            """;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public List<DestinatarioRh> listarTodosAtivos() {
        String sql = BASE_SELECT + ORDER_DISTINCT;
        return jdbcTemplate.query(sql, (rs, rowNum) -> mapRow(rs));
    }

    public List<DestinatarioRh> listarPorCriterios(
            List<String> tiposCargo,
            List<Long> orgaoIds,
            List<Long> setorIds,
            List<Long> cargoIds,
            List<String> matriculas) {

        List<String> filtros = new ArrayList<>();
        MapSqlParameterSource params = new MapSqlParameterSource();

        if (tiposCargo != null && !tiposCargo.isEmpty()) {
            filtros.add("upper(tc.tipo) in (:tiposCargo)");
            params.addValue("tiposCargo", tiposCargo.stream().map(String::toUpperCase).toList());
        }
        if (orgaoIds != null && !orgaoIds.isEmpty()) {
            filtros.add("o.id_pessoa in (:orgaoIds)");
            params.addValue("orgaoIds", orgaoIds);
        }
        if (setorIds != null && !setorIds.isEmpty()) {
            filtros.add("st.id in (:setorIds)");
            params.addValue("setorIds", setorIds);
        }
        if (cargoIds != null && !cargoIds.isEmpty()) {
            filtros.add("f.id in (:cargoIds)");
            params.addValue("cargoIds", cargoIds);
        }
        if (matriculas != null && !matriculas.isEmpty()) {
            filtros.add("s.matricula in (:matriculas)");
            params.addValue("matriculas", matriculas);
        }

        if (filtros.isEmpty()) {
            return List.of();
        }

        String sql = BASE_SELECT + " and (" + String.join(" or ", filtros) + ") " + ORDER_DISTINCT;
        List<DestinatarioRh> encontrados = jdbcTemplate.query(sql, params, (rs, rowNum) -> mapRow(rs));

        Map<String, DestinatarioRh> deduplicados = new LinkedHashMap<>();
        for (DestinatarioRh item : encontrados) {
            deduplicados.putIfAbsent(item.matricula(), item);
        }
        return new ArrayList<>(deduplicados.values());
    }

    public List<DestinatarioRh> buscarPorTexto(String termo, int limite) {
        if (termo == null || termo.isBlank()) {
            return List.of();
        }
        String pattern = "%" + termo.trim().toLowerCase() + "%";
        String sql = """
                select * from (
                """ + BASE_SELECT + """
                  and (lower(coalesce(p.nome, '')) like :termo or lower(s.matricula) like :termo)
                """ + ORDER_DISTINCT + """
                ) sub
                order by sub.nome asc
                limit :limite
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("termo", pattern)
                .addValue("limite", Math.max(1, Math.min(limite, 50)));
        return jdbcTemplate.query(sql, params, (rs, rowNum) -> mapRow(rs));
    }

    public List<OpcaoGrupoRh> listarTiposCargo() {
        String sql = """
                select upper(tc.tipo) as id,
                       case when upper(tc.tipo) = 'M' then 'Membros' else 'Servidores' end as nome,
                       count(distinct s.matricula) as qtd
                from servidor s
                join recursoshumanos.funcao f on f.id = s.id_cargo
                join recursoshumanos.tipocargo tc on tc.id = f.id_tipocargo
                where s.excluido = false and s.ativo = 'A' and upper(tc.tipo) in ('M', 'S')
                group by upper(tc.tipo)
                order by nome asc
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> new OpcaoGrupoRh(
                rs.getString("id"), rs.getString("nome"), rs.getLong("qtd")));
    }

    public List<OpcaoGrupoRh> listarOrgaos() {
        String sql = """
                select cast(o.id_pessoa as varchar) as id,
                       coalesce(po.nome, o.sigla, 'Órgão') as nome,
                       count(distinct s.matricula) as qtd
                from servidor s
                join setor st on st.id = s.id_setor and st.excluido = false
                join orgao o on o.id_pessoa = st.id_orgao and o.excluido = false
                left join pessoa po on po.id = o.id_pessoa and po.excluido = false
                where s.excluido = false and s.ativo = 'A'
                group by o.id_pessoa, po.nome, o.sigla
                order by nome asc
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> new OpcaoGrupoRh(
                rs.getString("id"), rs.getString("nome"), rs.getLong("qtd")));
    }

    public List<OpcaoGrupoRh> listarSetores() {
        String sql = """
                select cast(st.id as varchar) as id,
                       st.nome as nome,
                       count(distinct s.matricula) as qtd
                from servidor s
                join setor st on st.id = s.id_setor and st.excluido = false
                where s.excluido = false and s.ativo = 'A'
                group by st.id, st.nome
                order by st.nome asc
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> new OpcaoGrupoRh(
                rs.getString("id"), rs.getString("nome"), rs.getLong("qtd")));
    }

    public List<OpcaoGrupoRh> listarCargos() {
        String sql = """
                select cast(f.id as varchar) as id,
                       f.nome as nome,
                       count(distinct s.matricula) as qtd
                from servidor s
                join recursoshumanos.funcao f on f.id = s.id_cargo
                where s.excluido = false and s.ativo = 'A'
                group by f.id, f.nome
                order by f.nome asc
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> new OpcaoGrupoRh(
                rs.getString("id"), rs.getString("nome"), rs.getLong("qtd")));
    }

    private DestinatarioRh mapRow(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new DestinatarioRh(
                rs.getString("matricula"),
                rs.getString("nome"),
                rs.getString("setor_nome"),
                rs.getString("cargo_nome"),
                rs.getString("tipo_cargo"));
    }
}

