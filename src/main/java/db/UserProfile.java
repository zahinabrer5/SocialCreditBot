package db;

import org.jetbrains.annotations.NotNull;

import java.math.BigInteger;

public record UserProfile(String id, BigInteger balance, int numGain, int numLoss) implements Comparable<UserProfile> {
    @Override
    public int compareTo(@NotNull UserProfile o) {
        return o.balance().compareTo(this.balance);
    }
}
