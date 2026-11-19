package mppb.restapi.mobile.feature.mensagem.service;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Component
@ConfigurationProperties(prefix = "mensagens")
@Getter
@Setter
public class MensagemStorageProperties {

    private String authorityAdmin = "androidmppb.mensagens.admin";
    private String storagePath = "/tmp/mppb-mensagens-storage";
    private long maxImagemBytes = 10L * 1024 * 1024;
    private long maxAudioBytes = 15L * 1024 * 1024;
    private long maxDocumentoBytes = 25L * 1024 * 1024;
    private long maxVideoBytes = 50L * 1024 * 1024;
}

