package ma.youcode.covoiturage.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.youcode.covoiturage.dto.request.VehiculeRequest;
import ma.youcode.covoiturage.dto.response.VehiculeResponse;
import ma.youcode.covoiturage.entity.Conducteur;
import ma.youcode.covoiturage.entity.Vehicule;
import ma.youcode.covoiturage.exception.BusinessRuleException;
import ma.youcode.covoiturage.exception.ForbiddenOperationException;
import ma.youcode.covoiturage.exception.InvalidRequestException;
import ma.youcode.covoiturage.exception.ResourceNotFoundException;
import ma.youcode.covoiturage.mapper.VehiculeMapper;
import ma.youcode.covoiturage.repository.ConducteurRepository;
import ma.youcode.covoiturage.repository.TrajetRepository;
import ma.youcode.covoiturage.repository.VehiculeRepository;
import ma.youcode.covoiturage.service.VehiculeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class VehiculeServiceImpl implements VehiculeService {

    private final VehiculeRepository vehiculeRepository;
    private final ConducteurRepository conducteurRepository;
    private final TrajetRepository trajetRepository;
    private final VehiculeMapper vehiculeMapper;

    @Override
    @Transactional
    public VehiculeResponse create(Long actingDriverId, VehiculeRequest request) {
        // R-07 / R-36: Vérifier l'existence du conducteur propriétaire
        Conducteur conducteur = conducteurRepository.findById(actingDriverId)
                .orElseThrow(() -> new ResourceNotFoundException("Conducteur non trouvé avec l'id : " + actingDriverId));

        // R-06: Capacité comprise entre 1 et 8
        validateCapacite(request.capacite());

        // R-05: Immatriculation unique
        if (vehiculeRepository.existsByImmatriculationIgnoreCase(request.immatriculation())) {
            log.warn("Ajout de véhicule refusé : immatriculation déjà utilisée '{}'", request.immatriculation());
            throw new BusinessRuleException("Cette immatriculation est déjà enregistrée pour un autre véhicule");
        }

        Vehicule vehicule = vehiculeMapper.toEntity(request);
        vehicule.assignToConducteur(conducteur);
        Vehicule saved = vehiculeRepository.save(vehicule);

        log.info("Véhicule créé avec succès avec l'id {} pour le conducteur {}", saved.getId(), actingDriverId);
        return vehiculeMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public VehiculeResponse update(Long actingDriverId, Long id, VehiculeRequest request) {
        Vehicule vehicule = vehiculeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Véhicule non trouvé avec l'id : " + id));

        // R-36: Seul le conducteur propriétaire peut modifier son véhicule
        if (vehicule.getConducteur() == null || !vehicule.getConducteur().getId().equals(actingDriverId)) {
            log.warn("Tentative de modification non autorisée : conducteur {} sur véhicule {}", actingDriverId, id);
            throw new ForbiddenOperationException("Vous n'êtes pas autorisé à modifier le véhicule d'un autre conducteur");
        }

        // R-06: Capacité comprise entre 1 et 8
        validateCapacite(request.capacite());

        // R-05: Immatriculation unique (sauf si c'est la sienne actuelle)
        if (vehiculeRepository.existsByImmatriculationIgnoreCaseAndIdNot(request.immatriculation(), id)) {
            log.warn("Modification du véhicule {} refusée : immatriculation déjà utilisée '{}'", id, request.immatriculation());
            throw new BusinessRuleException("Cette immatriculation est déjà enregistrée pour un autre véhicule");
        }

        vehicule.updateDetails(request.marque(), request.modele(), request.immatriculation(), request.couleur(), request.capacite());
        log.info("Véhicule avec l'id {} mis à jour avec succès", id);
        return vehiculeMapper.toResponse(vehicule);
    }

    @Override
    public VehiculeResponse findById(Long id) {
        return vehiculeRepository.findById(id)
                .map(vehiculeMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Véhicule non trouvé avec l'id : " + id));
    }

    @Override
    public List<VehiculeResponse> findByDriver(Long driverId) {
        if (driverId == null) {
            log.warn("Recherche de véhicules sans identifiant conducteur");
            throw new InvalidRequestException("L'identifiant du conducteur est obligatoire pour lister ses véhicules");
        }

        if (!conducteurRepository.existsById(driverId)) {
            throw new ResourceNotFoundException("Conducteur non trouvé avec l'id : " + driverId);
        }

        List<Vehicule> vehicules = vehiculeRepository.findByConducteurId(driverId);
        return vehiculeMapper.toResponseList(vehicules);
    }

    private void validateCapacite(Integer capacite) {
        if (capacite == null || capacite < 1 || capacite > 8) {
            log.warn("Capacité de véhicule invalide : {}", capacite);
            throw new InvalidRequestException("La capacité du véhicule doit être comprise entre 1 et 8 places");
        }
    }

    @Override 
    @Transactional
    public void delete(Long driverId, Long id) {
        // Vérification de l'existence (404)
        Vehicule vehicule = vehiculeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Véhicule non trouvé avec l'id : " + id));
        // 2. R-07 / R-36 : Vérification de propriété (403)
        if (vehicule.getConducteur() == null || !vehicule.getConducteur().getId().equals(driverId)) {
            throw new ForbiddenOperationException("Vous n'êtes pas autorisé à supprimer le véhicule d'un autre conducteur");
        }
        // 3. R-08 : Vérification des trajets liés (409)
        if (trajetRepository.existsByVehiculeId(id)) {
            throw new BusinessRuleException("Impossible de supprimer un véhicule associé à un ou plusieurs trajets");
        }
        // 4. Suppression
        vehiculeRepository.delete(vehicule);
    }
}
