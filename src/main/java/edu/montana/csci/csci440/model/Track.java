package edu.montana.csci.csci440.model;

import edu.montana.csci.csci440.util.DB;
import redis.clients.jedis.Client;
import redis.clients.jedis.Jedis;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class Track extends Model {

    private Long trackId;
    private Long albumId;
    private Long mediaTypeId;
    private Long genreId;
    private String name;
    private Long milliseconds;
    private Long bytes;
    private BigDecimal unitPrice;
    private Artist artistCache;
    private Album albumCache;

    public static final String REDIS_CACHE_KEY = "cs440-tracks-count-cache";

    public Track() {
        mediaTypeId = 1l;
        genreId = 1l;
        milliseconds  = 0l;
        bytes  = 0l;
        unitPrice = new BigDecimal("0");
    }

    public Track(ResultSet results) throws SQLException {
        name = results.getString("Name");
        milliseconds = results.getLong("Milliseconds");
        bytes = results.getLong("Bytes");
        unitPrice = results.getBigDecimal("UnitPrice");
        trackId = results.getLong("TrackId");
        albumId = results.getLong("AlbumId");
        mediaTypeId = results.getLong("MediaTypeId");
        albumCache = getAlbum();
        artistCache = albumCache.getArtist();
    }

    public static Track find(long trackId) {
        try {
            try (Connection conn = DB.connect();
                 PreparedStatement stmt = conn.prepareStatement(
                         "SELECT *, Albums.Title AS AlbumTitle, Artists.Name AS ArtistName " +
                                 "FROM Tracks " +
                                 "JOIN Albums ON Tracks.AlbumId = Albums.AlbumId " +
                                 "JOIN Artists ON Albums.ArtistId = Artists.ArtistId " +
                                 "WHERE TrackId = ?")) {
                stmt.setLong(1, trackId);
                ResultSet resultSet = stmt.executeQuery();
                if (resultSet.next()) {
                    return new Track(resultSet);
                } else {
                    return null;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static Long count() {

        // automatically connects to redis instance running on local machine
        Jedis jedis = new Jedis();
        if(jedis.exists(REDIS_CACHE_KEY)) {
            return Long.parseLong(jedis.get(REDIS_CACHE_KEY));
        }

        // store count in redis, so we don't have to access the database again
        try (Connection conn = DB.connect();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT COUNT(*)\n" +
                             "FROM tracks;")) {
            ResultSet resultSet = stmt.executeQuery();
            if (resultSet.next()) {
                long count = resultSet.getLong(1);
                jedis.set(REDIS_CACHE_KEY, String.valueOf(count));
                return count;
            } else {
                throw new IllegalStateException("Could not find a count.");
            }
        } catch (SQLException sqlException) {
            throw new RuntimeException(sqlException);
        }

        // cache invalidation scenario: if someone does something to change the count of the number of tracks in the database
        // then, clear (delete) it
        // look up redis documentation for this
        // going to have to clear this value in the couple of places that changes the count of the number of tracks
    }

    public Album getAlbum() {
        return Album.find(albumId);
    }

    public MediaType getMediaType() {
        return MediaType.find(mediaTypeId);
    }
    public Genre getGenre() {
        return Genre.find(genreId);
    }
    public List<Playlist> getPlaylists(){
        try {
            try (Connection connect = DB.connect();
                 PreparedStatement stmt = connect.prepareStatement(
                         "SELECT * " +
                                 "FROM playlists " +
                                 "JOIN playlist_track ON playlists.PlaylistId = playlist_track.PlaylistId " +
                                 "WHERE playlist_track.TrackId = ?")) {
                stmt.setLong(1, this.trackId); // Assuming trackId is the ID of the current track
                try (ResultSet resultSet = stmt.executeQuery()) {
                    List<Playlist> playlists = new ArrayList<>();
                    while (resultSet.next()) {
                        playlists.add(new Playlist(resultSet));
                    }
                    return playlists;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Long getTrackId() {
        return trackId;
    }

    public void setTrackId(Long trackId) {
        this.trackId = trackId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getMilliseconds() {
        return milliseconds;
    }

    public void setMilliseconds(Long milliseconds) {
        this.milliseconds = milliseconds;
    }

    public Long getBytes() {
        return bytes;
    }

    public void setBytes(Long bytes) {
        this.bytes = bytes;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public Long getAlbumId() {
        return albumId;
    }

    public void setAlbumId(Long albumId) {
        this.albumId = albumId;
    }

    public void setAlbum(Album album) {
        albumId = album.getAlbumId();
    }

    public Long getMediaTypeId() {
        return mediaTypeId;
    }

    public void setMediaTypeId(Long mediaTypeId) {
        this.mediaTypeId = mediaTypeId;
    }

    public Long getGenreId() {
        return genreId;
    }

    public void setGenreId(Long genreId) {
        this.genreId = genreId;
    }

    public String getArtistName() {
        return artistCache.getName();
    }

    public String getAlbumTitle() {
        return albumCache.getTitle();
    }

    @Override
    public boolean verify() {
        _errors.clear();
        if (getName() == null || getName().isEmpty()) {
            _errors.add("Name is required");
        }
        if (albumId == null) {
            _errors.add("AlbumId is required");
        }
        return _errors.isEmpty();
    }

    public static List<Track> advancedSearch(int page, int count,
                                             String search, Integer artistId, Integer albumId,
                                             Integer maxRuntime, Integer minRuntime) {
        String sql = "SELECT tracks.*, albums.ArtistId " +
                "FROM tracks " +
                "JOIN albums ON tracks.AlbumId = albums.AlbumId " +
                "WHERE name LIKE ? ";

        ArrayList<Object> args = new ArrayList<>();
        args.add("%" + search + "%");

        if (artistId != null){
            sql += "AND albums.ArtistId = ? ";
            args.add(artistId);
        }
        if (albumId != null){
            sql += "AND albums.AlbumId = ? ";
            args.add(albumId);
        }
        if (maxRuntime != null){
            sql += "AND tracks.Milliseconds <= ? ";
            args.add(maxRuntime);
        }
        if (minRuntime != null){
            sql += "AND tracks.Milliseconds >= ? ";
            args.add(minRuntime);
        }

        sql += "LIMIT ?";
        args.add(count);

        try(Connection connect = DB.connect();
            PreparedStatement stmt = connect.prepareStatement(sql)) {

            for (int i = 0; i < args.size(); i++){
                Object arg = args.get(i);
                stmt.setObject(i + 1, arg);
            }

            ResultSet resultSet = stmt.executeQuery();
            List<Track> results = new LinkedList<>();
            while (resultSet.next()) {
                results.add(new Track(resultSet));
            }
            return results;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static List<Track> search(int page, int count, String orderBy, String search) {
        try {
            try(Connection connect = DB.connect();
                PreparedStatement stmt = connect.prepareStatement(
                        "SELECT *, Albums.Title AS AlbumTitle, Artists.Name AS ArtistName " +
                        "FROM Tracks " +
                        "JOIN Albums ON Tracks.AlbumId = Albums.AlbumId " +
                        "JOIN Artists ON Albums.ArtistId = Artists.ArtistId " +
                        "WHERE Artists.name LIKE ? " +
                                "OR Tracks.trackId LIKE ? " +
                                "OR Albums.title LIKE ? " +
                                "LIMIT ?")) {
                ArrayList<Track> result = new ArrayList<>();
                stmt.setString(1, "%" + search + "%");
                stmt.setString(2, "%" + search + "%");
                stmt.setString(3, "%" + search + "%");
                stmt.setInt(4, count);
                ResultSet resultSet = stmt.executeQuery();
                while (resultSet.next()) {
                    result.add(new Track(resultSet));
                }
                return result;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean create() {
        if (verify()) {
            try (Connection conn = DB.connect();
                 PreparedStatement stmt = conn.prepareStatement(
                         "INSERT INTO tracks (Name, Milliseconds, Bytes, UnitPrice, AlbumId, MediaTypeId, GenreId) VALUES (?, ?, ?, ?, ?, ?, ?)")) {
                stmt.setString(1, this.getName());
                stmt.setLong(2, this.getMilliseconds());
                stmt.setLong(3, this.getBytes());
                stmt.setBigDecimal(4, this.getUnitPrice());
                stmt.setLong(5, this.getAlbumId());
                stmt.setLong(6, this.getMediaTypeId());
                stmt.setLong(7, this.getGenreId());
                stmt.executeUpdate();
                this.trackId = DB.getLastID(conn);

                Jedis jedis = new Jedis();
                jedis.del(REDIS_CACHE_KEY);

                return true;
            } catch (SQLException sqlException) {
                throw new RuntimeException(sqlException);
            }
        }
        return false;
    }

    @Override
    public boolean update() {
        if (verify()) {
            try (Connection conn = DB.connect();
                 PreparedStatement stmt = conn.prepareStatement(
                         "UPDATE tracks SET Name=? WHERE TrackId=?")) {
                stmt.setString(1, this.getName());
                stmt.setLong(2, this.getTrackId());
                stmt.executeUpdate();
                return true;
            } catch (SQLException sqlException) {
                throw new RuntimeException(sqlException);
            }
        } else {
            return false;
        }
    }

    @Override
    public void delete() {
        List<Playlist> playlists = this.getPlaylists();
        for(Playlist p : playlists) {
            try (Connection conn = DB.connect(); PreparedStatement stmt = conn.prepareStatement("DELETE FROM playlist_track WHERE TrackId=?")) {
                stmt.setLong(1, getTrackId());
                stmt.executeUpdate();
            } catch (SQLException sqlException) {
                throw new RuntimeException(sqlException);
            }
        }

        try(Connection conn = DB.connect(); PreparedStatement stmt = conn.prepareStatement("DELETE FROM invoice_items WHERE TrackId=?")) {
            stmt.setLong(1, getTrackId());
            stmt.executeUpdate();
        } catch (SQLException sqlException){
            throw new RuntimeException(sqlException);
        }

        try(Connection conn = DB.connect(); PreparedStatement stmt = conn.prepareStatement("DELETE FROM tracks WHERE TrackId=?")) {
            stmt.setLong(1, getTrackId());
            stmt.executeUpdate();
        } catch (SQLException sqlException){
            throw new RuntimeException(sqlException);
        }

        Jedis jedis = new Jedis();
        jedis.del(REDIS_CACHE_KEY);
    }

    public static List<Track> forAlbum(Long albumId) {
        return Collections.emptyList();
    }

    // Sure would be nice if java supported default parameter values
    public static List<Track> all() {
        return all(0, Integer.MAX_VALUE);
    }

    public static List<Track> all(int page, int count) {
        return all(page, count, "TrackId");
    }

    public static List<Track> all(int page, int count, String orderBy) {
        try {
            try(Connection connect = DB.connect();
                PreparedStatement stmt = connect.prepareStatement(
                        "SELECT *, Albums.Title AS AlbumTitle, Artists.Name AS ArtistName " +
                        "FROM Tracks " +
                        "JOIN Albums ON Tracks.AlbumId = Albums.AlbumId " +
                        "JOIN Artists ON Albums.ArtistId = Artists.ArtistId " +
                        "ORDER BY " + orderBy +
                                " LIMIT ? OFFSET ?")) {
                ArrayList<Track> result = new ArrayList<>();
                int offset = (page == 0) ? (page * count) : (page - 1) * count;
                stmt.setInt(1, count);
                stmt.setInt(2, offset);
                ResultSet resultSet = stmt.executeQuery();
                while (resultSet.next()) {
                    result.add(new Track(resultSet));
                }
                return result;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }    }

}
