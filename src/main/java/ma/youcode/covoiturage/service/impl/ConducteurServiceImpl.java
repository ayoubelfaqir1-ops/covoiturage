package ma.youcode.covoiturage.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.youcode.covoiturage.dto.request.ConducteurRequest;
import ma.youcode.covoiturage.dto.response.ConducteurResponse;
import ma.youcode.covoiturage.entity.Conducteur;
import ma.youcode.covoiturage.exception.BusinessRuleException;
import ma.youcode.covoiturage.exception.ForbiddenOperationException;
import ma.youcode.covoiturage.exception.InvalidRequestException;
import ma.youcode.covoiturage.exception.ResourceNotFoundException;
import ma.youcode.covoiturage.mapper.ConducteurMapper;
import ma.youcode.covoiturage.repository.ConducteurRepository;
import ma.youcode.covoiturage.service.ConducteurService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ConducteurServiceImpl implements ConducteurService {

    private final ConducteurRepository conducteurRepository;
    private final ConducteurMapper conducteurMapper;

    @Override
    @Transactional
    public ConducteurResponse create(ConducteurRequest request) {
        // R-01: Email conducteur unique
        if (conducteurRepository.existsByEmailIgnoreCase(request.email())) {
            log.warn("Création conducteur refusée : email déjà utilisé '{}'", request.email());
            throw new BusinessRuleException("Cet email est déjà utilisé par un autre conducteur");
        }

        // R-02: Numéro de permis unique
        if (conducteurRepository.existsByNumeroPermisIgnoreCase(request.numeroPermis())) {
            log.warn("Création conducteur refusée : numéro de permis déjà utilisé '{}'", request.numeroPermis());
            throw new BusinessRuleException("Ce numéro de permis est déjà utilisé par un autre conducteur");
        }

        Conducteur conducteur = conducteurMapper.toEntity(request);
        Conducteur saved = conducteurRepository.save(conducteur);
        log.info("Conducteur créé avec succès avec l'id : {}", saved.getId());
        return conducteurMapper.toResponse(saved);
    }

    @Override
    public List<ConducteurResponse> findAll() {
        return conducteurMapper.toResponseList(conducteurRepository.findAll());
    }

    @Override
    public ConducteurResponse findById(Long id) {
        Conducteur conducteur = conducteurRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Conducteur non trouvé avec l'id : " + id));
        return conducteurMapper.toResponse(conducteur);
    }

    @Override
    @Transactional
    public ConducteurResponse update(Long actingId, Long id, ConducteurRequest request) {
        // R-36: Un utilisateur ne modifie que son propre profil
        if (!actingId.equals(id)) {
            log.warn("Tentative de modification non autorisée : acteur {} sur profil {}", actingId, id);
            throw new ForbiddenOperationException("Vous n'êtes pas autorisé à modifier le profil d'un autre conducteur");
        }

        Conducteur conducteur = conducteurRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Conducteur non trouvé avec l'id : " + id));

        // R-01: Email unique (sauf si c'est son propre email actuel)
        if (conducteurRepository.existsByEmailIgnoreCaseAndIdNot(request.email(), id)) {
            log.warn("Modification conducteur {} refusée : email déjà utilisé '{}'", id, request.email());
            throw new BusinessRuleException("Cet email est déjà utilisé par un autre conducteur");
        }

        // R-02: Permis unique (sauf si c'est son propre permis actuel)
        if (conducteurRepository.existsByNumeroPermisIgnoreCaseAndIdNot(request.numeroPermis(), id)) {
            log.warn("Modification conducteur {} refusée : permis déjà utilisé '{}'", id, request.numeroPermis());
            throw new BusinessRuleException("Ce numéro de permis est déjà utilisé par un autre conducteur");
        }

        conducteur.updateProfile(request.nom(), request.prenom(), request.email(), request.telephone(), request.numeroPermis());
        log.info("Conducteur avec l'id {} mis à jour avec succès", id);
        return conducteurMapper.toResponse(conducteur);
    }

    @Override
    public List<ConducteurResponse> search(String nom, String prenom, String email) {
        boolean hasNom = nom != null && !nom.isBlank();
        boolean hasPrenom = prenom != null && !prenom.isBlank();
        boolean hasEmail = email != null && !email.isBlank();

        if (!hasNom && !hasPrenom && !hasEmail) {
            log.warn("Recherche de conducteur sans paramètre");
            throw new InvalidRequestException("Au moins un paramètre de recherche ('nom', 'prenom' ou 'email') doit être fourni");
        }

        String searchNom = hasNom ? "%" + nom.trim().toLowerCase() + "%" : null;
        String searchPrenom = hasPrenom ? "%" + prenom.trim().toLowerCase() + "%" : null;
        String searchEmail = hasEmail ? "%" + email.trim().toLowerCase() + "%" : null;

        List<Conducteur> results = conducteurRepository.search(searchNom, searchPrenom, searchEmail);
        return conducteurMapper.toResponseList(results);
    }
}
