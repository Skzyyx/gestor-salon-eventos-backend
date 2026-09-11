package mx.gestorsalon.mapper;

import lombok.RequiredArgsConstructor;
import mx.gestorsalon.dto.ClienteDTO;
import mx.gestorsalon.model.Cliente;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ClienteMapper {

    private final DireccionMapper direccionMapper;

    public ClienteDTO toDTO(Cliente cliente) {
        if (cliente == null)
            return null;
        ClienteDTO dto = new ClienteDTO();
        dto.setId(cliente.getId());
        dto.setNombre(cliente.getNombre());
        dto.setTelefono(cliente.getTelefono());
        dto.setEmail(cliente.getEmail());
        dto.setActivo(cliente.getActivo());
        dto.setDireccion(direccionMapper.toDTO(cliente.getDireccion()));
        return dto;
    }

    public Cliente toEntity(ClienteDTO dto) {
        if (dto == null)
            return null;
        Cliente cliente = new Cliente();
        cliente.setNombre(dto.getNombre());
        cliente.setTelefono(dto.getTelefono());
        cliente.setEmail(dto.getEmail());
        cliente.setDireccion(direccionMapper.toEntity(dto.getDireccion()));
        cliente.setActivo(dto.getActivo() != null ? dto.getActivo() : true);
        return cliente;
    }
}
