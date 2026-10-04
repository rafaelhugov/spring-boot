package mppb.restapi.mobile.feature.synthetic.model;

import java.util.Arrays;
import java.util.Optional;

public enum IdentidadeSintetica {

    UAT_MEMBRO(
            "uat.membro",
            OrigemConta.UAT,
            PerfilFuncional.MEMBRO,
            "UAT-MEMBRO-001",
            "900001",
            900001,
            "00000000191",
            "000.000.001-91",
            "Lucas Almeida Monteiro",
            "membro.uat@example.invalid",
            "uat-membro"
    ),

    UAT_SERVIDOR(
            "uat.servidor",
            OrigemConta.UAT,
            PerfilFuncional.SERVIDOR,
            "UAT-SERVIDOR-001",
            "900002",
            900002,
            "00000000272",
            "000.000.002-72",
            "Mariana Costa Ribeiro",
            "servidor.uat@example.invalid",
            "uat-servidor"
    ),

    REVIEW_GOOGLE_MEMBRO(
            "review.google.membro",
            OrigemConta.REVIEW_GOOGLE,
            PerfilFuncional.MEMBRO,
            "UAT-MEMBRO-001",
            "900001",
            900001,
            "00000000191",
            "000.000.001-91",
            "Lucas Almeida Monteiro",
            "membro.uat@example.invalid",
            "google-review-membro"
    ),

    REVIEW_GOOGLE_SERVIDOR(
            "review.google.servidor",
            OrigemConta.REVIEW_GOOGLE,
            PerfilFuncional.SERVIDOR,
            "UAT-SERVIDOR-001",
            "900002",
            900002,
            "00000000272",
            "000.000.002-72",
            "Mariana Costa Ribeiro",
            "servidor.uat@example.invalid",
            "google-review-servidor"
    ),

    REVIEW_APPLE_MEMBRO(
            "review.apple.membro",
            OrigemConta.REVIEW_APPLE,
            PerfilFuncional.MEMBRO,
            "UAT-MEMBRO-001",
            "900001",
            900001,
            "00000000191",
            "000.000.001-91",
            "Lucas Almeida Monteiro",
            "membro.uat@example.invalid",
            "apple-review-membro"
    ),

    REVIEW_APPLE_SERVIDOR(
            "review.apple.servidor",
            OrigemConta.REVIEW_APPLE,
            PerfilFuncional.SERVIDOR,
            "UAT-SERVIDOR-001",
            "900002",
            900002,
            "00000000272",
            "000.000.002-72",
            "Mariana Costa Ribeiro",
            "servidor.uat@example.invalid",
            "apple-review-servidor"
    );

    private final String login;
    private final OrigemConta origem;
    private final PerfilFuncional perfil;
    private final String identificador;
    private final String matricula;
    private final Integer matriculaNumerica;
    private final String cpfSemFormatacao;
    private final String cpfFormatado;
    private final String nome;
    private final String email;
    private final String chaveSenha;

    IdentidadeSintetica(
            String login,
            OrigemConta origem,
            PerfilFuncional perfil,
            String identificador,
            String matricula,
            Integer matriculaNumerica,
            String cpfSemFormatacao,
            String cpfFormatado,
            String nome,
            String email,
            String chaveSenha) {
        this.login = login;
        this.origem = origem;
        this.perfil = perfil;
        this.identificador = identificador;
        this.matricula = matricula;
        this.matriculaNumerica = matriculaNumerica;
        this.cpfSemFormatacao = cpfSemFormatacao;
        this.cpfFormatado = cpfFormatado;
        this.nome = nome;
        this.email = email;
        this.chaveSenha = chaveSenha;
    }

    public String getLogin() {
        return login;
    }

    public OrigemConta getOrigem() {
        return origem;
    }

    public PerfilFuncional getPerfil() {
        return perfil;
    }

    public String getIdentificador() {
        return identificador;
    }

    public String getMatricula() {
        return matricula;
    }

    public Integer getMatriculaNumerica() {
        return matriculaNumerica;
    }

    public String getCpfSemFormatacao() {
        return cpfSemFormatacao;
    }

    public String getCpfFormatado() {
        return cpfFormatado;
    }

    public String getNome() {
        return nome;
    }

    public String getEmail() {
        return email;
    }

    public String getChaveSenha() {
        return chaveSenha;
    }

    public static Optional<IdentidadeSintetica> porLogin(String login) {
        if (login == null || login.isBlank()) {
            return Optional.empty();
        }
        return Arrays.stream(values())
                .filter(id -> id.login.equalsIgnoreCase(login.trim()))
                .findFirst();
    }

    public static Optional<IdentidadeSintetica> porMatricula(String matricula) {
        if (matricula == null || matricula.isBlank()) {
            return Optional.empty();
        }
        return Arrays.stream(values())
                .filter(id -> id.matricula.equalsIgnoreCase(matricula.trim()))
                .findFirst();
    }

    public static Optional<IdentidadeSintetica> porCpf(String cpf) {
        if (cpf == null || cpf.isBlank()) {
            return Optional.empty();
        }
        String normalizado = cpf.replaceAll("\\D", "");
        return Arrays.stream(values())
                .filter(id -> id.cpfSemFormatacao.equals(normalizado))
                .findFirst();
    }
}
