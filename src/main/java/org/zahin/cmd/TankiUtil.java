package org.zahin.cmd;

import org.zahin.util.HttpResponseType;

import java.util.List;

record Rating(int position, int value) {
}

record WeeklyRatings(Rating crystals, Rating efficiency, Rating golds, Rating score) {
}

record Supply(String id, String imageUrl, String name, int usages) {
}

record GameMode(String name, int scoreEarned, long timePlayed, String type) {
}

record Equipment(int grade, String id, String imageUrl, String name, List<String> properties, int scoreEarned,
                 long timePlayed) {
}

record Present(int count, String imageUrl, String name, String prototypeId) {
}

record ResponseJsonObj(
        int caughtGolds,
        int deaths,
        List<Equipment> dronesPlayed,
        int earnedCrystals,
        int gearScore,
        boolean hasPremium,
        List<Equipment> hullsPlayed,
        int kills,
        List<GameMode> modesPlayed,
        String name,
        List<Equipment> paintsPlayed,
        List<Present> presents,
        WeeklyRatings previousRating,
        int rank,
        WeeklyRatings rating,
        List<Equipment> resistanceModules,
        int score,
        int scoreBase,
        int scoreNext,
        List<Supply> suppliesUsage,
        List<Equipment> turretsPlayed
) {
}

record TankiRatingsApiResponse(ResponseJsonObj response, HttpResponseType responseType) {
}
