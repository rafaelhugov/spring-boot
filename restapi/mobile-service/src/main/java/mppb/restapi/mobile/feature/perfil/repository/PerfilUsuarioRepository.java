package mppb.restapi.mobile.feature.perfil.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;
import mppb.restapi.mobile.feature.perfil.dto.PerfilUsuarioDto;

@Repository
@RequiredArgsConstructor
public class PerfilUsuarioRepository {

    private final JdbcTemplate jdbcTemplate;

    public PerfilUsuarioDto buscarPorLogin(String login) {
        String sql = """
                select
                    u.login,
                    coalesce(p.nome, u.nome) as nome,
                    s.matricula,
                    s.ativo as situacao,
                    s.dataposse as data_posse,
                    s.dataadmissao as data_admissao,
                    s.dataexercicio as data_exercicio,
                    p.email,
                    p.tel as telefone,
                    p.cel as celular,
                    st.nome as setor_nome,
                    st.sigla as setor_sigla,
                    po.nome as orgao_nome,
                    o.sigla as orgao_sigla,
                    tc.tipo as tipo_cargo,
                    f.nome as cargo,
                    f.nome as titulo_cargo,
                    null as especialidade
                from autorizacao.usuario u
                join servidor s on s.matricula = u.matricula and s.excluido = false
                left join pessoa p on p.id = s.id_pessoa and p.excluido = false
                left join recursoshumanos.funcao f on f.id = s.id_cargo
                left join recursoshumanos.tipocargo tc on tc.id = f.id_tipocargo
                left join setor st on st.id = s.id_setor and st.excluido = false
                left join orgao o on o.id_pessoa = st.id_orgao and o.excluido = false
                left join pessoa po on po.id = o.id_pessoa and po.excluido = false
                where u.login = ?
                  and u.excluido = false
                order by
                    case when s.ativo = 'A' then 0 else 1 end,
                    s.dataatualizacao desc nulls last,
                    s.dataregistro desc nulls last,
                    s.id desc
                limit 1
                """;

        List<PerfilUsuarioDto> perfis = jdbcTemplate.query(sql, this::mapRow, login);
        if (perfis == null || perfis.isEmpty()) {
            return null;
        }
        return perfis.get(0);
    }

    private PerfilUsuarioDto mapRow(ResultSet rs, int rowNum) throws SQLException {
        PerfilUsuarioDto perfil = new PerfilUsuarioDto();
        perfil.setLogin(rs.getString("login"));
        perfil.setNome(rs.getString("nome"));
        perfil.setMatricula(rs.getString("matricula"));
        perfil.setSituacao(normalizarSituacao(rs.getString("situacao")));
        perfil.setDataPosse(rs.getDate("data_posse"));
        perfil.setDataAdmissao(rs.getDate("data_admissao"));
        perfil.setDataExercicio(rs.getDate("data_exercicio"));
        perfil.setEmail(rs.getString("email"));
        perfil.setTelefone(rs.getString("telefone"));
        perfil.setCelular(rs.getString("celular"));
        perfil.setSetorNome(rs.getString("setor_nome"));
        perfil.setSetorSigla(rs.getString("setor_sigla"));
        perfil.setOrgaoNome(rs.getString("orgao_nome"));
        perfil.setOrgaoSigla(rs.getString("orgao_sigla"));
        perfil.setTipoCargo(normalizarTipoCargo(rs.getString("tipo_cargo")));
        perfil.setCargo(rs.getString("cargo"));
        perfil.setTituloCargo(rs.getString("titulo_cargo"));
        perfil.setEspecialidade(rs.getString("especialidade"));
        return perfil;
    }

    private String normalizarSituacao(String situacao) {
        if (situacao == null || situacao.isBlank()) {
            return null;
        }
        return "A".equalsIgnoreCase(situacao.trim()) ? "Ativo" : "Inativo";
    }

    private String normalizarTipoCargo(String tipoCargo) {
        if (tipoCargo == null || tipoCargo.isBlank()) {
            return null;
        }
        String normalizado = tipoCargo.trim().toUpperCase();
        if ("M".equals(normalizado)) {
            return "Membro";
        }
        if ("S".equals(normalizado)) {
            return "Servidor";
        }
        return tipoCargo.trim();
    }
}
