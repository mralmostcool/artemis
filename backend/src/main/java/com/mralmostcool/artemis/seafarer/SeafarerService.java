package com.mralmostcool.artemis.seafarer;

import com.mralmostcool.artemis.seafarer.internal.model.IndosMaster;
import com.mralmostcool.artemis.seafarer.internal.model.ProfileIndosMapping;
import com.mralmostcool.artemis.seafarer.internal.model.RankMaster;
import com.mralmostcool.artemis.seafarer.internal.model.SeafarerMedical;
import com.mralmostcool.artemis.seafarer.internal.repository.IndosMasterRepository;
import com.mralmostcool.artemis.seafarer.internal.repository.ProfileIndosMappingRepository;
import com.mralmostcool.artemis.seafarer.internal.repository.RankMasterRepository;
import com.mralmostcool.artemis.seafarer.internal.repository.SeafarerMedicalRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class SeafarerService {

    private final RankMasterRepository rankRepository;
    private final IndosMasterRepository indosRepository;
    private final ProfileIndosMappingRepository mappingRepository;
    private final SeafarerMedicalRepository medicalRepository;

    public SeafarerService(RankMasterRepository rankRepository,
                           IndosMasterRepository indosRepository,
                           ProfileIndosMappingRepository mappingRepository,
                           SeafarerMedicalRepository medicalRepository) {
        this.rankRepository = rankRepository;
        this.indosRepository = indosRepository;
        this.mappingRepository = mappingRepository;
        this.medicalRepository = medicalRepository;
    }

    public List<RankMaster> getAllRanks() {
        return rankRepository.findAll();
    }

    @Transactional
    public RankMaster createRank(RankMaster rank) {
        if (rank.getName() == null || rank.getName().isBlank()) {
            throw new IllegalArgumentException("Rank name is mandatory");
        }
        if (rank.getLevel() == null) {
            throw new IllegalArgumentException("Rank level is mandatory");
        }
        java.util.Optional<RankMaster> existing = rankRepository.findAll().stream()
                .filter(r -> r.getName().equalsIgnoreCase(rank.getName()))
                .findFirst();
        if (existing.isPresent()) {
            return existing.get();
        }
        return rankRepository.save(rank);
    }

    @Transactional
    public RankMaster updateRank(UUID rankId, RankMaster updated) {
        RankMaster rank = rankRepository.findById(rankId)
                .orElseThrow(() -> new IllegalArgumentException("Rank not found"));
        if (updated.getName() != null && !updated.getName().isBlank()) {
            rank.setName(updated.getName());
        }
        if (updated.getLevel() != null) {
            rank.setLevel(updated.getLevel());
        }
        return rankRepository.save(rank);
    }

    @Transactional
    public void deleteRank(UUID rankId) {
        rankRepository.deleteById(rankId);
    }

    public List<IndosMaster> querySeafarers(String indos, String firstName) {
        if (indos != null && !indos.isBlank()) {
            return indosRepository.findByIndos(indos).map(List::of).orElse(List.of());
        }
        if (firstName != null && !firstName.isBlank()) {
            return indosRepository.findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(firstName, firstName);
        }
        return indosRepository.findAll();
    }

    public Optional<IndosMaster> getIndosRecord(UUID id) {
        return indosRepository.findById(id);
    }

    public Optional<IndosMaster> getIndosByNumber(String indos) {
        return indosRepository.findByIndos(indos);
    }

    @Transactional
    public IndosMaster createIndosRecord(IndosMaster indosMaster) {
        if (indosMaster.getIndos() == null || indosMaster.getIndos().length() != 7) {
            throw new IllegalArgumentException("INDoS must be exactly 7 characters");
        }
        if (indosMaster.getFirstName() == null || indosMaster.getFirstName().isBlank()) {
            throw new IllegalArgumentException("First name is mandatory");
        }
        if (indosMaster.getDateOfBirth() == null) {
            throw new IllegalArgumentException("Date of birth is mandatory");
        }
        if (indosMaster.getRank() == null || indosMaster.getRank().getId() == null) {
            throw new IllegalArgumentException("Rank detail is mandatory");
        }
        java.util.Optional<IndosMaster> existing = indosRepository.findByIndos(indosMaster.getIndos());
        if (existing.isPresent()) {
            return existing.get();
        }
        RankMaster rank = rankRepository.findById(indosMaster.getRank().getId())
                .orElseThrow(() -> new IllegalArgumentException("Rank not found"));
        indosMaster.setRank(rank);
        return indosRepository.save(indosMaster);
    }

    @Transactional
    public IndosMaster updateIndosRecord(UUID id, IndosMaster updated) {
        IndosMaster record = indosRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("INDoS Master record not found"));
        if (updated.getFirstName() != null && !updated.getFirstName().isBlank()) {
            record.setFirstName(updated.getFirstName());
        }
        if (updated.getLastName() != null) {
            record.setLastName(updated.getLastName());
        }
        if (updated.getPassportNo() != null) {
            record.setPassportNo(updated.getPassportNo().isBlank() ? null : updated.getPassportNo());
        }
        if (updated.getCdcNo() != null) {
            record.setCdcNo(updated.getCdcNo().isBlank() ? null : updated.getCdcNo());
        }
        if (updated.getRank() != null && updated.getRank().getId() != null) {
            RankMaster rank = rankRepository.findById(updated.getRank().getId())
                    .orElseThrow(() -> new IllegalArgumentException("Rank not found"));
            record.setRank(rank);
        }
        return indosRepository.save(record);
    }

    @Transactional
    public IndosMaster toggleIndosStatus(UUID id, boolean active) {
        IndosMaster record = indosRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("INDoS Master record not found"));
        record.setActive(active);
        return indosRepository.save(record);
    }

    public Optional<ProfileIndosMapping> getLink(UUID profileId) {
        return mappingRepository.findByProfileId(profileId);
    }

    @Transactional
    public ProfileIndosMapping linkProfile(UUID profileId, String indosNum) {
        IndosMaster indos = indosRepository.findByIndos(indosNum)
                .orElseThrow(() -> new IllegalArgumentException("INDoS number not found in registry"));
        
        Optional<ProfileIndosMapping> existing = mappingRepository.findByProfileId(profileId);
        if (existing.isPresent()) {
            throw new IllegalArgumentException("Profile is already linked to another INDoS number");
        }

        ProfileIndosMapping mapping = ProfileIndosMapping.builder()
                .profileId(profileId)
                .indosMaster(indos)
                .build();
        return mappingRepository.save(mapping);
    }

    @Transactional
    public void unlinkProfile(UUID profileId) {
        mappingRepository.deleteById(profileId);
    }

    public List<SeafarerMedical> getMedicalRecords(UUID indosId) {
        return medicalRepository.findByIndosMasterIdOrderByExaminationDateDesc(indosId);
    }

    @Transactional
    public SeafarerMedical createMedicalRecord(UUID indosId, SeafarerMedical medical) {
        IndosMaster indos = indosRepository.findById(indosId)
                .orElseThrow(() -> new IllegalArgumentException("INDoS record not found"));
        
        if (medical.getDoctorName() == null || medical.getDoctorName().isBlank()) {
            throw new IllegalArgumentException("Doctor name is mandatory");
        }
        if (medical.getDoctorRegistrationNo() == null || medical.getDoctorRegistrationNo().isBlank()) {
            throw new IllegalArgumentException("Doctor registration number is mandatory");
        }
        if (medical.getExaminationDate() == null || medical.getExpiryDate() == null) {
            throw new IllegalArgumentException("Examination and expiry dates are mandatory");
        }
        if (medical.getExpiryDate().isBefore(medical.getExaminationDate())) {
            throw new IllegalArgumentException("Expiry date must be after examination date");
        }

        medical.setIndosMaster(indos);
        return medicalRepository.save(medical);
    }
}
