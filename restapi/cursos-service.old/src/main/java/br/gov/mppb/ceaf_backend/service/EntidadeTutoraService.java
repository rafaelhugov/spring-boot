package br.gov.mppb.ceaf_backend.service;
import br.gov.mppb.ceaf_backend.domain.EntidadeTutora;
import br.gov.mppb.ceaf_backend.repository.EntidadeTutoraRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class EntidadeTutoraService {

    private final EntidadeTutoraRepository entidadeTutoraRepository;

    public EntidadeTutoraService(EntidadeTutoraRepository entidadeTutoraRepository) {
        this.entidadeTutoraRepository = entidadeTutoraRepository;
    }

    public List<EntidadeTutora> findAll() {
        return entidadeTutoraRepository.findAll();
    }

    public EntidadeTutora save(EntidadeTutora entidadeTutora) {
        return entidadeTutoraRepository.save(entidadeTutora);
    }

    public void delete(Integer id) {
        entidadeTutoraRepository.deleteById(id);
    }
}
