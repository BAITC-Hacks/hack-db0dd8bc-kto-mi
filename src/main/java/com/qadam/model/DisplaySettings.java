package com.qadam.model;

/**
 * Frontend display settings applied when a child views a lesson adapted for a given profile.
 *
 * @param fontSizePx            base font size in pixels
 * @param lineHeight            line height multiplier
 * @param dyslexiaFont          whether a dyslexia-friendly font should be used
 * @param backgroundColor       page background color as a hex string, e.g. {@code #FDF6E3}
 * @param pictogramPerSentence  whether a pictogram is shown next to every sentence
 * @param showSequenceStructure whether sentences are grouped into First / Then / Finally sections
 */
public record DisplaySettings(
        int fontSizePx,
        double lineHeight,
        boolean dyslexiaFont,
        String backgroundColor,
        boolean pictogramPerSentence,
        boolean showSequenceStructure
) {
}
