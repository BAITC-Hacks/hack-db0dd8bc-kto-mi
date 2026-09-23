package com.qadam.service;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * No task, team or proposal exists with the requested id.
 */
@Getter
public class NotFoundException extends RuntimeException {

    @Getter
    @RequiredArgsConstructor
    public enum Resource {
        TASK("Задача", "не найдена"),
        TEAM("Команда", "не найдена"),
        PROPOSAL("Отклик", "не найден");

        private final String displayName;
        /** «не найден» in the grammatical gender of {@link #displayName}. */
        private final String notFound;
    }

    /** Message for the user, in Russian. */
    public String getUserMessage() {
        return resource.getDisplayName() + " с id=" + id + " " + resource.getNotFound();
    }

    private final Resource resource;
    private final long id;

    public NotFoundException(Resource resource, long id) {
        super(resource + " " + id + " not found");
        this.resource = resource;
        this.id = id;
    }
}
