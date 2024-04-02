package ru.yandex.practicum.filmorate.storage.film;

import ru.yandex.practicum.filmorate.model.Film;

import java.util.List;
import java.util.Set;

public interface FilmStorage {
    Film add(Film film);

    Film update(Film film);

    Film get(long id);

    List<Film> getAll();

    Film addLike(long id, long userId);

    Film removeLike(long id, long userId);

    Set<Long> getLikes(long id);

    List<Film> getPopular(int count);

    boolean notContainFilm(long id);

    // DIRECTOR.Получить список фильмов режиссера отсортированных по количеству лайков или году выпуска.
    List<Film> getDirectorSortedFilms(long id, String sortBy);
}