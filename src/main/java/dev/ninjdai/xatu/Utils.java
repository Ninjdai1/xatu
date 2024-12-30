package dev.ninjdai.xatu;

public class Utils {
    public static final int DAY_LENGTH_IN_SECONDS = 60 * 60 * 24;

    enum Durations {
        DAY(DAY_LENGTH_IN_SECONDS),
        WEEK(DAY_LENGTH_IN_SECONDS * 7),
        MONTH(DAY_LENGTH_IN_SECONDS * 30),
        YEAR(DAY_LENGTH_IN_SECONDS * 365);

        public final int duration;
        Durations(int duration) {
            this.duration = duration;
        }
    }
}
