package ma.youcode.covoiturage.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.youcode.covoiturage.dto.request.PassagerRequest;
import ma.youcode.covoiturage.dto.response.PassagerResponse;
import ma.youcode.covoiturage.entity.Passager;
import ma.youcode.covoiturage.exception.BusinessRuleException;
import ma.youcode.covoiturage.exception.ForbiddenOperationException;
import ma.youcode.covoiturage.exception.InvalidRequestException;
import ma.youcode.covoiturage.exception.ResourceNotFoundException;
import ma.youcode.covoiturage.mapper.PassagerMapper;
import ma.youcode.covoiturage.repository.PassagerRepository;
import ma.youcode.covoiturage.service.PassagerService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PassagerServiceImpl implements PassagerService {

    private final PassagerRepository passagerRepository;
    private final PassagerMapper passagerMapper;

    @Override
    @Transactional
    public PassagerResponse create(PassagerRequest request) {
        // R-04: Email passager unique
        if (passagerRepository.existsByEmailIgnoreCase(request.email())) {
            log.warn("Création passager refusée : email déjà utilisé '{}'", request.email());
            throw new BusinessRuleException("Cet email est déjà utilisé par un autre passager");
        }

        Passager passager = passagerMapper.toEntity(request);
        Passager saved = passagerRepository.save(passager);
        log.info("Passager créé avec succès avec l'id : {}", saved.getId());
        return passagerMapper.toResponse(saved);
    }

    @Override
    public PassagerResponse findById(Long id) {
        return passagerRepository.findById(id)
                .map(passagerMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Passager non trouvé avec l'id : " + id));
    }

    @Override
    @Transactional
    public PassagerResponse update(Long actingId, Long id, PassagerRequest request) {
        // R-36: Seul le passager concerné peut modifier son profil
        if (actingId == null || !actingId.equals(id)) {
            log.warn("Tentative de modification non autorisée : acteur {} sur profil passager {}", actingId, id);
            throw new ForbiddenOperationException("Vous n'êtes pas autorisé à modifier le profil d'un autre passager");
        }

        Passager passager = passagerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Passager non trouvé avec l'id : " + id));

        // R-04: Email unique (sauf si c'est son propre email actuel)
        if (passagerRepository.existsByEmailIgnoreCaseAndIdNot(request.email(), id)) {
            log.warn("Modification passager {} refusée : email déjà utilisé '{}'", id, request.email());
            throw new BusinessRuleException("Cet email est déjà utilisé par un autre passager");
        }

        passager.updateProfile(request.nom(), request.prenom(), request.email(), request.telephone());
        log.info("Passager avec l'id {} mis à jour avec succès", id);
        return passagerMapper.toResponse(passager);
    }

    @Override
    public List<PassagerResponse> findAll() {
        return passagerMapper.toResponseList(passagerRepository.findAll());
    }

    @Override
    public List<PassagerResponse> searchByEmail(String email) {
        // F-10: Recherche sans email -> 400
        if (email == null || email.isBlank()) {
            log.warn("Recherche de passager sans email");
            throw new InvalidRequestException("Le paramètre 'email' est obligatoire pour la recherche");
        }

        List<Passager> results = passagerRepository.findByEmailContainingIgnoreCase(email.trim());
        return passagerMapper.toResponseList(results);
    }
}
