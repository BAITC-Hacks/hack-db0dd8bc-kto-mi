package com.qadam.dto;

import com.qadam.model.AdaptationProfile;
import com.qadam.model.DisplaySettings;

/**
 * An adaptation profile the teacher can choose.
 *
 * @param code value to send as {@code profile} when creating a lesson
 * @param name human-readable profile name
 */
public record ProfileResponse(
        AdaptationProfile code,
        String name,
        DisplaySettings displaySettings
) {

    public static ProfileResponse of(AdaptationProfile profile) {
        return new ProfileResponse(profile, profile.getDisplayName(), profile.getDisplaySettings());
    }
}
