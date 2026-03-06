package com.immocare.model.dto;

import com.immocare.model.enums.BankAccountType;
import com.immocare.model.enums.SubcategoryDirection;
import java.math.BigDecimal;
import java.util.List;

public record TransactionStatisticsDTO(
    BigDecimal totalIncome,
    BigDecimal totalExpenses,
    BigDecimal netBalance,
    List<CategoryBreakdownDTO> byCategory,
    List<BuildingBreakdownDTO> byBuilding,
    List<UnitBreakdownDTO> byUnit,
    List<BankAccountBreakdownDTO> byBankAccount,
    List<MonthlyTrendDTO> monthlyTrend
) {

    public record CategoryBreakdownDTO(
        Long categoryId,
        String categoryName,
        List<SubcategoryBreakdownDTO> subcategories,
        BigDecimal categoryTotal
    ) {}

    public record SubcategoryBreakdownDTO(
        Long subcategoryId,
        String subcategoryName,
        SubcategoryDirection direction,
        BigDecimal amount,
        long transactionCount,
        double percentage
    ) {}

    public record BuildingBreakdownDTO(
        Long buildingId,
        String buildingName,
        BigDecimal income,
        BigDecimal expenses,
        BigDecimal balance
    ) {}

    public record UnitBreakdownDTO(
        Long unitId,
        String unitNumber,
        String buildingName,
        BigDecimal income,
        BigDecimal expenses,
        BigDecimal balance
    ) {}

    public record BankAccountBreakdownDTO(
        Long bankAccountId,
        String label,
        BankAccountType type,
        BigDecimal income,
        BigDecimal expenses,
        BigDecimal balance
    ) {}

    public record MonthlyTrendDTO(int year, int month, BigDecimal income, BigDecimal expenses) {}
}
