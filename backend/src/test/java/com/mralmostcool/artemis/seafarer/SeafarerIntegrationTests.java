package com.mralmostcool.artemis.seafarer;

import com.mralmostcool.artemis.seafarer.internal.model.IndosMaster;
import com.mralmostcool.artemis.seafarer.internal.model.RankMaster;
import com.mralmostcool.artemis.seafarer.internal.model.SeafarerMedical;
import com.mralmostcool.artemis.seafarer.internal.repository.IndosMasterRepository;
import com.mralmostcool.artemis.seafarer.internal.repository.RankMasterRepository;
import com.mralmostcool.artemis.seafarer.internal.repository.SeafarerMedicalRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class SeafarerIntegrationTests {

    @Autowired
    private SeafarerService seafarerService;

    @Autowired
    private RankMasterRepository rankRepository;

    @Autowired
    private IndosMasterRepository indosRepository;

    @Autowired
    private SeafarerMedicalRepository medicalRepository;

    @Test
    @Transactional
    void testRankAndIndosOperations() {
        RankMaster rank = RankMaster.builder()
                .name("Cadet Officer")
                .level(1)
                .build();
        rank = seafarerService.createRank(rank);
        assertThat(rank.getId()).isNotNull();

        IndosMaster record = IndosMaster.builder()
                .indos("99ZZ123")
                .firstName("Robert")
                .lastName("Bruce")
                .rank(rank)
                .dateOfBirth(LocalDate.of(1998, 6, 15))
                .gender("MALE")
                .nationality("Indian")
                .isActive(true)
                .build();

        IndosMaster saved = seafarerService.createIndosRecord(record);
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getIndos()).isEqualTo("99ZZ123");

        SeafarerMedical medical = SeafarerMedical.builder()
                .doctorName("Dr. Jane Watson")
                .doctorRegistrationNo("DR-99231")
                .examinationDate(LocalDate.of(2026, 8, 1))
                .expiryDate(LocalDate.of(2027, 8, 1))
                .isFit(true)
                .remarks("Healthy")
                .build();

        SeafarerMedical savedMedical = seafarerService.createMedicalRecord(saved.getId(), medical);
        assertThat(savedMedical.getId()).isNotNull();
        assertThat(savedMedical.isFit()).isTrue();

        List<SeafarerMedical> records = seafarerService.getMedicalRecords(saved.getId());
        assertThat(records).isNotEmpty();
        assertThat(records.get(0).getDoctorName()).isEqualTo("Dr. Jane Watson");
    }
}
