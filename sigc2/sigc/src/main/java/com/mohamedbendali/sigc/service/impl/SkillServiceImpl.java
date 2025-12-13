package com.mohamedbendali.sigc.service.impl;

import com.mohamedbendali.sigc.dto.SkillDTO;
import com.mohamedbendali.sigc.entity.Skill;
import com.mohamedbendali.sigc.exception.DuplicateResourceException;
import com.mohamedbendali.sigc.exception.OperationNotAllowedException;
import com.mohamedbendali.sigc.exception.ResourceNotFoundException;
import com.mohamedbendali.sigc.repository.SkillRepository;
import com.mohamedbendali.sigc.service.SkillService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class SkillServiceImpl implements SkillService {

    private final SkillRepository skillRepository;

    @Override
    @Transactional(readOnly = true)
    public List<SkillDTO> findAllSkillsAsList() {
        log.debug("Fetching all skills as a list");
        return skillRepository.findAll(Sort.by(Sort.Direction.ASC, "name")) // Trier par nom par ex.
                .stream()
                .map(this::convertToDto) // Utiliser votre mapper
                .collect(Collectors.toList());
    }
    @Override
    public SkillDTO createSkill(SkillDTO skillDto) {
        log.debug("Attempting to create skill: {}", skillDto.getName());
        skillRepository.findByNameIgnoreCase(skillDto.getName()).ifPresent(s -> {
            throw new DuplicateResourceException("Skill", "name", skillDto.getName());
        });
        Skill skill = convertToEntity(skillDto);
        Skill savedSkill = skillRepository.save(skill);
        log.info("Skill created successfully with ID: {}", savedSkill.getId());
        return convertToDto(savedSkill);
    }

    @Override
    public SkillDTO updateSkill(Long id, SkillDTO skillDto) {
        log.debug("Attempting to update skill ID: {}", id);
        Skill skill = skillRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Skill", "id", id));

        // Vérifier si le nouveau nom existe déjà (sauf si c'est le même skill)
        skillRepository.findByNameIgnoreCase(skillDto.getName()).ifPresent(existing -> {
            if (!existing.getId().equals(id)) {
                throw new DuplicateResourceException("Skill", "name", skillDto.getName());
            }
        });

        skill.setName(skillDto.getName());
        skill.setCategory(skillDto.getCategory());
        Skill updatedSkill = skillRepository.save(skill);
        log.info("Skill updated successfully with ID: {}", updatedSkill.getId());
        return convertToDto(updatedSkill);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SkillDTO> findById(Long id) {
        log.debug("Fetching skill by ID: {}", id);
        return skillRepository.findById(id).map(this::convertToDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SkillDTO> findByName(String name) {
        log.debug("Fetching skill by name: {}", name);
        return skillRepository.findByNameIgnoreCase(name).map(this::convertToDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SkillDTO> findAll(Pageable pageable) {
        log.debug("Fetching all skills, page: {}, size: {}", pageable.getPageNumber(), pageable.getPageSize());
        return skillRepository.findAll(pageable).map(this::convertToDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SkillDTO> searchSkills(String name) {
        log.debug("Searching skills with name containing: {}", name);
        // Utiliser une méthode de repository comme findByNameContainingIgnoreCase si définie
        // Pour l'instant, on filtre en mémoire (pas idéal pour de gros volumes)
        return skillRepository.findAll().stream()
                .filter(skill -> skill.getName().toLowerCase().contains(name.toLowerCase()))
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }


    @Override
    public void deleteSkill(Long id) {
        log.debug("Attempting to delete skill ID: {}", id);
        Skill skill = skillRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Skill", "id", id));
        // Attention: La suppression échouera si la compétence est utilisée (liée à des candidats/offres)
        // sauf si les relations ManyToMany sont configurées avec CascadeType.REMOVE (non recommandé)
        // ou si on délie manuellement d'abord.
        // Pour l'instant, on essaie de supprimer directement.
        try {
            skillRepository.delete(skill);
            log.info("Skill deleted successfully with ID: {}", id);
        } catch (Exception e) {
            log.error("Could not delete skill ID {}. It might be in use.", id, e);
            // Relancer une exception métier plus spécifique ?
            throw new OperationNotAllowedException("Impossible de supprimer la compétence ID " + id + ". Elle est peut-être utilisée par des candidats ou des offres.", e);
        }
    }

    // --- Mapping Helpers ---
    private SkillDTO convertToDto(Skill skill) {
        if (skill == null) return null;
        return new SkillDTO(skill.getId(), skill.getName(), skill.getCategory());
    }

    private Skill convertToEntity(SkillDTO dto) {
        if (dto == null) return null;
        Skill skill = new Skill();
        skill.setId(dto.getId()); // Peut être null pour la création
        skill.setName(dto.getName());
        skill.setCategory(dto.getCategory());
        return skill;
    }
}