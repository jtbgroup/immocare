package com.immocare.model.entity;

import com.immocare.model.enums.TagMatchField;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "tag_learning_rule")
public class TagLearningRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "match_field", nullable = false, length = 30)
    private TagMatchField matchField;

    @Column(name = "match_value", nullable = false, length = 200)
    private String matchValue;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subcategory_id", nullable = false)
    private TagSubcategory subcategory;

    @Column(nullable = false)
    private int confidence = 1;

    @Column(name = "last_matched_at")
    private LocalDateTime lastMatchedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public TagMatchField getMatchField() { return matchField; }
    public void setMatchField(TagMatchField matchField) { this.matchField = matchField; }
    public String getMatchValue() { return matchValue; }
    public void setMatchValue(String matchValue) { this.matchValue = matchValue; }
    public TagSubcategory getSubcategory() { return subcategory; }
    public void setSubcategory(TagSubcategory subcategory) { this.subcategory = subcategory; }
    public int getConfidence() { return confidence; }
    public void setConfidence(int confidence) { this.confidence = confidence; }
    public LocalDateTime getLastMatchedAt() { return lastMatchedAt; }
    public void setLastMatchedAt(LocalDateTime lastMatchedAt) { this.lastMatchedAt = lastMatchedAt; }
}
