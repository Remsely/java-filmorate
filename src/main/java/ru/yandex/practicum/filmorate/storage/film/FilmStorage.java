package ru.yandex.practicum.filmorate.storage.film;

import ru.yandex.practicum.filmorate.model.Film;

import java.util.List;
import java.util.Set;

public interface FilmStorage {
    Film add(Film film);

    Film update(Film film);

    Film get(long id);

    Film addLike(long id, long userId);

    Film removeLike(long id, long userId);

    List<Film> getAll();

    List<Film> getFilmWithDirectorName(String name);

    List<Film> getFilmWithName(String name);

    List<Film> getDirectorSortedFilms(Long id, String sortBy);

    Set<Long> getLikes(long id);

    List<Film> getPopularFilm(int count, Long genreId, Integer year);

    void delete(long id);

    boolean notContainFilm(long id);
}
