package mppb.restapi.mobile.feature.carteirafuncional.service;

import mppb.restapi.mobile.feature.carteirafuncional.dto.ArquivoTO;
import mppb.restapi.mobile.feature.carteirafuncional.dto.CarteiraFuncionalTO;
import mppb.restapi.mobile.feature.carteirafuncional.dto.ServidorResponseTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Date;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import mppb.restapi.mobile.feature.synthetic.dataset.SyntheticDataService;

@Slf4j
@Service
public class CarteiraService {

    private final JdbcTemplate jdbcTemplate;
    private final JcrService jcrService;

    private SyntheticDataService syntheticDataService;

    @Value("${carteira.pgj-fallback:ANTONIO HORTÊNCIO ROCHA NETO}")
    private String nomePgjFallback;

    private boolean avisoPgjFallbackRegistrado;

    public CarteiraService(JdbcTemplate jdbcTemplate, JcrService jcrService) {
        this.jdbcTemplate = jdbcTemplate;
        this.jcrService = jcrService;
    }

    @Autowired
    public CarteiraService(JdbcTemplate jdbcTemplate, JcrService jcrService, @Autowired(required = false) SyntheticDataService syntheticDataService) {
        this.jdbcTemplate = jdbcTemplate;
        this.jcrService = jcrService;
        this.syntheticDataService = syntheticDataService;
    }

    public CarteiraFuncionalTO recuperaCarteiraFuncionalByLogin(String login) {
        if (syntheticDataService != null && syntheticDataService.isSyntheticLogin(login)) {
            return syntheticDataService.getCarteiraByLogin(login);
        }
        return recuperaCarteiraFuncionalByMd5OrLogin(login, null);
    }

    public CarteiraFuncionalTO recuperaCarteiraFuncionalByHash(String hash) {
        if (syntheticDataService != null && syntheticDataService.isSyntheticHash(hash)) {
            return syntheticDataService.getCarteiraByHash(hash);
        }
        return recuperaCarteiraFuncionalByMd5OrLogin(null, hash);
    }

    private CarteiraFuncionalTO recuperaCarteiraFuncionalByMd5OrLogin(String login, String hash) {
        String randomMD5 = "";

        if (hash == null) {
            randomMD5 = UUID.randomUUID().toString().replace("-", ""); // Basic MD5 equivalent size logic
        }

        String sql = "SELECT * FROM recursoshumanos.dadosqrcode(?, ?, ?)";
        List<CarteiraFuncionalTO> lista = jdbcTemplate.query(sql,
                new Object[]{login, hash, randomMD5},
                new BeanPropertyRowMapper<>(CarteiraFuncionalTO.class));

        if (lista == null || lista.isEmpty()) {
            return null;
        }

        CarteiraFuncionalTO carteira = lista.get(0);

        if (hash == null) {
            jdbcTemplate.execute("insert into recursoshumanos.qrcodecarteira(id_servidor, hash, dataregistro) values("
                    + carteira.getIdServidor() + ", '" + carteira.getMd5() + "', now() )");
        }

        if (login != null) {
            carteira.setLogin(login);
        } else if (carteira.getLogin() == null && carteira.getMatricula() != null) {
            carteira.setLogin(getLoginByMatricula(carteira.getMatricula()));
        }
        carteira.setDataPosse(buscarDataPosse(carteira));
        carteira.setNomePgj(getAtualPGJ());

        return carteira;
    }

    private Date buscarDataPosse(CarteiraFuncionalTO carteira) {
        if (carteira == null) {
            return null;
        }

        try {
            if (carteira.getIdServidor() != null) {
                List<Date> datas = jdbcTemplate.queryForList(
                        "select s.dataposse from servidor s where s.excluido = false and s.id = ? order by s.ativo asc limit 1",
                        Date.class,
                        carteira.getIdServidor());
                if (datas != null && !datas.isEmpty()) {
                    return datas.get(0);
                }
            }

            if (carteira.getMatricula() != null && !carteira.getMatricula().isBlank()) {
                List<Date> datas = jdbcTemplate.queryForList(
                        "select s.dataposse from servidor s where s.excluido = false and s.matricula = ? order by s.ativo asc limit 1",
                        Date.class,
                        carteira.getMatricula());
                if (datas != null && !datas.isEmpty()) {
                    return datas.get(0);
                }
            }
        } catch (DataAccessException e) {
            log.warn("Nao foi possivel buscar data de posse da carteira funcional", e);
        }

        return null;
    }

    public byte[] getBytesFotoServidorPorLogin(String login) {
        if (login == null || login.isBlank()) {
            return null;
        }
        if (syntheticDataService != null && syntheticDataService.isSyntheticLogin(login)) {
            return syntheticDataService.getFotoByLogin(login);
        }

        StringBuilder query = new StringBuilder();
        byte[] bytes = null;

        query.append("select bytes, id, pathrepositorio, ");
        query.append("case when estanorepositorio is not null then estanorepositorio ");
        query.append("else false end as estanorepositorio ");
        query.append("from ged.arquivo where id = ");
        query.append("(select pf.id_fotoprincipal from pessoafisica pf ");
        query.append("join servidor s on pf.id_pessoa = s.id_pessoa ");
        query.append("join servidor ser on ser.id_pessoa = pf.id_pessoa ");
        query.append("join autorizacao.usuario u on u.matricula= s.matricula and u.login = '" + login + "' and u.excluido = false ");
        query.append("where not ser.excluido order by ser.dataregistro desc limit 1) ");

        List<ArquivoTO> lista = jdbcTemplate.query(query.toString(),
                BeanPropertyRowMapper.newInstance(ArquivoTO.class));

        if (lista == null || lista.isEmpty()) {
            return null;
        }

        ArquivoTO arquivo = lista.get(0);

        if (arquivo.getBytes() != null) {
            bytes = arquivo.getBytes();
        }

        if ((bytes == null || bytes.length == 0)) {
            boolean estaNoRepositorio = arquivo.getPathrepositorio() != null && arquivo.getEstanorepositorio();
            if (estaNoRepositorio) {
                try {
                    bytes = jcrService.getBytes(arquivo.getPathrepositorio());
                } catch (Exception e) {
                    log.error("Repositorio JCR ERRO - Problema no repositório ao recuperar o arquivo de id "
                            + arquivo.getId() + " - " + e.getMessage() + ". Path repositorio: " + arquivo.getPathrepositorio(), e);
                }
            }
        }
        return bytes;
    }

    public byte[] getBytesFotoServidorPorCarteira(CarteiraFuncionalTO carteira) {
        if (carteira == null) {
            return null;
        }
        if (syntheticDataService != null && syntheticDataService.isSyntheticCarteira(carteira)) {
            return syntheticDataService.getFotoByLogin(carteira.getLogin());
        }

        byte[] bytes = getBytesFotoServidorPorLogin(carteira.getLogin());
        if (bytes != null && bytes.length > 0) {
            return bytes;
        }

        if (carteira.getIdPessoa() == null && carteira.getIdServidor() == null && carteira.getMatricula() == null) {
            return null;
        }

        StringBuilder query = new StringBuilder();
        query.append("select a.bytes, a.id, a.pathrepositorio, ");
        query.append("case when a.estanorepositorio is not null then a.estanorepositorio ");
        query.append("else false end as estanorepositorio ");
        query.append("from ged.arquivo a ");
        query.append("join pessoafisica pf on pf.id_fotoprincipal = a.id ");
        query.append("join servidor s on s.id_pessoa = pf.id_pessoa ");
        query.append("where not s.excluido ");

        Object parametro;
        if (carteira.getIdPessoa() != null) {
            query.append("and pf.id_pessoa = ? ");
            parametro = carteira.getIdPessoa();
        } else if (carteira.getIdServidor() != null) {
            query.append("and s.id_servidor = ? ");
            parametro = carteira.getIdServidor();
        } else {
            query.append("and s.matricula = ? ");
            parametro = carteira.getMatricula();
        }

        query.append("order by s.dataregistro desc limit 1");

        List<ArquivoTO> lista = jdbcTemplate.query(query.toString(),
                new Object[]{parametro},
                BeanPropertyRowMapper.newInstance(ArquivoTO.class));

        if (lista == null || lista.isEmpty()) {
            return null;
        }

        ArquivoTO arquivo = lista.get(0);
        bytes = arquivo.getBytes();

        if ((bytes == null || bytes.length == 0)) {
            boolean estaNoRepositorio = arquivo.getPathrepositorio() != null && arquivo.getEstanorepositorio();
            if (estaNoRepositorio) {
                try {
                    bytes = jcrService.getBytes(arquivo.getPathrepositorio());
                } catch (Exception e) {
                    log.error("Repositorio JCR ERRO - Problema no repositório ao recuperar o arquivo de id "
                            + arquivo.getId() + " - " + e.getMessage() + ". Path repositorio: " + arquivo.getPathrepositorio(), e);
                }
            }
        }

        return bytes;
    }

    public String getAtualPGJ() {
        try {
            String query = "select vw.servidor_nome as pessoaFisicaNome from diariooficial.mapa_por_membro_vw vw where vw.ativo='A' and vw.funcao_id = 1 and vw.designacao_tipoletra = 'F' order by designacao_id desc limit 1";

            List<ServidorResponseTO> lista = jdbcTemplate.query(query,
                    BeanPropertyRowMapper.newInstance(ServidorResponseTO.class));

            if (lista == null || lista.isEmpty()) {
                return nomePgjFallback;
            }

            ServidorResponseTO servidor = lista.get(0);

            if (servidor == null || servidor.getPessoaFisicaNome() == null) {
                return nomePgjFallback;
            }

            return servidor.getPessoaFisicaNome();
        } catch (DataAccessException e) {
            registrarAvisoPgjFallback("Sem acesso para buscar PGJ atual no banco. Usando nome de fallback. Causa: " + e.getMostSpecificCause().getMessage());
            return nomePgjFallback;
        } catch (Exception e) {
            log.warn("Erro ao buscar o PGJ atual no banco de dados. Usando nome de fallback para testes.", e);
            return nomePgjFallback;
        }
    }

    private void registrarAvisoPgjFallback(String mensagem) {
        if (!avisoPgjFallbackRegistrado) {
            log.warn(mensagem);
            avisoPgjFallbackRegistrado = true;
        }
    }

    public String getLoginByMatricula(String matricula) {
        try {
            String sql = "select login from autorizacao.usuario where matricula = ? and excluido = false order by id desc limit 1";
            return jdbcTemplate.queryForObject(sql, String.class, matricula);
        } catch (Exception e) {
            log.error("Erro ao buscar login por matricula: " + matricula, e);
            return null;
        }
    }
}
