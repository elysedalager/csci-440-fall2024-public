package edu.montana.csci.csci440.homeworks.hwk3;

import edu.montana.csci.csci440.DBTest;
import edu.montana.csci.csci440.model.Track;
import edu.montana.csci.csci440.util.DB;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Homework3 extends DBTest {

    @Test
    /*
     * Create a view tracksPlus to display the artist, song title, album, and genre for all tracks.
     */
    public void createTracksPlusView(){
        executeDDL("DROP VIEW IF EXISTS tracksPlus;");
        executeDDL("CREATE VIEW tracksPlus AS\n" +
                "SELECT artists.Name AS ArtistName, tracks.Name AS SongTitle, tracks.TrackId AS TrackId, albums.Title AS AlbumTitle, genres.Name AS GenreName\n" +
                "FROM artists\n" +
                "JOIN albums ON artists.ArtistId = albums.ArtistId\n" +
                "JOIN tracks ON albums.AlbumId = tracks.AlbumId\n" +
                "JOIN genres ON tracks.GenreId = genres.GenreId;");

        List<Map<String, Object>> results = exec("SELECT * FROM tracksPlus ORDER BY TrackId");
        assertEquals(3503, results.size());
        assertEquals("Rock", results.get(0).get("GenreName"));
        assertEquals("AC/DC", results.get(0).get("ArtistName"));
        assertEquals("For Those About To Rock We Salute You", results.get(0).get("AlbumTitle"));
    }

    @Test
    /*
     * Create a table grammy_infos to track grammy information for an artist.  The table should include
     * a reference to the artist, the album (if the grammy was for an album) and the song (if the grammy was
     * for a song).  There should be a string column indicating if the artist was nominated or won.  Finally,
     * there should be a reference to the grammy_category table
     *
     * Create a table grammy_category
     */
    public void createGrammyInfoTable(){
        executeDDL("CREATE TABLE grammy_categories(\n" +
                "    GrammyCategoryId INTEGER NOT NULL PRIMARY KEY,\n" +
                "    Name NVARCHAR (160) NOT NULL\n" +
                ");");
        executeDDL("CREATE TABLE grammy_infos(\n" +
                "    GrammyId INTEGER NOT NULL PRIMARY KEY,\n" +
                "    ArtistId INTEGER NOT NULL,\n" +
                "    AlbumId INTEGER,\n" +
                "    TrackId INTEGER,\n" +
                "    Status NVARCHAR(160) NOT NULL,\n" +
                "    GrammyCategoryId INTEGER NOT NULL,\n" +
                "    FOREIGN KEY (ArtistId) REFERENCES artists (ArtistId),\n" +
                "    FOREIGN KEY (AlbumId) REFERENCES albums (AlbumId),\n" +
                "    FOREIGN KEY (TrackId) REFERENCES tracks (TrackId),\n" +
                "    FOREIGN KEY (GrammyCategoryId) REFERENCES grammy_categories (GrammyCategoryId)\n" +
                ");");

        // TEST CODE
        executeUpdate("INSERT INTO grammy_categories(Name) VALUES ('Greatest Ever');");
        Object categoryId = exec("SELECT GrammyCategoryId FROM grammy_categories").get(0).get("GrammyCategoryId");

        executeUpdate("INSERT INTO grammy_infos(ArtistId, AlbumId, TrackId, GrammyCategoryId, Status) VALUES (1, 1, 1, " + categoryId + ",'Won');");

        List<Map<String, Object>> results = exec("SELECT * FROM grammy_infos");
        assertEquals(1, results.size());
        assertEquals(1, results.get(0).get("ArtistId"));
        assertEquals(1, results.get(0).get("AlbumId"));
        assertEquals(1, results.get(0).get("TrackId"));
        assertEquals(1, results.get(0).get("GrammyCategoryId"));
    }

    @Test
    /*
     * Bulk insert five categories of your choosing in the genres table
     */
    public void bulkInsertGenres(){
        Integer before = (Integer) exec("SELECT COUNT(*) as COUNT FROM genres").get(0).get("COUNT");

        executeUpdate("INSERT INTO genres (Name)\n" +
                "VALUES ('Elyse''s hits'),\n" +
                "       ('Carson''s hits'),\n" +
                "       ('Elizabeth''s hits'),\n" +
                "       ('Mason''s hits'),\n" +
                "       ('Talia''s hits');");

        Integer after = (Integer) exec("SELECT COUNT(*) as COUNT FROM genres").get(0).get("COUNT");
        assertEquals(before + 5, after);
    }
}
