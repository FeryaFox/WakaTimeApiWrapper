package ru.feryafox.wakatimewrapper.enums;

import java.util.Arrays;
import java.util.stream.Collectors;

public enum Scope {
    // Read summaries
    READ_SUMMARIES("read_summaries"),
    READ_SUMMARIES_CATEGORIES("read_summaries.categories"),
    READ_SUMMARIES_DEPENDENCIES("read_summaries.dependencies"),
    READ_SUMMARIES_EDITORS("read_summaries.editors"),
    READ_SUMMARIES_LANGUAGES("read_summaries.languages"),
    READ_SUMMARIES_MACHINES("read_summaries.machines"),
    READ_SUMMARIES_OPERATING_SYSTEMS("read_summaries.operating_systems"),
    READ_SUMMARIES_PROJECTS("read_summaries.projects"),

    // Read stats
    READ_STATS("read_stats"),
    READ_STATS_BEST_DAY("read_stats.best_day"),
    READ_STATS_CATEGORIES("read_stats.categories"),
    READ_STATS_DEPENDENCIES("read_stats.dependencies"),
    READ_STATS_EDITORS("read_stats.editors"),
    READ_STATS_LANGUAGES("read_stats.languages"),
    READ_STATS_MACHINES("read_stats.machines"),
    READ_STATS_OPERATING_SYSTEMS("read_stats.operating_systems"),
    READ_STATS_PROJECTS("read_stats.projects"),

    // Other scopes
    READ_GOALS("read_goals"),
    READ_ORGS("read_orgs"),
    WRITE_ORGS("write_orgs"),
    READ_PRIVATE_LEADERBOARDS("read_private_leaderboards"),
    WRITE_PRIVATE_LEADERBOARDS("write_private_leaderboards"),
    READ_HEARTBEATS("read_heartbeats"),
    WRITE_HEARTBEATS("write_heartbeats"),
    EMAIL("email");

    private final String value;

    Scope(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static String scopesToString(Scope[] scopes) {
        return Arrays.stream(scopes)
                .map(Scope::getValue)
                .collect(Collectors.joining(","));
    }
}
