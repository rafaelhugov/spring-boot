package mppb.restapi.cursos.feature.documentos.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import mppb.restapi.cursos.feature.inscricao.enums.SituacaoInscricaoEnum;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeracaoDocumentoRequest {
    private String tipoDocumento; // LISTA_PRESENCA, OFICIO_DRH, OFICIO_CORREGEDORIA, OFICIO_CONSOLIDADO

    // Ofício DRH / Corregedoria
    private String numeroOficio;
    private String anoOficio;
    private String destinatarioNome;
    private String destinatarioCargo;
    private String orgaoDestinatario;
    private String assunto;
    private String tratamentoDestinatario;
    private String pronomeTratamento;
    private String finalidadeAnotacao;
    private String orgaoPromotor;
    
    // Lista de Presença
    private Integer quantidadeLinhasAdicionais;
    private Boolean exibirBrasao;

    // Assinante e outros
    private String assinanteNome;
    private String assinanteCargo;
    
    // Filtros
    private String tipoPublico; // Todos, Servidores, Membros
    private SituacaoInscricaoEnum situacaoInscricao; // APROVADO, INSCRITO, etc
    
    // Sobrescrita de evento
    private String localEvento;
    private String descricaoComplementarEvento;
    private String nomeCurso;
    private String dataEvento;
    private String horarioEvento;
}
