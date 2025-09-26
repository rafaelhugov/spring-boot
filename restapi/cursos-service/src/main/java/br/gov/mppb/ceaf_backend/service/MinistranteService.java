package br.gov.mppb.ceaf_backend.service;

import br.gov.mppb.ceaf_backend.domain.*;
import br.gov.mppb.ceaf_backend.repository.*;
import br.gov.mppb.ceaf_backend.service.dto.*;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class MinistranteService {

    @Autowired private CursoRepository cursoRepository;
    @Autowired private MinistranteRepository ministranteRepository;
    @Autowired private PapelMinistranteRepository papelMinistranteRepository;
    @Autowired private CursoMinistranteRepository cursoMinistranteRepository;
    @Autowired private ServidorService servidorService;

    @Transactional(readOnly = true)
    public List<MinistranteCursoDTO> getAllByCurso(Long cursoId) {
        return cursoMinistranteRepository.findByCursoId(cursoId).stream()
                .map(assoc -> {
                    Ministrante m = assoc.getMinistrante();
                    String nome = m.getNome();
                    // CORREÇÃO: Se o nome for nulo e for um servidor interno, busca o nome.
                    if (nome == null && m.getIdServidor() != null) {
                        // Esta é uma solução temporária. O ideal é que o nome seja salvo
                        // na tabela 'ministrante' no momento da criação.
                        Optional<ServidorDTO> optional = servidorService.buscarServidorPorId(m.getIdServidor());
                        ServidorDTO servidor = optional.get();

                        nome = servidor.getNome();
                    }
                    return new MinistranteCursoDTO(
                            assoc.getId(), m.getId(), m.getIdServidor(), nome, m.getNomeOrgaoExterno(), assoc.getPapel()
                    );
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public MinistranteCursoDTO addMinistranteToCurso(Long cursoId, MinistranteAddDTO dto) {
        Curso curso = cursoRepository.findById(cursoId)
                .orElseThrow(() -> new EntityNotFoundException("Curso não encontrado com o id: " + cursoId));

        Ministrante ministrante = ministranteRepository.findByIdServidor(dto.getIdServidor())
                .orElseGet(() -> {
                    Ministrante newMinistrante = new Ministrante();
                    newMinistrante.setIdServidor(dto.getIdServidor());
                    newMinistrante.setNome(dto.getNome());
                    return ministranteRepository.save(newMinistrante);
                });

        if (cursoMinistranteRepository.existsByCursoIdAndMinistranteId(cursoId, ministrante.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Este ministrante já está associado a este curso.");
        }

        PapelMinistrante papel = papelMinistranteRepository.findById(dto.getPapel().getId())
                .orElseThrow(() -> new EntityNotFoundException("Papel não encontrado"));

        CursoMinistrante associacao = new CursoMinistrante();
        associacao.setCurso(curso);
        associacao.setMinistrante(ministrante);
        associacao.setPapel(papel);

        CursoMinistrante savedAssoc = cursoMinistranteRepository.save(associacao);
        return new MinistranteCursoDTO(
                savedAssoc.getId(),
                savedAssoc.getMinistrante().getId(),
                savedAssoc.getMinistrante().getIdServidor(),
                savedAssoc.getMinistrante().getNome(),
                savedAssoc.getMinistrante().getNomeOrgaoExterno(),
                savedAssoc.getPapel()
        );
    }


    @Transactional
    public MinistranteCursoDTO updateMinistrante(Long cursoId, Long ministranteAssociacaoId, MinistranteUpdateDTO dto) {
        CursoMinistrante associacao = cursoMinistranteRepository.findById(ministranteAssociacaoId)
                .orElseThrow(() -> new EntityNotFoundException("Associação de ministrante não encontrada"));

        Ministrante ministrante = associacao.getMinistrante();

        // REGRA DE NEGÓCIO: Se for externo, permite atualizar nome e órgão
        if (ministrante.getIdServidor() == null) {
            ministrante.setNome(dto.getNome());
            ministrante.setNomeOrgaoExterno(dto.getNomeOrgaoExterno());
            ministranteRepository.save(ministrante);
        }

        PapelMinistrante novoPapel = papelMinistranteRepository.findById(dto.getPapel().getId())
                .orElseThrow(() -> new EntityNotFoundException("Papel não encontrado"));

        associacao.setPapel(novoPapel);
        CursoMinistrante updatedAssoc = cursoMinistranteRepository.save(associacao);

        return new MinistranteCursoDTO(
                updatedAssoc.getId(),
                updatedAssoc.getMinistrante().getId(),
                updatedAssoc.getMinistrante().getIdServidor(),
                updatedAssoc.getMinistrante().getNome(),
                updatedAssoc.getMinistrante().getNomeOrgaoExterno(),
                updatedAssoc.getPapel()
        );
    }
    @Transactional
    public void removeMinistranteFromCurso(Long cursoId, Long ministranteAssociacaoId) {
        // A validação de que a associação pertence ao curso foi omitida por simplicidade,
        // mas poderia ser adicionada para segurança extra.
        if (!cursoMinistranteRepository.existsById(ministranteAssociacaoId)) {
            throw new EntityNotFoundException("Associação de ministrante não encontrada com o id: " + ministranteAssociacaoId);
        }
        cursoMinistranteRepository.deleteById(ministranteAssociacaoId);
    }
}

