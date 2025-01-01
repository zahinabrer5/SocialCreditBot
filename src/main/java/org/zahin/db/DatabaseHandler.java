package org.zahin.db;

import org.zahin.util.Util;

import java.io.*;
import java.math.BigInteger;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class DatabaseHandler {
    private final File databaseFile;
    private final Map<String, UserProfile> userTable = new HashMap<>();

    public DatabaseHandler(String databaseFile) {
        this.databaseFile = new File(databaseFile);
    }

    public void loadDatabase() {
        try (BufferedReader br = new BufferedReader(new FileReader(databaseFile))) {
            br.readLine();
            for (String line; (line = br.readLine()) != null; ) {
                String[] splitted = line.split(",");
                String id = splitted[0];
                BigInteger balance = new BigInteger(splitted[1]);
                int numGain = Integer.parseInt(splitted[2]);
                int numLoss = Integer.parseInt(splitted[3]);
                LocalDate lastDaily = LocalDate.parse(splitted[4]);
                LocalDate lastRob = LocalDate.parse(splitted[5]);
                UserProfile profile = new UserProfile(id, balance, numGain, numLoss, lastDaily, lastRob);
                userTable.put(id, profile);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void saveDatabase() {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(databaseFile, false))) {
            bw.write("User ID,Balance,Number of Gains,Number of Losses,Last date /daily used");
            bw.newLine();
            for (Map.Entry<String, UserProfile> entry : userTable.entrySet()) {
                String id = entry.getKey();
                UserProfile profile = entry.getValue();
                bw.write(id);
                bw.write(",");
                bw.write(profile.balance().toString());
                bw.write(",");
                bw.write(String.valueOf(profile.numGain()));
                bw.write(",");
                bw.write(String.valueOf(profile.numLoss()));
                bw.write(",");
                bw.write(profile.lastDaily().toString());
                bw.write(",");
                bw.write(profile.lastRob().toString());
                bw.newLine();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public UserProfile read(String id) {
        if (!userTable.containsKey(id)) {
            LocalDate yesterday = LocalDate.now().minusDays(1);
            UserProfile profile = new UserProfile(id, BigInteger.valueOf(100), 0, 0, yesterday, yesterday);
            userTable.put(id, profile);
        }
        return userTable.get(id);
    }

    public void update(String id, BigInteger amount) {
        UserProfile profile = read(id);
        BigInteger balance = profile.balance().add(amount);
        int numGain = profile.numGain();
        int numLoss = profile.numLoss();
        if (amount.signum() > 0)
            numGain++;
        else
            numLoss++;
        UserProfile updatedProfile = new UserProfile(id, balance, numGain, numLoss, profile.lastDaily(), profile.lastRob());
        userTable.put(id, updatedProfile);
        saveDatabase();
    }

    public Map<String, BigInteger> getRanking() {
        Map<String, UserProfile> sorted = Util.sortByValue(userTable);
        Map<String, BigInteger> result = new LinkedHashMap<>();
        for (Map.Entry<String, UserProfile> entry : sorted.entrySet())
            result.put(entry.getKey(), entry.getValue().balance());
        return result;
    }

    public LocalDate getLastDailyUse(String userId) {
        read(userId);
        return userTable.get(userId).lastDaily();
    }

    public void setLastDailyUse(String userId, LocalDate date) {
        read(userId);
        userTable.computeIfPresent(userId, (id, profile) ->
                new UserProfile(id, profile.balance(), profile.numGain(), profile.numLoss(), date, profile.lastRob()));
    }

    public LocalDate getLastRobUse(String userId) {
        read(userId);
        return userTable.get(userId).lastRob();
    }

    public void setLastRobUse(String userId, LocalDate date) {
        read(userId);
        userTable.computeIfPresent(userId, (id, profile) ->
                new UserProfile(id, profile.balance(), profile.numGain(), profile.numLoss(), profile.lastDaily(), date));
    }
}
