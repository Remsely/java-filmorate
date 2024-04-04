package ru.yandex.practicum.filmorate.storage.genre;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.EntityNotFoundException;
import ru.yandex.practicum.filmorate.exception.FilmAttributeNotExistOnFilmCreationException;
import ru.yandex.practicum.filmorate.model.ErrorResponse;
import ru.yandex.practicum.filmorate.model.Genre;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class GenreDbStorage implements GenreStorage {
    private final JdbcTemplate jdbcTemplate;

    @Override
    public Genre get(long id) {
        if (this.notContainGenre(id)) {
            throw new EntityNotFoundException(
                    new ErrorResponse("Genre id", String.format("Не найден жанр с ID: %d.", id))
            );
        }
        String sqlQuery = "SELECT * FROM genre WHERE genre_id = ?";
        return jdbcTemplate.queryForObject(sqlQuery, (rs, rowNum) ->
                new Genre(id, rs.getString("name")), id);
    }

    @Override
    public List<Genre> getAll() {
        String sqlQuery = "SELECT * FROM genre";
        return jdbcTemplate.query(sqlQuery, (rs, rowNum) ->
                new Genre(rs.getLong("genre_id"), rs.getString("name"))
        );
    }

    @Override
    public List<Genre> getFilmGenres(long id) {
        String sqlQuery = "SELECT * FROM film_genre WHERE film_id = ?";
        return jdbcTemplate.query(sqlQuery, (rs, rowNum) -> this.get(rs.getLong("genre_id")), id);
    }

    @Override
    public List<Genre> addFilmGenres(long id, List<Genre> genres) {
        if (genres != null && !genres.isEmpty()) {
            Set<Genre> uniqFilmGenres = new HashSet<>(genres);

            for (Genre genre : uniqFilmGenres) {
                long genreId = genre.getId();

                if (notContainGenre(genreId)) {
                    throw new FilmAttributeNotExistOnFilmCreationException(
                            new ErrorResponse("Genre id", String.format("Не найден жанр с ID: %d.", id))
                    );
                }
                this.add(id, genreId);
            }
        }
        return genres;
    }

    @Override
    public List<Genre> updateFilmGenres(long id, List<Genre> genres) {
        if (genres != null) {
            if (genres.isEmpty()) {
                this.delete(id);
                return genres;
            }

            List<Long> currentFilmGenres = this.getFilmGenresIds(id);
            Set<Long> uniqNewGenres = genres.stream().map(Genre::getId).collect(Collectors.toSet());
            Set<Long> genresToAdd = new HashSet<>();

            for (Long genreId : uniqNewGenres) {
                if (notContainGenre(genreId)) {
                    throw new FilmAttributeNotExistOnFilmCreationException(
                            new ErrorResponse("Genre id", String.format("Не найден жанр с ID: %d.", id))
                    );
                }

                if (!currentFilmGenres.contains(genreId)) {
                    genresToAdd.add(genreId);
                }
            }
            currentFilmGenres.forEach(genreId -> {
                if (!uniqNewGenres.contains(genreId))
                    this.delete(id, genreId);
            });
            genresToAdd.forEach(genreId -> this.add(id, genreId));
        }
        return genres;
    }

    @Override
    public void add(long filmId, long genreId) {
        String sqlQuery = "INSERT INTO film_genre (film_id, genre_id) VALUES (?, ?)";
        jdbcTemplate.update(sqlQuery, filmId, genreId);
    }

    @Override
    public void delete(long filmId, long genreId) {
        String sqlQuery = "DELETE FROM film_genre WHERE film_id = ? AND genre_id = ?";
        jdbcTemplate.update(sqlQuery, filmId, genreId);
    }

    @Override
    public void delete(long filmId) {
        String sqlQuery = "DELETE FROM film_genre WHERE film_id = ?";
        jdbcTemplate.update(sqlQuery, filmId);
    }

    @Override
    public boolean notContainGenre(long id) {
        String sqlQuery = "SELECT COUNT(*) FROM genre WHERE genre_id = ?";
        Integer count = jdbcTemplate.queryForObject(sqlQuery, Integer.class, id);
        return count != null && count == 0;
    }

    private List<Long> getFilmGenresIds(long id) {
        String sqlQuery = "SELECT genre_id FROM film_genre WHERE film_id = ?";
        return jdbcTemplate.query(sqlQuery, (rs, rowNum) -> rs.getLong("genre_id"), id);
    }
}