package com.immocare.model.dto;

import com.immocare.model.entity.PebScore;
import java.time.LocalDate;
import java.util.List;

/**
 * Summary of PEB score improvements over time for a housing unit.
 * UC004 - US020 Track PEB Score Improvements.
 */
public class PebImprovementDTO {

    private PebScore firstScore;
    private LocalDate firstScoreDate;
    private PebScore currentScore;
    private LocalDate currentScoreDate;

    /** Positive = improvement, negative = degradation, 0 = no change */
    private int gradesImproved;

    private int yearsCovered;

    private List<PebScoreStepDTO> history;

    // Getters and setters

    public PebScore getFirstScore() { return firstScore; }
    public void setFirstScore(PebScore firstScore) { this.firstScore = firstScore; }

    public LocalDate getFirstScoreDate() { return firstScoreDate; }
    public void setFirstScoreDate(LocalDate firstScoreDate) { this.firstScoreDate = firstScoreDate; }

    public PebScore getCurrentScore() { return currentScore; }
    public void setCurrentScore(PebScore currentScore) { this.currentScore = currentScore; }

    public LocalDate getCurrentScoreDate() { return currentScoreDate; }
    public void setCurrentScoreDate(LocalDate currentScoreDate) { this.currentScoreDate = currentScoreDate; }

    public int getGradesImproved() { return gradesImproved; }
    public void setGradesImproved(int gradesImproved) { this.gradesImproved = gradesImproved; }

    public int getYearsCovered() { return yearsCovered; }
    public void setYearsCovered(int yearsCovered) { this.yearsCovered = yearsCovered; }

    public List<PebScoreStepDTO> getHistory() { return history; }
    public void setHistory(List<PebScoreStepDTO> history) { this.history = history; }
}
