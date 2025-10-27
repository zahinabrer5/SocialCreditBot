package org.zahin.util;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.List;
import java.util.Map.Entry;

public class Util {
    public static NumberFormat plusMinusNumFmt = new DecimalFormat("+#;-#");
    public static DecimalFormat twoDecFmt = new DecimalFormat("#.##");
    public static DecimalFormat oneDecFmt = new DecimalFormat("#.#");

    public static String thousandsSep(long n) {
        return thousandsSep(n, ' ');
    }

    public static String thousandsSep(long n, char sep) {
        return String.format(Locale.US, "%,d", n).replace(',', sep);
    }

    public static String pluralizer(String singular, String plural, long amount) {
        return amount == 1 ? singular : plural;
    }

    // https://stackoverflow.com/a/2581754
    public static <K, V extends Comparable<? super V>> Map<K, V> sortByValue(Map<K, V> map) {
        List<Entry<K, V>> list = new ArrayList<>(map.entrySet());
        list.sort(Entry.comparingByValue());

        Map<K, V> result = new LinkedHashMap<>();
        for (Entry<K, V> entry : list)
            result.put(entry.getKey(), entry.getValue());

        return result;
    }

    public static Image urlToImage(String url) {
        try {
            return ImageIO.read(new URI(url).toURL());
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    // https://stackoverflow.com/a/13605411/21405641

    /**
     * Converts a given Image into a BufferedImage
     *
     * @param img The Image to be converted
     * @return The converted BufferedImage
     */
    public static BufferedImage toBufferedImage(Image img) {
        if (img instanceof BufferedImage)
            return (BufferedImage) img;

        // Create a buffered image with transparency
        BufferedImage bimage = new BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_INT_ARGB);

        // Draw the image on to the buffered image
        Graphics2D bGr = bimage.createGraphics();
        bGr.drawImage(img, 0, 0, null);
        bGr.dispose();

        // Return the buffered image
        return bimage;
    }

    public static int mostCommonColour(Image img) {
        Image scaled = img.getScaledInstance(1, 1, Image.SCALE_REPLICATE);
        return toBufferedImage(scaled).getRGB(0, 0);
    }

    // https://stackoverflow.com/a/13632114/21405641
    public static String urlContentToString(String requestURL) {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new URI(requestURL).toURL().openStream()))) {
            StringBuilder sb = new StringBuilder();
            for (String line; (line = br.readLine()) != null; )
                sb.append(line);
            return sb.toString();
        } catch (URISyntaxException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static long millisToHours(long millis) {
        return Math.round(millis / 3600.0 / 1000);
    }

    public static BigInteger randomBigInteger(BigInteger min, BigInteger max, Random rand) {
        BigInteger randomNumber;
        do {
            randomNumber = min.add(new BigInteger(max.bitLength(), rand));
        } while (randomNumber.compareTo(max) >= 0 || randomNumber.compareTo(min) <= 0);
        return randomNumber;
    }

    /**
     * @param ns nanoseconds
     * @return a nice formatted String of days, hours, minutes and seconds
     */
    // https://stackoverflow.com/a/45075606/21405641
    public static String formatTimeWithDays(long ns) {
        long tempSec = ns / 1_000_000_000;
        long sec = tempSec % 60;
        long min = (tempSec / 60) % 60;
        long hour = (tempSec / (60 * 60)) % 24;
        long day = (tempSec / (24 * 60 * 60)) % 24;

        String secStr = pluralizer("second", "seconds", sec);
        String minStr = pluralizer("minute", "minutes", min);
        String hourStr = pluralizer("hour", "hours", hour);
        String dayStr = pluralizer("day", "days", day);
        return String.format("%d %s, %d %s, %d %s and %d %s", day, dayStr, hour, hourStr, min, minStr, sec, secStr);
    }

    /**
     * @param ns nanoseconds
     * @return a nice formatted String of hours, minutes and seconds
     */
    // https://stackoverflow.com/a/45075606/21405641
    public static String formatTime(long ns) {
        long tempSec = ns / 1_000_000_000;
        long sec = tempSec % 60;
        long min = (tempSec / 60) % 60;
        long hour = tempSec / (60 * 60);

        String secStr = pluralizer("second", "seconds", sec);
        String minStr = pluralizer("minute", "minutes", min);
        String hourStr = pluralizer("hour", "hours", hour);
        return String.format("%d %s, %d %s and %d %s", hour, hourStr, min, minStr, sec, secStr);
    }

    // https://stackoverflow.com/a/20536597/21405641
    public static String randAlphaNum(int length, Random rand) {
        final String SALTCHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890";
        StringBuilder salt = new StringBuilder();
        while (salt.length() < length) { // length of the random string.
            int index = (int) (rand.nextFloat() * SALTCHARS.length());
            salt.append(SALTCHARS.charAt(index));
        }
        return salt.toString();
    }

    public static long oneDayCooldown(LocalDate date, ZoneId z) {
        LocalDate today = LocalDate.now(z);
        if (!date.isBefore(today)) {
            ZonedDateTime now = ZonedDateTime.now(z);
            ZonedDateTime tomorrowMidnight = today.plusDays(1).atStartOfDay(z);
            return Duration.between(now, tomorrowMidnight).toNanos();
        }
        return 0;
    }

    public static BigInteger scaleBigInteger(BigInteger bi, double scalar) {
        return new BigDecimal(bi).multiply(BigDecimal.valueOf(scalar))
                .setScale(0, RoundingMode.HALF_UP).toBigInteger();
    }
}
