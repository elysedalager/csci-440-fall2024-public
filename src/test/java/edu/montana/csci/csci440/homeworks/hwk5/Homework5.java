package edu.montana.csci.csci440.homeworks.hwk5;

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

public class Homework5 extends DBTest {

    @Test
    /*
     * Select tracks that have been sold more than once (> 1)
     *
     * Select the albums that have tracks that have been sold more than once (> 1)
     *   NOTE: This is NOT the same as albums whose tracks have been sold more than once!
     *         An album could have had three tracks, each sold once, and should not be included
     *         in this result.  It should only include the albums of the tracks found in the first
     *         query.
     * */
    public void selectPopularTracksAndTheirAlbums() throws SQLException {

        // HINT: join to invoice items and do a group by/having to get the right answer
        List<Map<String, Object>> tracks = exec("SELECT *\n" +
                "FROM tracks\n" +
                "JOIN invoice_items ON tracks.TrackId = invoice_items.TrackId\n" +
                "GROUP BY tracks.TrackId\n" +
                "HAVING COUNT(invoice_items.InvoiceLineId) > 1;");
        assertEquals(256, tracks.size());

        // HINT: join to tracks and invoice items and do a group by/having to get the right answer
        //       note: you will need to use the DISTINCT operator to get the right result!
        List<Map<String, Object>> albums = exec("SELECT DISTINCT *\n" +
                "FROM albums\n" +
                "JOIN tracks ON albums.AlbumId = tracks.AlbumId\n" +
                "JOIN invoice_items ON tracks.TrackId = invoice_items.TrackId\n" +
                "GROUP BY albums.AlbumId\n" +
                "HAVING SUM((SELECT COUNT(*)\n" +
                "            FROM invoice_items\n" +
                "            WHERE invoice_items.TrackId = tracks.TrackId) > 1) > 0;");
        assertEquals(166, albums.size());
    }

    @Test
    /*
     * Select customers emails who are assigned to Jane Peacock as a Rep and
     * who have purchased something from the 'Rock' Genre
     *
     * Please use an IN clause and a sub-select to generate customer IDs satisfying the criteria
     * */
    public void selectCustomersMeetingCriteria() throws SQLException {
        // HINT: join to invoice items and do a group by/having to get the right answer
        List<Map<String, Object>> tracks = exec("SELECT DISTINCT customers.Email\n" +
                "FROM customers\n" +
                "JOIN employees ON customers.SupportRepId = employees.EmployeeId\n" +
                "WHERE employees.FirstName = 'Jane' AND employees.LastName = 'Peacock' AND customers.CustomerId IN (\n" +
                "    SELECT DISTINCT invoices.CustomerId\n" +
                "    FROM invoices\n" +
                "    JOIN invoice_items ON invoices.InvoiceId = invoice_items.InvoiceId\n" +
                "    JOIN tracks ON invoice_items.TrackId = tracks.TrackId\n" +
                "    JOIN genres ON tracks.GenreId = genres.GenreId\n" +
                "    WHERE genres.Name = 'Rock');" );
        assertEquals(21, tracks.size());
    }

}
