package br.gov.mppb.athena.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

class ServicoVersaoAtualDocumentoTest {

    private final ServicoVersaoAtualDocumento servico = new ServicoVersaoAtualDocumento();

    @Test
    void deveAtualizarReferenciaSomenteParaVersaoDoProprioDocumento() {
        UUID documento = UUID.randomUUID();
        UUID versao = UUID.randomUUID();
        EstadoVersaoAtualDocumento estado = new EstadoVersaoAtualDocumento(documento, null, 0);

        EstadoVersaoAtualDocumento atualizado = servico.atualizar(
                estado,
                new ComandoAtualizarVersaoAtual(documento, versao, documento, null, 0),
                () -> { }
        );

        assertThat(atualizado.idVersaoAtual()).isEqualTo(versao);
        assertThat(atualizado.versaoControle()).isEqualTo(1);
    }

    @Test
    void deveRejeitarVersaoPertencenteAOutroDocumento() {
        UUID documento = UUID.randomUUID();
        EstadoVersaoAtualDocumento estado = new EstadoVersaoAtualDocumento(documento, null, 0);

        assertThatThrownBy(() -> servico.atualizar(
                estado,
                new ComandoAtualizarVersaoAtual(
                        documento,
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        null,
                        0
                ),
                () -> { }
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("versao atual deve pertencer ao proprio documento.");
    }

    @Test
    void devePreservarEstadoAnteriorQuandoPersistenciaDaNovaVersaoFalhar() {
        UUID documento = UUID.randomUUID();
        UUID versaoAtual = UUID.randomUUID();
        EstadoVersaoAtualDocumento estado = new EstadoVersaoAtualDocumento(documento, versaoAtual, 3);

        assertThatThrownBy(() -> servico.atualizar(
                estado,
                new ComandoAtualizarVersaoAtual(
                        documento,
                        UUID.randomUUID(),
                        documento,
                        versaoAtual,
                        3
                ),
                () -> { throw new IllegalStateException("falha simulada"); }
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("falha simulada");

        assertThat(estado.idVersaoAtual()).isEqualTo(versaoAtual);
        assertThat(estado.versaoControle()).isEqualTo(3);
    }

    @Test
    void deveRejeitarAtualizacaoConcorrenteComReferenciaEsperadaObsoleta() {
        UUID documento = UUID.randomUUID();
        EstadoVersaoAtualDocumento estado = new EstadoVersaoAtualDocumento(
                documento,
                UUID.randomUUID(),
                4
        );
        AtomicBoolean persistenciaExecutada = new AtomicBoolean();

        assertThatThrownBy(() -> servico.atualizar(
                estado,
                new ComandoAtualizarVersaoAtual(
                        documento,
                        UUID.randomUUID(),
                        documento,
                        UUID.randomUUID(),
                        3
                ),
                () -> persistenciaExecutada.set(true)
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("referencia ou versao de controle divergente.");

        assertThat(persistenciaExecutada).isFalse();
    }
}
