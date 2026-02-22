package com.immocare.model.entity;

/**
 * Energy Performance Certificate (PEB) score values.
 * Ordered from best (A_PLUS_PLUS) to worst (G).
 * UC004 - Manage PEB Scores.
 */
public enum PebScore {
    A_PLUS_PLUS,
    A_PLUS,
    A,
    B,
    C,
    D,
    E,
    F,
    G;

    /**
     * Returns the number of grades between two scores.
     * Positive = improvement (other is better than this).
     * Negative = degradation.
     */
    public int gradesTo(PebScore other) {
        return this.ordinal() - other.ordinal();
    }
}
