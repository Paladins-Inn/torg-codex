package de.paladinsinn.torg.codex.data.adapter.out.persistence;

import de.paladinsinn.torg.codex.application.port.out.CatalogPersistencePort;
import de.paladinsinn.torg.codex.data.mapper.VehicleEntityMapper;
import de.paladinsinn.torg.codex.data.repository.VehicleRepository;
import de.paladinsinn.torg.codex.domain.model.Vehicle;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Outbound persistence adapter exposing {@link Vehicle} domain models backed by the
 * {@code Vehicle} JPA entity via {@link VehicleEntityMapper}.
 */
public final class JpaVehiclePersistenceAdapter implements CatalogPersistencePort<Vehicle> {

    private final VehicleRepository repository;
    private final VehicleEntityMapper mapper;

    public JpaVehiclePersistenceAdapter(VehicleRepository repository, VehicleEntityMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public List<Vehicle> findAll() {
        return repository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public Optional<Vehicle> findById(UUID id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Vehicle> findByCosm(String cosm) {
        return repository.findByCosm(cosm).stream().map(mapper::toDomain).toList();
    }
}
