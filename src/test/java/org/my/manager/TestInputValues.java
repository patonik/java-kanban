package org.my.manager;

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
}
