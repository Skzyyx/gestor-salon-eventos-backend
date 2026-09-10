package mx.gestorsalon.mapper;

import mx.gestorsalon.dto.ClienteDTO;
import mx.gestorsalon.model.Cliente;
import org.springframework.stereotype.Component;

@Component
public class ClienteMapper {

    public ClienteDTO toDTO(Cliente cliente) {
        if (cliente == null)
            return null;
        ClienteDTO dto = new ClienteDTO();
        dto.setId(cliente.getId());
        dto.setNombre(cliente.getNombre());
        dto.setTelefono(cliente.getTelefono());
        dto.setEmail(cliente.getEmail());
        dto.setActivo(cliente.getActivo());
        dto.setDireccion(DireccionMapper.toDTO(cliente.getDireccion()));
        return dto;
    }

    public Cliente toEntity(ClienteDTO dto) {
        if (dto == null)
            return null;
        Cliente cliente = new Cliente();
        cliente.setNombre(dto.getNombre());
        cliente.setTelefono(dto.getTelefono());
        cliente.setEmail(dto.getEmail());
        cliente.setDireccion(DireccionMapper.toEntity(dto.getDireccion()));
        cliente.setActivo(dto.getActivo() != null ? dto.getActivo() : true);
        return cliente;
    }
}
