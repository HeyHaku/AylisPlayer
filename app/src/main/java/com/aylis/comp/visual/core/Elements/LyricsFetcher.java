package com.aylis.comp.visual.core.Elements;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import android.os.AsyncTask;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import org.json.JSONArray;
import org.json.JSONObject;
import android.content.Context;
import com.aylis.PlayerCore;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

public class LyricsFetcher {
    private String lastArtist = "";
    private String lastTitle = "";
    private boolean forceRefetch = false;
    private AsyncTask<Void, Void, String> fetchTask;
    private List<LrcLine> rawParsedLyrics = new ArrayList<>();
    private volatile List<LrcLine> parsedLyrics = new ArrayList<>();
    private int maxCharsPerLine = 0;
    private String lastError = "";
    private static java.util.HashSet<String> cachedLyricsFiles = null;

    public static void initCacheFast() {
        if (cachedLyricsFiles != null) return;
        cachedLyricsFiles = new java.util.HashSet<>();
        try {
            Context ctx = PlayerCore.s().getAppContext();
            if (ctx == null) return;
            File dir = new File(ctx.getCacheDir(), "lyrics_cache");
            if (dir.exists() && dir.isDirectory()) {
                File[] files = dir.listFiles();
                if (files != null) {
                    for (File f : files) {
                        cachedLyricsFiles.add(f.getName());
                    }
                }
            }
        } catch (Exception e) {}
    }

    public static boolean hasCacheFast(String artist, String title) {
        if (cachedLyricsFiles == null) initCacheFast();
        if (artist == null) artist = "";
        if (title == null) title = "";
        artist = artist.trim();
        title = title.trim();
        title = title.replaceAll("(?i)\\.(mp3|m4a|flac|wav|ogg|wma)$", "").trim();
        if (artist.isEmpty() && title.contains("-")) {
            String[] parts = title.split("-", 2);
            artist = parts[0].trim();
            title = parts[1].trim();
        }
        String name = artist + "_" + title;
        name = name.replaceAll("[^a-zA-Z0-9а-яА-Я._-]", "_") + ".lrc";
        return cachedLyricsFiles.contains(name);
    }

    private File getCacheFile(String artist, String title) {
        try {
            Context ctx = PlayerCore.s().getAppContext();
            if (ctx == null) return null;
            File dir = new File(ctx.getCacheDir(), "lyrics_cache");
            if (!dir.exists()) dir.mkdirs();
            String name = artist + "_" + title;
            name = name.replaceAll("[^a-zA-Z0-9а-яА-Я._-]", "_");
            return new File(dir, name + ".lrc");
        } catch (Exception e) {
            return null;
        }
    }

    public void setMaxCharsPerLine(int max) {
        if (this.maxCharsPerLine != max) {
            this.maxCharsPerLine = max;
            applyWordWrap();
        }
    }

    public void setForceRefetch(boolean force) {
        this.forceRefetch = force;
    }

    public List<LrcLine> getParsedLyrics() {
        return parsedLyrics;
    }

    public void clear() {
        rawParsedLyrics.clear();
        parsedLyrics = new ArrayList<>();
        lastArtist = "";
        lastTitle = "";
        lastError = "";
        if (fetchTask != null)
            fetchTask.cancel(true);
    }

    public void checkAndFetchLyrics(String artist, String title, String album, long durationSec, String fallbackText) {
        if (artist == null)
            artist = "";
        if (title == null)
            title = "";
        artist = artist.trim();
        title = title.trim();

        title = title.replaceAll("(?i)\\.(mp3|m4a|flac|wav|ogg|wma)$", "").trim();

        // Clean up YouTube specific tags from title
        title = title.replaceAll("\\(.*?\\)", "").trim();
        title = title.replaceAll("\\[.*?\\]", "").trim();
        title = title.replaceAll("(?i)(official video|official audio|lyric video|lyrics)", "").trim();

        if (artist.equalsIgnoreCase("YouTube Music")) {
            artist = "";
        }

        if (artist.isEmpty() && title.contains("-")) {
            String[] parts = title.split("-", 2);
            artist = parts[0].trim();
            title = parts[1].trim();
        }

        if (artist.equals(lastArtist) && title.equals(lastTitle) && !forceRefetch)
            return;

        lastArtist = artist;
        lastTitle = title;
        final boolean fForceRefetch = forceRefetch;
        forceRefetch = false;
        forceRefetch = false;
        rawParsedLyrics.clear();
        parsedLyrics = new ArrayList<>();

        parseLyrics(fallbackText, durationSec);
        if (!rawParsedLyrics.isEmpty()) {
            return;
        }

        if (title.isEmpty()) {
            rawParsedLyrics.add(new LrcLine(0, "Лирика не найдена: Неизвестный трек"));
            applyWordWrap();
            return;
        }

        if (fetchTask != null)
            fetchTask.cancel(true);

        File cacheFile = getCacheFile(artist, title);
        String cacheStatus = "";
        boolean shouldReadCache = false;

        if (fForceRefetch) {
            cacheStatus = "Skipped cache for refetch";
        } else if (cacheFile != null && cacheFile.exists()) {
            shouldReadCache = true;
        } else {
            cacheStatus = "No file";
        }

        if (shouldReadCache) {
            try {
                FileInputStream fis = new FileInputStream(cacheFile);
                BufferedReader in = new BufferedReader(new InputStreamReader(fis));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = in.readLine()) != null) sb.append(line).append("\n");
                in.close();
                String cachedLrc = sb.toString();
                if (!cachedLrc.trim().isEmpty()) {
                    parseLyrics(cachedLrc, durationSec);
                    if (!rawParsedLyrics.isEmpty()) {
                        return; // Found in cache!
                    } else {
                        cacheStatus = "Parse failed";
                    }
                } else {
                    cacheStatus = "Empty cache";
                }
            } catch (Exception e) {
                cacheStatus = "Read err";
            }
        }

        String searchDesc = artist.isEmpty() ? title : (artist + " - " + title);
        List<LrcLine> initialLyrics = new ArrayList<>();
        initialLyrics.add(new LrcLine(0, "Search lyric: " + searchDesc + "..."));
        parsedLyrics = initialLyrics;

        final String fArtist = artist;
        final String fTitle = title;
        final String fAlbum = album;
        final long fDurationSec = durationSec;
        final String fTextFallback = fallbackText;
        final String fCacheStatus = cacheStatus;

        fetchTask = new AsyncTask<Void, Void, String>() {
            private String lastError = "";
            private String performRequest(String urlStr) {
                try {
                    URL url = new URL(urlStr);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setConnectTimeout(10000);
                    conn.setReadTimeout(10000);
                    conn.setRequestProperty("User-Agent", "OpenPlayer/1.0 (https://github.com/aylis)");
                    conn.setRequestProperty("Accept", "application/json");

                    if (conn.getResponseCode() == 200) {
                        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                        StringBuilder response = new StringBuilder();
                        String inputLine;
                        while ((inputLine = in.readLine()) != null) {
                            response.append(inputLine);
                        }
                        in.close();
                        return response.toString();
                    } else {
                        lastError = "HTTP " + conn.getResponseCode();
                        System.err.println("LRCLIB API returned code: " + conn.getResponseCode());
                    }
                } catch (Exception e) {
                    lastError = e.getClass().getSimpleName();
                    e.printStackTrace();
                }
                return null;
            }

            private int levenshtein(String a, String b) {
                int[] costs = new int[b.length() + 1];
                for (int j = 0; j < costs.length; j++)
                    costs[j] = j;
                for (int i = 1; i <= a.length(); i++) {
                    costs[0] = i;
                    int nw = i - 1;
                    for (int j = 1; j <= b.length(); j++) {
                        int cj = Math.min(1 + Math.min(costs[j], costs[j - 1]),
                                a.charAt(i - 1) == b.charAt(j - 1) ? nw : nw + 1);
                        nw = costs[j];
                        costs[j] = cj;
                    }
                }
                return costs[b.length()];
            }

            private float getSimilarity(String expected, String actual) {
                if (expected == null || expected.trim().isEmpty())
                    return 0.0f;
                if (actual == null || actual.trim().isEmpty())
                    return 0.0f;

                expected = expected.toLowerCase().replaceAll("[^a-zа-я0-9]", "");
                actual = actual.toLowerCase().replaceAll("[^a-zа-я0-9]", "");
                if (expected.isEmpty() || actual.isEmpty())
                    return 0.0f;

                if (actual.contains(expected) || expected.contains(actual))
                    return 1.0f;

                int dist = levenshtein(expected, actual);
                int maxLen = Math.max(expected.length(), actual.length());
                return 1.0f - ((float) dist / maxLen);
            }

            private boolean isArtistMatch(String expected, String actual) {
                return getSimilarity(expected, actual) > 0.6f;
            }

            @Override
            protected String doInBackground(Void... voids) {
                if (isCancelled())
                    return null;

                try {
                    String trackQuery = java.net.URLEncoder.encode(fTitle, "UTF-8").replace("+", "%20");
                    String artistQuery = java.net.URLEncoder.encode(fArtist, "UTF-8").replace("+", "%20");
                    String apiBase = "https://lrclib.net/api/";
                    String q = fArtist.isEmpty() ? trackQuery : (artistQuery + "%20" + trackQuery);
                    String searchUrl = apiBase + "search?q=" + q;
                    String resp = performRequest(searchUrl);
                    String bestLrc = null;

                    if (resp != null && resp.trim().startsWith("[")) {
                        JSONArray arr = new JSONArray(resp);
                        int bestScore = -999999;

                        for (int i = 0; i < arr.length(); i++) {
                            JSONObject obj = arr.getJSONObject(i);
                            if (obj.has("syncedLyrics") && !obj.isNull("syncedLyrics")) {
                                String trackArtist = obj.optString("artistName", "");
                                String trackTitle = obj.optString("trackName", "");
                                int duration = obj.optInt("duration", 0);

                                int score = 0;

                                // Даем бонус за релевантность (популярность) от LRCLIB. Первые результаты
                                // получают больше баллов.
                                score += (arr.length() - i) * 2;

                                if (!fArtist.isEmpty()) {
                                    float artistSim = getSimilarity(fArtist, trackArtist);
                                    if (artistSim > 0.6f) {
                                        score += (int) (artistSim * 100);
                                    } else {
                                        score -= 50;
                                    }
                                }

                                float titleSim = getSimilarity(fTitle, trackTitle);
                                score += (int) (titleSim * 100);

                                if (fDurationSec > 0 && duration > 0) {
                                    int diff = (int) Math.abs(duration - fDurationSec);
                                    if (diff <= 2) {
                                        score += 200;
                                    } else if (diff <= 5) {
                                        score += 100;
                                    } else if (diff <= 15) {
                                        score += 50;
                                    } else {
                                        score -= diff * 2;
                                    }
                                }

                                String lrc = obj.getString("syncedLyrics");
                                if (lrc != null && !lrc.trim().isEmpty()) {
                                    if (score > bestScore) {
                                        bestScore = score;
                                        bestLrc = lrc;
                                    }
                                }
                            }
                        }

                        if (bestLrc != null)
                            return bestLrc;
                    }

                    return bestLrc;
                } catch (Exception e) {
                    lastError = "JSON " + e.getClass().getSimpleName();
                }

                return null;
            }

            @Override
            protected void onPostExecute(String lrc) {
                if (isCancelled())
                    return;
                if (!fArtist.equals(lastArtist) || !fTitle.equals(lastTitle))
                    return;

                if (lrc != null && !lrc.trim().isEmpty()) {
                    File cacheFile = getCacheFile(fArtist, fTitle);
                    if (cacheFile != null) {
                        try {
                            FileOutputStream fos = new FileOutputStream(cacheFile);
                            fos.write(lrc.getBytes("UTF-8"));
                            fos.close();
                            if (cachedLyricsFiles != null) cachedLyricsFiles.add(cacheFile.getName());
                        } catch (Exception e) {}
                    }
                    parseLyrics(lrc, fDurationSec);
                } else {
                    parseLyrics(fTextFallback, 0);
                    if (parsedLyrics.isEmpty()) {
                        rawParsedLyrics.clear();
                        rawParsedLyrics.add(new LrcLine(0, "Lyric not found" + (lastError.isEmpty() ? "" : " (" + lastError + ")") + " [" + fCacheStatus + "]"));
                        applyWordWrap();
                    }
                }
            }
        };
        fetchTask.execute();
    }

    public void parseLyrics(String rawText, long durationSec) {
        rawParsedLyrics.clear();
        if (rawText == null) {
            applyWordWrap();
            return;
        }
        String[] lines = rawText.split("\\n");
        Pattern pattern = Pattern.compile("\\[(\\d{2,}):(\\d{2})(?:\\.(\\d{1,3}))?\\](.*)");
        for (String line : lines) {
            Matcher m = pattern.matcher(line);
            if (m.find()) {
                try {
                    long min = Long.parseLong(m.group(1));
                    long sec = Long.parseLong(m.group(2));
                    long ms = 0;
                    String msStr = m.group(3);
                    if (msStr != null) {
                        if (msStr.length() == 2)
                            ms = Long.parseLong(msStr) * 10;
                        else if (msStr.length() == 3)
                            ms = Long.parseLong(msStr);
                        else if (msStr.length() == 1)
                            ms = Long.parseLong(msStr) * 100;
                    }
                    long time = (min * 60 + sec) * 1000 + ms;
                    String txt = m.group(4);
                    if (txt == null) txt = "";
                    txt = txt.trim();
                    rawParsedLyrics.add(new LrcLine(time, txt));
                } catch (Exception e) {}
            }
        }

        Collections.sort(rawParsedLyrics, new Comparator<LrcLine>() {
            @Override
            public int compare(LrcLine l1, LrcLine l2) {
                return Long.compare(l1.timeMs, l2.timeMs);
            }
        });

        applyWordWrap();
    }

    private void applyWordWrap() {
        List<LrcLine> newParsedLyrics = new ArrayList<>();
        if (maxCharsPerLine <= 0) {
            newParsedLyrics.addAll(rawParsedLyrics);
            parsedLyrics = newParsedLyrics;
            return;
        }

        for (int i = 0; i < rawParsedLyrics.size(); i++) {
            LrcLine line = rawParsedLyrics.get(i);
            String text = line.text;

            if (text.length() <= maxCharsPerLine) {
                newParsedLyrics.add(new LrcLine(line.timeMs, text));
                continue;
            }

            List<String> chunks = new ArrayList<>();
            String[] words = text.split(" ");
            StringBuilder currentChunk = new StringBuilder();

            for (String w : words) {
                if (currentChunk.length() + w.length() > maxCharsPerLine && currentChunk.length() > 0) {
                    chunks.add(currentChunk.toString().trim());
                    currentChunk = new StringBuilder();
                }
                currentChunk.append(w).append(" ");
            }
            if (currentChunk.length() > 0) {
                chunks.add(currentChunk.toString().trim());
            }

            long startTime = line.timeMs;
            long endTime = startTime + 3000;
            if (i < rawParsedLyrics.size() - 1) {
                endTime = rawParsedLyrics.get(i + 1).timeMs;
            }
            long totalDuration = Math.max(0, endTime - startTime);

            int totalChars = 0;
            for (String s : chunks)
                totalChars += s.length();

            long accumulatedTime = startTime;
            for (String s : chunks) {
                newParsedLyrics.add(new LrcLine(accumulatedTime, s));
                long chunkDuration = (totalChars > 0) ? (long) (totalDuration * ((float) s.length() / totalChars)) : 0;
                accumulatedTime += chunkDuration;
            }
        }
        parsedLyrics = newParsedLyrics;
    }
}
