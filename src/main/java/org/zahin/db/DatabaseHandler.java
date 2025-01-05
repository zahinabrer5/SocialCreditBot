package org.zahin.db;

import org.simplejavamail.api.email.Email;
import org.simplejavamail.api.mailer.Mailer;
import org.simplejavamail.email.EmailBuilder;
import org.zahin.Bot;
import org.zahin.util.Util;

import java.io.*;
import java.math.BigInteger;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class DatabaseHandler {
    private final File userTableFile;
    private final File verificationTableFile;
    private final Map<String, UserProfile> userTable = new HashMap<>();
    private final Map<String, VerificationData> verificationTable = new HashMap<>();
    private final Mailer mailer;

    public DatabaseHandler(Mailer mailer) {
        this.mailer = mailer;

        userTableFile = new File(Bot.dotenv.get("USER_TABLE_FILE"));
        verificationTableFile = new File(Bot.dotenv.get("VERIFICATION_TABLE_FILE"));
    }

    public void loadDatabase() {
        try (BufferedReader br = new BufferedReader(new FileReader(userTableFile))) {
            br.readLine();
            for (String line; (line = br.readLine()) != null; ) {
                String[] splitted = line.split(",");
                String id = splitted[0];
                BigInteger balance = new BigInteger(splitted[1]);
                int numGain = Integer.parseInt(splitted[2]);
                int numLoss = Integer.parseInt(splitted[3]);
                LocalDate lastDaily = LocalDate.parse(splitted[4]);
                LocalDate lastRob = LocalDate.parse(splitted[5]);
                int numRobs = Integer.parseInt(splitted[6]);
                UserProfile profile = new UserProfile(id, balance, numGain, numLoss, lastDaily, lastRob, numRobs);
                userTable.put(id, profile);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try (BufferedReader br = new BufferedReader(new FileReader(verificationTableFile))) {
            br.readLine();
            for (String line; (line = br.readLine()) != null; ) {
                String[] splitted = line.split(",");
                String id = splitted[0];
                String schoolEmail = splitted[1];
                String code = splitted[2];

                VerificationData profile = new VerificationData(id, schoolEmail, code);
                verificationTable.put(id, profile);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void saveDatabase() {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(userTableFile, false))) {
            bw.write("User ID,Balance,Number of Gains,Number of Losses,Last date /daily used,Last date /rob used,Number of robberies");
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
                bw.write(",");
                bw.write(String.valueOf(profile.numRobs()));
                bw.newLine();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(verificationTableFile, false))) {
            bw.write("User ID,School Email,Verification Code");
            bw.newLine();
            for (Map.Entry<String, VerificationData> entry : verificationTable.entrySet()) {
                String id = entry.getKey();
                VerificationData profile = entry.getValue();
                bw.write(id);
                bw.write(",");
                bw.write(profile.schoolEmail());
                bw.write(",");
                bw.write(profile.code());
                bw.newLine();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public UserProfile read(String id) {
        if (!userTable.containsKey(id)) {
            LocalDate yesterday = LocalDate.now().minusDays(1);
            UserProfile profile = new UserProfile(id, BigInteger.valueOf(100), 0, 0, yesterday, yesterday, 0);
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
        UserProfile updatedProfile = new UserProfile(id, balance, numGain, numLoss, profile.lastDaily(), profile.lastRob(), profile.numRobs());
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
                new UserProfile(id, profile.balance(), profile.numGain(), profile.numLoss(), date, profile.lastRob(), profile.numRobs()));
    }

    public LocalDate getLastRobUse(String userId) {
        read(userId);
        return userTable.get(userId).lastRob();
    }

    public void setLastRobUse(String userId, LocalDate date) {
        read(userId);
        userTable.computeIfPresent(userId, (id, profile) ->
                new UserProfile(id, profile.balance(), profile.numGain(), profile.numLoss(), profile.lastDaily(), date, profile.numRobs() + 1));
    }

    public int getNumRobs(String userId) {
        read(userId);
        return userTable.get(userId).numRobs();
    }

    public UserProfile removeUserRow(String userId) {
        read(userId);
        UserProfile profile = userTable.remove(userId);
        saveDatabase();
        return profile;
    }

    public void saveVerifCode(String id, String schoolEmail) {
        if (verificationTable.containsKey(id))
            return;

        String code = Util.randAlphaNum(8, Bot.rand);
        for (VerificationData datum : verificationTable.values()) {
            if (datum.schoolEmail().equals(schoolEmail))
                return;
            if (datum.code().equals(code))
                code = Util.randAlphaNum(8, Bot.rand);
        }

        Email email = EmailBuilder.startingBlank()
                .to(schoolEmail)
                .withSubject("OC STEM Discord Verification Code")
                .withHTMLText(String.format("<h1>%s</h1><p>is your verification code for the OC STEM Discord server.</p>", code))
                .buildEmail();
        mailer.sendMail(email);

        VerificationData data = new VerificationData(id, schoolEmail, code);
        verificationTable.put(id, data);
    }

    public boolean matchVerifCode(String id, String givenCode) {
        for (Map.Entry<String, VerificationData> entry : verificationTable.entrySet()) {
            String currId = entry.getKey();
            String currCode = entry.getValue().code();

            if (currId.equals(id) && currCode.equals(givenCode)) {
                verificationTable.remove(id);
                return true;
            }
        }
        return false;
    }
}
