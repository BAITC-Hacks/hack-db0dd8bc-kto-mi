package com.qadam.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Industry of a business task. The API uses the constant name as the code.
 */
@Getter
@RequiredArgsConstructor
public enum Industry {
    IT("IT и цифровые сервисы"),
    RETAIL("Розничная торговля"),
    FINANCE("Финансы"),
    EDUCATION("Образование"),
    HEALTHCARE("Здравоохранение"),
    MANUFACTURING("Производство"),
    LOGISTICS("Логистика"),
    AGRICULTURE("Сельское хозяйство"),
    HORECA("Гостиницы и общепит"),
    MEDIA("Медиа и маркетинг");

    private final String displayName;
}
