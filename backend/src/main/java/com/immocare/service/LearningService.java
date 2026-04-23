package com.immocare.service;

import com.immocare.model.dto.AccountingMonthSuggestionDTO;
import com.immocare.model.dto.SubcategorySuggestionDTO;
import com.immocare.model.entity.AccountingMonthRule;
import com.immocare.model.entity.TagLearningRule;
import com.immocare.model.entity.TagSubcategory;
import com.immocare.model.enums.SubcategoryDirection;
import com.immocare.model.enums.TagMatchField;
import com.immocare.model.enums.TransactionDirection;
import com.immocare.repository.AccountingMonthRuleRepository;
import com.immocare.repository.TagLearningRuleRepository;
import com.immocare.repository.TagSubcategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class LearningService {

    private final TagLearningRuleRepository learningRuleRepository;
    private final AccountingMonthRuleRepository accountingMonthRuleRepository;
    private final TagSubcategoryRepository tagSubcategoryRepository;

    public LearningService(TagLearningRuleRepository learningRuleRepository,
            AccountingMonthRuleRepository accountingMonthRuleRepository,
            TagSubcategoryRepository tagSubcategoryRepository) {
        this.learningRuleRepository = learningRuleRepository;
        this.accountingMonthRuleRepository = accountingMonthRuleRepository;
        this.tagSubcategoryRepository = tagSubcategoryRepository;
    }

    /**
     * Suggest a subcategory based on transaction fields.
     * Priority order (BR-UC015-18/19):
     * 1. ASSET_TYPE (if assetType non-null) — exact match on match_value
     * 2. COUNTERPARTY_ACCOUNT (if non-blank) — exact match
     * 3. DESCRIPTION (if non-blank) — contains match
     * Results filtered by direction compatibility and deduplicated by subcategoryId.
     */
    public List<SubcategorySuggestionDTO> suggestSubcategory(
            String counterpartyAccount,
            String description,
            String assetType,
            TransactionDirection direction,
            int minConfidence) {

        Map<Long, SubcategorySuggestionDTO> best = new LinkedHashMap<>();

        if (assetType != null && !assetType.isBlank()) {
            findSuggestionsForField(TagMatchField.ASSET_TYPE, assetType, direction, minConfidence, best);
        }
        if (counterpartyAccount != null && !counterpartyAccount.isBlank()) {
            findSuggestionsForField(TagMatchField.COUNTERPARTY_ACCOUNT, counterpartyAccount, direction, minConfidence,
                    best);
        }
        if (description != null && !description.isBlank()) {
            findSuggestionsForField(TagMatchField.DESCRIPTION, description, direction, minConfidence, best);
        }

        return best.values().stream()
                .sorted(Comparator.comparingInt(SubcategorySuggestionDTO::confidence).reversed())
                .toList();
    }

    /**
     * Overload without assetType for backward compatibility with existing callers.
     */
    public List<SubcategorySuggestionDTO> suggestSubcategory(
            String counterpartyAccount,
            String description,
            TransactionDirection direction,
            int minConfidence) {
        return suggestSubcategory(counterpartyAccount, description, null, direction, minConfidence);
    }

    private void findSuggestionsForField(TagMatchField field, String value, TransactionDirection direction,
            int minConf, Map<Long, SubcategorySuggestionDTO> best) {
        learningRuleRepository.findSuggestions(field, value, minConf).forEach(rule -> {
            TagSubcategory sub = rule.getSubcategory();
            if (isCompatible(sub.getDirection(), direction)) {
                best.merge(sub.getId(),
                        new SubcategorySuggestionDTO(sub.getId(), sub.getName(),
                                sub.getCategory().getId(), sub.getCategory().getName(), rule.getConfidence()),
                        (existing, newer) -> existing.confidence() >= newer.confidence() ? existing : newer);
            }
        });
    }

    private boolean isCompatible(SubcategoryDirection subDir, TransactionDirection txDir) {
        return subDir == SubcategoryDirection.BOTH
                || (subDir == SubcategoryDirection.INCOME && txDir == TransactionDirection.INCOME)
                || (subDir == SubcategoryDirection.EXPENSE && txDir == TransactionDirection.EXPENSE);
    }

    public AccountingMonthSuggestionDTO suggestAccountingMonth(Long subcategoryId, String counterpartyAccount) {
        if (subcategoryId == null) {
            return new AccountingMonthSuggestionDTO(LocalDate.now().withDayOfMonth(1), 0);
        }
        String cp = (counterpartyAccount != null && !counterpartyAccount.isBlank()) ? counterpartyAccount : null;
        List<AccountingMonthRule> rules = cp != null
                ? accountingMonthRuleRepository.findBestMatch(subcategoryId, cp)
                : List.of();

        if (rules.isEmpty()) {
            Optional<AccountingMonthRule> generic = accountingMonthRuleRepository
                    .findBySubcategoryIdAndCounterpartyAccountIsNull(subcategoryId);
            if (generic.isEmpty()) {
                return new AccountingMonthSuggestionDTO(LocalDate.now().withDayOfMonth(1), 0);
            }
            AccountingMonthRule rule = generic.get();
            return new AccountingMonthSuggestionDTO(
                    LocalDate.now().plusMonths(rule.getMonthOffset()).withDayOfMonth(1),
                    rule.getConfidence());
        }

        AccountingMonthRule rule = rules.get(0);
        return new AccountingMonthSuggestionDTO(
                LocalDate.now().plusMonths(rule.getMonthOffset()).withDayOfMonth(1),
                rule.getConfidence());
    }

    @Transactional
    public void reinforceTagRule(Long subcategoryId, TagMatchField field, String matchValue) {
        if (matchValue == null || matchValue.isBlank()) return;
        TagSubcategory sub = tagSubcategoryRepository.findById(subcategoryId).orElse(null);
        if (sub == null) return;

        TagLearningRule rule = learningRuleRepository
                .findByMatchFieldAndMatchValueIgnoreCaseAndSubcategoryId(field, matchValue, subcategoryId)
                .orElseGet(() -> {
                    TagLearningRule r = new TagLearningRule();
                    r.setMatchField(field);
                    r.setMatchValue(matchValue);
                    r.setSubcategory(sub);
                    r.setConfidence(0);
                    return r;
                });
        rule.setConfidence(rule.getConfidence() + 1);
        rule.setLastMatchedAt(LocalDateTime.now());
        learningRuleRepository.save(rule);
    }

    /**
     * Convenience overload for the most common case (COUNTERPARTY_ACCOUNT).
     */
    @Transactional
    public void reinforceTagRule(Long subcategoryId, String counterpartyAccount) {
        reinforceTagRule(subcategoryId, TagMatchField.COUNTERPARTY_ACCOUNT, counterpartyAccount);
    }

    @Transactional
    public void reinforceAccountingMonthRule(Long subcategoryId, String counterpartyAccount, int offset) {
        TagSubcategory sub = tagSubcategoryRepository.findById(subcategoryId).orElse(null);
        if (sub == null) return;

        boolean isGeneric = counterpartyAccount == null || counterpartyAccount.isBlank();

        AccountingMonthRule rule;
        if (isGeneric) {
            rule = accountingMonthRuleRepository
                    .findBySubcategoryIdAndCounterpartyAccountIsNull(subcategoryId)
                    .orElseGet(() -> {
                        AccountingMonthRule r = new AccountingMonthRule();
                        r.setSubcategory(sub);
                        r.setMonthOffset(offset);
                        r.setConfidence(0);
                        return r;
                    });
        } else {
            rule = accountingMonthRuleRepository
                    .findBySubcategoryIdAndCounterpartyAccountIgnoreCase(subcategoryId, counterpartyAccount)
                    .orElseGet(() -> {
                        AccountingMonthRule r = new AccountingMonthRule();
                        r.setSubcategory(sub);
                        r.setCounterpartyAccount(counterpartyAccount);
                        r.setMonthOffset(offset);
                        r.setConfidence(0);
                        return r;
                    });
        }
        rule.setMonthOffset(offset);
        rule.setConfidence(rule.getConfidence() + 1);
        rule.setLastMatchedAt(LocalDateTime.now());
        accountingMonthRuleRepository.save(rule);
    }
}
