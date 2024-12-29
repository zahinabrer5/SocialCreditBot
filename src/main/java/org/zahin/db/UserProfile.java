package org.zahin.db;

import org.jetbrains.annotations.NotNull;

import java.math.BigInteger;
import java.time.LocalDate;

public record UserProfile(String id, BigInteger balance, int numGain, int numLoss,
                          LocalDate lastDaily) implements Comparable<UserProfile> {
    @Override
    public int compareTo(@NotNull UserProfile o) {
        return o.balance().compareTo(this.balance);
    }
}
