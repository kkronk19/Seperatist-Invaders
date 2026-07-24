package spaceinvaders.services.scores;

import java.time.LocalDate;

/** Value object deliberately kept independent of Swing and game entities. */
public record HighScoreEntry(int score, LocalDate date, String mode) { }
