package ru.yandex.practicum.filmorate.storage.film;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.EntityNotFoundException;
import ru.yandex.practicum.filmorate.exception.FilmAttributeNotExistOnFilmCreationException;
import ru.yandex.practicum.filmorate.model.ErrorResponse;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.enumarate.ChoosingSearch;
import ru.yandex.practicum.filmorate.storage.director.DirectorStorage;
import ru.yandex.practicum.filmorate.storage.genre.GenreStorage;
import ru.yandex.practicum.filmorate.storage.mpa.MPAStorage;

import java.sql.*;
import java.sql.Date;
import java.util.*;

@RequiredArgsConstructor
@Component
public class FilmDbStorage implements FilmStorage {
    private final JdbcTemplate jdbcTemplate;
    private final GenreStorage genreStorage;
    private final MPAStorage mpaStorage;
    private final DirectorStorage directorStorage;

    @Override
    public Film add(Film film) {
        String sqlQuery = "INSERT INTO film (name, description, rating_id, release, duration) VALUES (?, ?, ?, ?, ?)";

        KeyHolder keyHolder = new GeneratedKeyHolder();

        long mpaId = film.getMpa().getId();
        if (mpaStorage.notContainMPA(mpaId)) {
            throw new FilmAttributeNotExistOnFilmCreationException(
                    new ErrorResponse("MPA id", String.format("Не найден MPA с ID: %d.", mpaId))
            );
        }

        jdbcTemplate.update(connection -> {
                    PreparedStatement statement = connection.prepareStatement(sqlQuery, new String[]{"film_id"});
                    statement.setString(1, film.getName());
                    statement.setString(2, film.getDescription());

                    if (film.getMpa() == null) {
                        statement.setNull(3, Types.INTEGER);
                    } else {
                        statement.setLong(3, film.getMpa().getId());
                    }

                    statement.setDate(4, Date.valueOf(film.getReleaseDate()));
                    statement.setInt(5, film.getDuration());
                    return statement;
                },
                keyHolder);

        long id = Objects.requireNonNull(keyHolder.getKey()).longValue();
        genreStorage.addFilmGenres(id, film.getGenres());
        directorStorage.addDirectors(id, film.getDirectors());
        return this.get(id);
    }

    @Override
    public Film update(Film film) {
        long id = film.getId();
        checkFilmExist(id);

        String sqlQuery =
                "UPDATE film " +
                        "SET name = ?, " +
                        "    description = ?, " +
                        "    rating_id = ?, " +
                        "    release = ?, " +
                        "    duration = ? " +
                        "WHERE film_id = ?";
        jdbcTemplate.update(sqlQuery,
                film.getName(),
                film.getDescription(),
                film.getMpa() == null ? null : film.getMpa().getId(),
                film.getReleaseDate(),
                film.getDuration(),
                id);
        genreStorage.updateFilmGenres(id, film.getGenres());
        directorStorage.updateFilmDirectors(id, film.getDirectors());
        return this.get(film.getId());
    }

    @Override
    public Film get(long id) {
        checkFilmExist(id);
        String filmSqlQuery = "SELECT * FROM film WHERE film_id = ?";
        return jdbcTemplate.queryForObject(filmSqlQuery, this::mapRowToFilm, id);
    }

    @Override
    public void delete(long id) {
        if (this.notContainFilm(id)) {
            throw new EntityNotFoundException(
                    new ErrorResponse("Film id", String.format("Не найден фильм с ID: %d.", id))
            );
        }
        String sqlQuery = "DELETE FROM film WHERE film_id = " + id;
        jdbcTemplate.execute(sqlQuery);
    }

    @Override
    public List<Film> getAll() {
        String sqlQuery = "SELECT * FROM film";
        return jdbcTemplate.query(sqlQuery, this::mapRowToFilm);
    }

    @Override
    public List<Film> getFilmWithDirectorName(String name) {
        String nameStr = "%" + name.toLowerCase() + "%";
        String sqlQuery = "SELECT f.*" +
                "FROM FILM f " +
                "JOIN FILM_DIRECTOR fd ON f.FILM_ID  = fd.FILM_ID " +
                "JOIN DIRECTOR d ON d.DIRECTOR_ID  = fd.DIRECTOR_ID " +
                "left join like_film l on l.film_id = f.film_id " +
                "WHERE LOWER(d.NAME) LIKE ? " +
                "GROUP BY f.film_id " +
                "ORDER BY COUNT(l.film_id)";
        return jdbcTemplate.query(sqlQuery, this::mapRowToFilm, nameStr);
    }

    @Override
    public Film addLike(long id, long userId) {
        checkFilmExist(id);
        String sqlQuery = "MERGE INTO like_film (film_id, user_id) VALUES (?, ?)";
        jdbcTemplate.update(sqlQuery, id, userId);
        return this.get(id);
    }

    @Override
    public Film removeLike(long id, long userId) {
        checkFilmExist(id);
        String sqlQuery = "DELETE FROM like_film WHERE film_id = ? AND user_id = ?";
        jdbcTemplate.update(sqlQuery, id, userId);
        return this.get(id);
    }

    @Override
    public Set<Long> getFilmLikes(long id) {
        checkFilmExist(id);
        String sqlQuery = "SELECT user_id FROM like_film WHERE film_id = ?";
        return (new HashSet<>(jdbcTemplate.query(sqlQuery, (rs, rowNum) -> rs.getLong("user_id"), id)));
    }

    @Override
    public List<Film> getCommonFilms(long id1, long id2) {
        final String sqlQuery = "SELECT f.* " +
                "FROM like_film AS lf1 " +
                "         JOIN like_film AS lf2 ON lf1.film_id = lf2.film_id AND lf2.user_id = ? " +
                "         JOIN film AS f ON lf1.film_id = f.film_id " +
                "WHERE lf1.user_id = ?";
        return jdbcTemplate.query(sqlQuery, this::mapRowToFilm, id1, id2);
    }

    @Override
    public List<Film> getFilmWithName(String name) {
        String nameStr = "%" + name.toLowerCase() + "%";
        String sqlQuery = "SELECT f.* " +
                "FROM film f " +
                "left join like_film l on l.film_id = f.film_id " +
                "WHERE LOWER(f.name) LIKE ? " +
                "GROUP BY f.film_id " +
                "ORDER BY COUNT(l.film_id)";
        return jdbcTemplate.query(sqlQuery, this::mapRowToFilm, nameStr);
    }

    @Override
    public List<Film> getPopularFilm(int count, Long genreId, Integer year) {
        String sqlQuery = "SELECT fl.* " +
                "FROM film fl " +
                "LEFT JOIN like_film li ON li.film_id = fl.film_id " +
                (genreId != null ? "JOIN film_genre fg ON fl.film_id = fg.film_id" +
                        " WHERE fg.genre_id = " + genreId + " " : "") +
                (year != null ? (genreId != null ? "AND" : "WHERE") + " EXTRACT(YEAR FROM CAST(fl.release AS" +
                        " TIMESTAMP)) = " + year + " " : "") +
                "GROUP BY fl.film_id " +
                "ORDER BY COUNT(li.film_id) DESC " +
                "LIMIT " + count;
        return jdbcTemplate.query(sqlQuery, this::mapRowToFilm);
    }

    @Override
    public List<Film> getDirectorSortedFilms(Long directorId, String sortBy) {
        if (directorId != null && directorStorage.notContainDirector(directorId)) {
            throw new EntityNotFoundException(
                    new ErrorResponse("Director id", String.format("Не найден режиссер с ID: %d.", directorId))
            );
        }

        String sqlQuery = "SELECT f.film_id " +
                "FROM film f " +
                "JOIN film_director fd ON f.film_id = fd.film_id " +
                ("year".equals(sortBy) ? "WHERE fd.director_id = ? " +
                        "ORDER BY f.release" :
                        "LEFT JOIN like_film lf ON f.film_id = lf.film_id " +
                                "WHERE fd.director_id = ? " +
                                "GROUP BY f.film_id " +
                                "ORDER BY COUNT(lf.film_id) DESC");

        return jdbcTemplate.query(sqlQuery, new Object[]{directorId}, this::mapRowToSortedFilms);
    }

    @Override
    public List<Film> getRecommendations(Long id) {
        String sqlQuery =
                "SELECT film.* " +
                        "FROM film " +
                        "  INNER JOIN like_film AS u2 ON film.film_id = u2.film_id " +
                        "  LEFT JOIN like_film AS u1 ON u2.film_id = u1.film_id and u1.user_id = ? " +
                        "WHERE u1.film_id IS NULL " +
                        "  AND u2.user_id = ( " +
                        "    SELECT u2.user_id " +
                        "    FROM like_film AS u1 " +
                        "      INNER JOIN like_film AS u2 ON u1.film_id = u2.film_id " +
                        "    WHERE u1.user_id = ? " +
                        "      AND u2.user_id <> ? " +
                        "    GROUP BY u2.user_id " +
                        "    ORDER BY count(*) DESC " +
                        "    LIMIT 1) ";
        return jdbcTemplate.query(sqlQuery, this::mapRowToFilm, id, id, id);
    }

    @Override
    public List<Film> search(String query, List<String> by) {
        String searchStr = "%" + query.toLowerCase() + "%";
        List<Object> args = new ArrayList<>();

        StringBuilder conditions = new StringBuilder();
        if (by.contains(String.valueOf(ChoosingSearch.title))) {
            conditions.append("LOWER(name) like ? \n");
            args.add(searchStr);
        }
        if (by.contains(String.valueOf(ChoosingSearch.director))) {
            if (conditions.length() > 0) {
                conditions.append(" OR ");
            }
            conditions.append(" film_id IN ( \n " +
                    "SELECT fd.film_id \n " +
                    "FROM film_director AS fd \n " +
                    "  INNER JOIN director AS d ON fd.director_id = d.director_id \n " +
                    "WHERE LOWER(d.name) LIKE ? \n " +
                    ") \n ");
            args.add(searchStr);
        }
        if (conditions.length() > 0) {
            conditions.insert(0, "WHERE ");
        }

        String sqlQuery = "SELECT \n " +
                "  film.*, (SELECT COUNT(*) FROM like_film WHERE film.film_id = like_film.film_id) as likes \n " +
                "FROM film \n " +
                conditions +
                "ORDER BY likes DESC";

        return jdbcTemplate.query(sqlQuery,
                args.toArray(),
                this::mapRowToFilm);
    }

    @Override
    public boolean notContainFilm(long id) {
        String sqlQuery = "SELECT COUNT(*) FROM film WHERE film_id = ?";
        Integer count = jdbcTemplate.queryForObject(sqlQuery, Integer.class, id);
        return count != null && count == 0;
    }

    private Film mapRowToSortedFilms(ResultSet rs, int rowNum) throws SQLException {
        long id = rs.getLong("film_id");
        return get(id);
    }

    private Film mapRowToFilm(ResultSet rs, int rowNum) throws SQLException {
        long id = rs.getLong("film_id");
        return Film.builder()
                .id(id)
                .name(rs.getString("name"))
                .description(rs.getString("description"))
                .releaseDate(rs.getDate("release").toLocalDate())
                .duration(rs.getInt("duration"))
                .genres(genreStorage.getFilmGenres(id))
                .directors(directorStorage.getFilmDirectors(id))
                .mpa(mpaStorage.get(rs.getLong("rating_id")))
                .likes(this.getFilmLikes(id))
                .build();
    }

    private void checkFilmExist(long id) {
        if (this.notContainFilm(id)) {
            throw new EntityNotFoundException(
                    new ErrorResponse("Film id", String.format("Не найден фильм с ID: %d.", id))
            );
        }
    }
}
