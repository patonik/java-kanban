package org.my.manager;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

public interface TestInputValues {
    List<String> LEVEL_1_NAMES = List.of(
            "Establish Base Camp",
            "Gather Supplies",
            "Secure Resources",
            "Establish Farming Practices",
            "Strengthen Defenses",
            "Establish Trading Network");
    List<List<String>> LEVEL_2_NAMES = List.of(
            List.of("Secure Perimeter", "Set Up Communication Center"),
            List.of("Scavenge for Food", "Collect Medical Supplies"),
            List.of("Secure Water Source", "Gather Firewood"),
            List.of("Prepare Soil for Planting", "Plant Crops"),
            List.of("Set Up Watchtowers", "Develop Early Warning System"),
            List.of("Identify Nearby Survivor Groups", "Trade for Essential Supplies")
    );
    List<String> LEVEL_1_DESCRIPTIONS = List.of(
            "Secure the initial location for survival",
            "Collect essential resources for survival",
            "Ensure a sustainable supply of water and other essentials",
            "Develop a sustainable farming system for long-term survival",
            "Enhance the security measures to protect the group",
            "Connect with other survivor groups for resource exchange"
    );
    List<List<String>> LEVEL_2_DESCRIPTIONS = List.of(
            List.of("Strengthen the defense of the base", "Establish a communication hub for coordination"),
            List.of("Search for food sources in the surrounding area", "Gather medical resources for emergencies"),
            List.of("Establish a reliable water source", "Collect firewood for cooking and warmth"),
            List.of("Get the soil ready for planting crops", "Begin cultivating and planting crops"),
            List.of("Establish watchtowers for surveillance", "Create a system to detect and alert to potential threats"),
            List.of("Locate and assess nearby groups", "Exchange goods and resources with other groups")
    );
    List<LocalDateTime> LEVEL_1_START_DATE_TIMES = List.of(
            //put into tasks
            LocalDateTime.of(2024, 2, 20, 1, 0),
            LocalDateTime.of(2024, 2, 20, 1, 45),
            LocalDateTime.of(2024, 2, 23, 12, 0),
            //ignored
            LocalDateTime.of(2024, 3, 1, 23, 0),
            LocalDateTime.of(2025, 12, 31, 23, 45),
            LocalDateTime.of(2026, 2, 1, 0, 0)
    );
    List<List<LocalDateTime>> LEVEL_2_START_DATE_TIMES = List.of(
            // ignored
            List.of(LocalDateTime.of(2024, 2, 20, 1, 0),
                    LocalDateTime.of(2024, 2, 20, 1, 30)),
            List.of(LocalDateTime.of(2024, 2, 20, 1, 45),
                    LocalDateTime.of(2024, 2, 22, 1, 30)),
            List.of(LocalDateTime.of(2024, 2, 23, 12, 0),
                    LocalDateTime.of(2024, 2, 25, 1, 30)),
            //put into epics
            List.of(LocalDateTime.of(2024, 3, 1, 23, 0),
                    LocalDateTime.of(2024, 8, 20, 1, 30)),
            List.of(LocalDateTime.of(2025, 12, 31, 23, 45),
                    LocalDateTime.of(2026, 1, 1, 0, 0)),
            List.of(LocalDateTime.of(2026, 2, 1, 0, 0),
                    LocalDateTime.of(2027, 1, 1, 0, 0))
    );
    List<Duration> LEVEL_1_DURATION = List.of(
            Duration.between(LEVEL_1_START_DATE_TIMES.get(0),
                    LEVEL_1_START_DATE_TIMES.get(1)),
            Duration.between(LEVEL_1_START_DATE_TIMES.get(1),
                    LEVEL_1_START_DATE_TIMES.get(2)),
            Duration.between(LEVEL_1_START_DATE_TIMES.get(2),
                    LEVEL_1_START_DATE_TIMES.get(3)),
            Duration.between(LEVEL_1_START_DATE_TIMES.get(3),
                    LEVEL_1_START_DATE_TIMES.get(4)),
            Duration.between(LEVEL_1_START_DATE_TIMES.get(4),
                    LEVEL_1_START_DATE_TIMES.get(5)),
            Duration.of(10, ChronoUnit.YEARS)
    );
    List<List<Duration>> LEVEL_2_DURATION = List.of(
            List.of(Duration.between(LEVEL_2_START_DATE_TIMES.get(0).get(0), LEVEL_2_START_DATE_TIMES.get(0).get(1)),
                    Duration.between(LEVEL_2_START_DATE_TIMES.get(0).get(1), LEVEL_2_START_DATE_TIMES.get(1).get(0))
            ),
            List.of(Duration.between(LEVEL_2_START_DATE_TIMES.get(1).get(0), LEVEL_2_START_DATE_TIMES.get(1).get(1)),
                    Duration.between(LEVEL_2_START_DATE_TIMES.get(1).get(1), LEVEL_2_START_DATE_TIMES.get(2).get(0))
            ),
            List.of(Duration.between(LEVEL_2_START_DATE_TIMES.get(2).get(0), LEVEL_2_START_DATE_TIMES.get(2).get(1)),
                    Duration.between(LEVEL_2_START_DATE_TIMES.get(2).get(1), LEVEL_2_START_DATE_TIMES.get(3).get(0))
            ),
            List.of(Duration.between(LEVEL_2_START_DATE_TIMES.get(3).get(0), LEVEL_2_START_DATE_TIMES.get(3).get(1)),
                    Duration.between(LEVEL_2_START_DATE_TIMES.get(3).get(1), LEVEL_2_START_DATE_TIMES.get(4).get(0))
            ),
            List.of(Duration.between(LEVEL_2_START_DATE_TIMES.get(4).get(0), LEVEL_2_START_DATE_TIMES.get(4).get(1)),
                    Duration.between(LEVEL_2_START_DATE_TIMES.get(4).get(1), LEVEL_2_START_DATE_TIMES.get(5).get(0))
            ),
            List.of(Duration.between(LEVEL_2_START_DATE_TIMES.get(5).get(0), LEVEL_2_START_DATE_TIMES.get(5).get(1)),
                    LEVEL_1_DURATION.get(5).minus(Duration.between(LEVEL_2_START_DATE_TIMES.get(5).get(0), LEVEL_2_START_DATE_TIMES.get(5).get(1)))
            )
    );
}
