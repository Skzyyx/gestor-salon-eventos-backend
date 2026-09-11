package mx.gestorsalon.service;

import lombok.RequiredArgsConstructor;
import mx.gestorsalon.dto.ClienteDTO;
import mx.gestorsalon.exception.AccesoDenegadoException;
import mx.gestorsalon.exception.RecursoNoEncontradoException;
import mx.gestorsalon.mapper.ClienteMapper;
import mx.gestorsalon.mapper.DireccionMapper;
import mx.gestorsalon.model.Cliente;
import mx.gestorsalon.model.Negocio;
import mx.gestorsalon.repository.ClienteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ClienteService {

    private final ClienteRepository clienteRepository;
    private final ClienteMapper clienteMapper;
    private final DireccionMapper direccionMapper;

    @Transactional(readOnly = true)
    public List<ClienteDTO> getAllByNegocio(Long negocioId) {
        return clienteRepository.findByNegocioId(negocioId)
                .stream()
                .map(clienteMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public ClienteDTO create(Negocio negocio, ClienteDTO dto) {
        Cliente cliente = clienteMapper.toEntity(dto);
        cliente.setNegocio(negocio); // Magia multi-tenant
        cliente = clienteRepository.save(cliente);
        return clienteMapper.toDTO(cliente);
    }

    @Transactional
    public ClienteDTO update(Negocio negocio, Long clienteId, ClienteDTO dto) {
        Cliente cliente = obtenerPropio(negocio, clienteId);

        cliente.setNombre(dto.getNombre());
        cliente.setTelefono(dto.getTelefono());
        cliente.setEmail(dto.getEmail());
        cliente.setDireccion(direccionMapper.toEntity(dto.getDireccion()));
        if (dto.getActivo() != null) {
            cliente.setActivo(dto.getActivo());
        }

        cliente = clienteRepository.save(cliente);
        return clienteMapper.toDTO(cliente);
    }

    @Transactional
    public void deactivate(Negocio negocio, Long clienteId) {
        Cliente cliente = obtenerPropio(negocio, clienteId);
        cliente.setActivo(false); // Soft-delete, como el resto del catálogo
        clienteRepository.save(cliente);
    }

    /**
     * Carga un cliente garantizando que pertenezca al negocio del usuario autenticado.
     * Centraliza la validación de tenant para create/update/deactivate.
     */
    private Cliente obtenerPropio(Negocio negocio, Long clienteId) {
        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cliente no encontrado"));
        if (!cliente.getNegocio().getId().equals(negocio.getId())) {
            throw new AccesoDenegadoException("El cliente no pertenece a su negocio");
        }
        return cliente;
    }
}
