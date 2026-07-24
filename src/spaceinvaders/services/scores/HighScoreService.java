package spaceinvaders.services.scores;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Small, dependency-free local top-ten store. Malformed lines are ignored. */
public final class HighScoreService {
    private static final int LIMIT = 10;
    private static final Path FILE = Path.of(System.getProperty("user.home"), ".separatist-invaders", "high-scores.txt");

    public Path location() { return FILE; }

    public synchronized List<HighScoreEntry> load() {
        List<HighScoreEntry> entries = new ArrayList<>();
        if (!Files.exists(FILE)) return entries;
        try {
            for (String line : Files.readAllLines(FILE, StandardCharsets.UTF_8)) {
                String[] parts = line.split("\\|", 3);
                if (parts.length != 3) continue;
                try {
                    int score = Integer.parseInt(parts[0]);
                    if (score >= 0) entries.add(new HighScoreEntry(score, LocalDate.parse(parts[1]), parts[2]));
                } catch (RuntimeException ignored) { /* one corrupt entry must not lose the board */ }
            }
        } catch (IOException e) {
            System.err.println("Could not read high scores: " + e.getMessage());
        }
        entries.sort(Comparator.comparingInt(HighScoreEntry::score).reversed());
        return entries.subList(0, Math.min(LIMIT, entries.size()));
    }

    public synchronized void recordOnce(int score, String mode) {
        if (score <= 0) return;
        List<HighScoreEntry> entries = new ArrayList<>(load());
        HighScoreEntry candidate = new HighScoreEntry(score, LocalDate.now(), mode == null ? "Chapter 1" : mode);
        if (!entries.contains(candidate)) entries.add(candidate);
        entries.sort(Comparator.comparingInt(HighScoreEntry::score).reversed());
        if (entries.size() > LIMIT) entries = new ArrayList<>(entries.subList(0, LIMIT));
        try {
            Files.createDirectories(FILE.getParent());
            List<String> lines = new ArrayList<>();
            for (HighScoreEntry entry : entries) lines.add(entry.score() + "|" + entry.date() + "|" + entry.mode());
            Files.write(FILE, lines, StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("Could not save high scores: " + e.getMessage());
        }
    }
}
