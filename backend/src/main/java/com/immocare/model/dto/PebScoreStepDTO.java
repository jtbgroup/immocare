package com.immocare.model.dto;

import com.immocare.model.entity.PebScore;
import java.time.LocalDate;

/**
 * Represents a single transition between two consecutive PEB scores.
 * UC004 - US020 Track PEB Score Improvements.
 */
public class PebScoreStepDTO {

    private PebScore fromScore;
    private PebScore toScore;

    /** IMPROVED | DEGRADED | UNCHANGED */
    private String direction;

    private LocalDate date;

    public PebScoreStepDTO() {}

    public PebScoreStepDTO(PebScore fromScore, PebScore toScore, String direction, LocalDate date) {
        this.fromScore = fromScore;
        this.toScore = toScore;
        this.direction = direction;
        this.date = date;
    }

    public PebScore getFromScore() { return fromScore; }
    public void setFromScore(PebScore fromScore) { this.fromScore = fromScore; }

    public PebScore getToScore() { return toScore; }
    public void setToScore(PebScore toScore) { this.toScore = toScore; }

    public String getDirection() { return direction; }
    public void setDirection(String direction) { this.direction = direction; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
}
