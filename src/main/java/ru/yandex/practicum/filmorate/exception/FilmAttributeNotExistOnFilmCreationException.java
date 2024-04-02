package ru.yandex.practicum.filmorate.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import ru.yandex.practicum.filmorate.model.ErrorResponse;

@AllArgsConstructor
@Getter
public class FilmAttributeNotExistOnFilmCreationException extends RuntimeException {
    private final ErrorResponse errorResponse;
}
