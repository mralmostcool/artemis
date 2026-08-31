package com.mralmostcool.artemis.vessel;

import com.mralmostcool.artemis.seafarer.SeafarerService;
import com.mralmostcool.artemis.seafarer.internal.model.IndosMaster;
import com.mralmostcool.artemis.seafarer.internal.model.RankMaster;
import com.mralmostcool.artemis.vessel.internal.model.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class VesselIntegrationTests {

    @Autowired
    private VesselService vesselService;

    @Autowired
    private SeafarerService seafarerService;

    @Test
    @Transactional
    void testVesselBerthStayConcessionCrewManifestFlow() {
        Company company = Company.builder()
                .name("Artemis Shipping Ltd")
                .rpslNo("RPSL-MUM-9921")
                .rpslValidUntil(LocalDate.of(2030, 12, 31))
                .isActive(true)
                .build();
        company = vesselService.createCompany(company);
        assertThat(company.getId()).isNotNull();

        Vessel vessel = Vessel.builder()
                .name("Artemis Voyager")
                .imo("IMO998822")
                .flag("Indian")
                .vesselType("Bulk Carrier")
                .callSign("VTAV1")
                .isActive(true)
                .build();
        vessel = vesselService.createVessel(company.getId(), vessel);
        assertThat(vessel.getId()).isNotNull();

        Berth berth = Berth.builder()
                .berthName("JNPT Berth 5")
                .maxDraftMeters(14.50)
                .maxLoaMeters(300.00)
                .isActive(true)
                .build();
        berth = vesselService.createBerth(berth);
        assertThat(berth.getId()).isNotNull();

        // Stay scheduling (Minimum 1 year stay check validation)
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime endOfStay = now.plusYears(1).plusDays(5);
        BerthAllocation stay = vesselService.allocateBerthToVessel(berth.getId(), vessel.getId(), now, endOfStay);
        assertThat(stay.getId()).isNotNull();

        // Cadet training requests
        TrainingBerthRequest req = TrainingBerthRequest.builder().requestedSlots(2).build();
        req = vesselService.createTrainingBerthRequest(vessel.getId(), req);
        assertThat(req.getId()).isNotNull();
        assertThat(req.getStatus()).isEqualTo("PENDING");

        req = vesselService.approveTrainingBerthRequest(req.getId(), 2, 8.50);
        assertThat(req.getStatus()).isEqualTo("APPROVED");
        assertThat(req.getConcessionRatePerDayUsd()).isEqualTo(8.50);

        // Seed Seafarer
        RankMaster rank = RankMaster.builder().name("Deck Cadet Officer").level(1).build();
        rank = seafarerService.createRank(rank);

        IndosMaster indos = IndosMaster.builder()
                .indos("77ZZ991")
                .firstName("Bob")
                .lastName("Jones")
                .rank(rank)
                .dateOfBirth(LocalDate.of(2001, 8, 12))
                .gender("MALE")
                .nationality("Indian")
                .build();
        indos = seafarerService.createIndosRecord(indos);

        // Allocate seafarer to berth stay
        BerthSeafarerAllocation alloc = vesselService.allocateBerthToSeafarer(
                berth.getId(),
                indos.getId(),
                stay.getId(),
                now,
                now.plusDays(120)
        );
        assertThat(alloc.getId()).isNotNull();

        // Log concessions cadet credits
        ConcessionLedger ledger = vesselService.logConcession(alloc.getId(), 120, 8.50);
        assertThat(ledger.getId()).isNotNull();
        assertThat(ledger.getConcessionValueUsd()).isEqualTo(120 * 8.50);

        List<ConcessionLedger> list = vesselService.getConcessionsByCompany(company.getId());
        assertThat(list).isNotEmpty();

        // Crew manifest list output check
        String crewListJson = vesselService.generateImoCrewListJson(vessel.getId());
        assertThat(crewListJson).contains("77ZZ991");
        assertThat(crewListJson).contains("Bob");
    }
}
