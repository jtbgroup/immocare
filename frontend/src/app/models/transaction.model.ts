export type TransactionDirection = "INCOME" | "EXPENSE";
export type TransactionStatus = "DRAFT" | "CONFIRMED" | "RECONCILED";
export type TransactionSource = "MANUAL" | "IMPORT";
export type AssetType = "BOILER" | "FIRE_EXTINGUISHER" | "METER";
export type SubcategoryDirection = "INCOME" | "EXPENSE" | "BOTH";
export type BankAccountType = "CURRENT" | "SAVINGS";

// ─── Import Parser ────────────────────────────────────────────────────────────

export interface ImportParser {
  id: number;
  code: string;
  label: string;
  description: string;
  format: "CSV" | "PDF";
  bankHint?: string;
  active: boolean;
}

// ─── Bank Account ─────────────────────────────────────────────────────────────

export interface BankAccount {
  id: number;
  label: string;
  accountNumber: string;
  type: BankAccountType;
  isActive: boolean;
  ownerUserId?: number;
}

// ─── Tags ─────────────────────────────────────────────────────────────────────

export interface TagCategory {
  id: number;
  name: string;
  description?: string;
  subcategoryCount: number;
}

export interface TagSubcategory {
  id: number;
  categoryId: number;
  categoryName: string;
  name: string;
  direction: SubcategoryDirection;
  description?: string;
  usageCount: number;
}

// ─── Transaction ──────────────────────────────────────────────────────────────

export interface TransactionAssetLink {
  id?: number;
  assetType: AssetType;
  assetId: number;
  assetLabel?: string;
  notes?: string;
}

export interface FinancialTransactionSummary {
  id: number;
  reference: string;
  transactionDate: string;
  accountingMonth: string;
  direction: TransactionDirection;
  amount: number;
  status: TransactionStatus;
  source: TransactionSource;
  bankAccountLabel?: string;
  categoryName?: string;
  subcategoryName?: string;
  buildingName?: string;
  unitNumber?: string;
  leaseId?: number;
  suggestedLeaseId?: number;
  buildingId?: number;
  housingUnitId?: number;
}

export interface FinancialTransaction extends FinancialTransactionSummary {
  externalReference?: string;
  valueDate?: string;
  description?: string;
  counterpartyAccount?: string;
  bankAccountId?: number;
  subcategoryId?: number;
  categoryId?: number;
  leaseId?: number;
  suggestedLeaseId?: number;
  housingUnitId?: number;
  buildingId?: number;
  importBatchId?: number;
  parserCode?: string;
  assetLinks: TransactionAssetLink[];
  editable: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface PagedTransactionResponse {
  content: FinancialTransactionSummary[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  totalIncome: number;
  totalExpenses: number;
  netBalance: number;
}

// ─── Statistics ───────────────────────────────────────────────────────────────

export interface TransactionStatistics {
  totalIncome: number;
  totalExpenses: number;
  netBalance: number;
  byCategory: CategoryBreakdown[];
  byBuilding: BuildingBreakdown[];
  byUnit: UnitBreakdown[];
  byBankAccount: BankAccountBreakdown[];
  monthlyTrend: MonthlyTrend[];
}

export interface CategoryBreakdown {
  categoryId: number;
  categoryName: string;
  subcategories: SubcategoryBreakdown[];
  categoryTotal: number;
}

export interface SubcategoryBreakdown {
  subcategoryId: number;
  subcategoryName: string;
  direction: SubcategoryDirection;
  amount: number;
  transactionCount: number;
  percentage: number;
}

export interface BuildingBreakdown {
  buildingId?: number;
  buildingName: string;
  income: number;
  expenses: number;
  balance: number;
}

export interface UnitBreakdown {
  unitId?: number;
  unitNumber: string;
  buildingName: string;
  income: number;
  expenses: number;
  balance: number;
}

export interface BankAccountBreakdown {
  bankAccountId?: number;
  label: string;
  type?: BankAccountType;
  income: number;
  expenses: number;
  balance: number;
}

export interface MonthlyTrend {
  year: number;
  month: number;
  income: number;
  expenses: number;
}

// ─── Import ───────────────────────────────────────────────────────────────────

export interface ImportBatchResult {
  batchId: number | null;
  totalRows: number;
  importedCount: number;
  duplicateCount: number;
  errorCount: number;
  errors: { rowNumber: number; rawLine: string; errorMessage: string }[];
}

export interface SubcategorySuggestion {
  subcategoryId: number;
  subcategoryName: string;
  categoryId: number;
  categoryName: string;
  confidence: number;
}

export interface ImportPreviewSuggestedLease {
  leaseId: number;
  unitId: number;
  unitNumber: string;
  buildingId?: number;
  buildingName: string;
  personId: number;
  personFullName: string;
}

export interface ImportPreviewRow {
  rowNumber: number;
  rawLine: string;
  transactionDate: string | null;
  amount: number | null;
  direction: TransactionDirection | null;
  description: string | null;
  counterpartyAccount: string | null;
  fingerprint: string | null;
  duplicateInDb: boolean;
  duplicateTransactionId?: number;
  suggestedSubcategory: SubcategorySuggestion | null;
  suggestedLease: ImportPreviewSuggestedLease | null;
  parseError: string | null;
  /** Client-side only — whether the row is checked for import. */
  selected: boolean;

  // ── Client-side enrichment (set via detail panel) ─────────────────────────
  enrichedSubcategoryId?: number;
  enrichedSubcategoryName?: string;
  enrichedLeaseId?: number;
  enrichedUnitId?: number;
  enrichedUnitNumber?: string;
  enrichedBuildingId?: number;
  enrichedBuildingName?: string;
}

// ─── Enrichment payload sent to /import ──────────────────────────────────────

/** Mirrors ImportRowEnrichmentDTO on the backend. */
export interface ImportRowEnrichment {
  fingerprint: string;
  subcategoryId?: number;
  leaseId?: number;
  housingUnitId?: number;
  buildingId?: number;
  directionOverride?: string;
}

// ─── Filters ──────────────────────────────────────────────────────────────────

export interface TransactionFilter {
  direction?: TransactionDirection;
  from?: string;
  to?: string;
  accountingFrom?: string;
  accountingTo?: string;
  categoryId?: number;
  subcategoryId?: number;
  bankAccountId?: number;
  buildingId?: number;
  unitId?: number;
  status?: TransactionStatus;
  search?: string;
  importBatchId?: number;
  assetType?: AssetType;
  assetId?: number;
  page?: number;
  size?: number;
  sort?: string;
}

export interface StatisticsFilter {
  accountingFrom?: string;
  accountingTo?: string;
  buildingId?: number;
  unitId?: number;
  bankAccountId?: number;
  direction?: TransactionDirection;
}

// ─── Requests ─────────────────────────────────────────────────────────────────

export interface CreateTransactionRequest {
  direction: TransactionDirection;
  transactionDate: string;
  valueDate?: string;
  accountingMonth: string;
  amount: number;
  description?: string;
  counterpartyAccount?: string;
  bankAccountId?: number;
  subcategoryId?: number;
  leaseId?: number;
  housingUnitId?: number;
  buildingId?: number;
  assetLinks?: TransactionAssetLink[];
}

export interface ConfirmTransactionRequest {
  subcategoryId?: number;
  accountingMonth?: string;
  leaseId?: number;
  buildingId?: number;
  housingUnitId?: number;
}

export interface SaveTagCategoryRequest {
  name: string;
  description?: string;
}

export interface SaveTagSubcategoryRequest {
  categoryId: number;
  name: string;
  direction: SubcategoryDirection;
  description?: string;
}

export interface SaveBankAccountRequest {
  label: string;
  accountNumber: string;
  type: BankAccountType;
  isActive: boolean;
}

// ─── Labels ───────────────────────────────────────────────────────────────────

export const DIRECTION_LABELS: Record<TransactionDirection, string> = {
  INCOME: "Income",
  EXPENSE: "Expense",
};

export const STATUS_LABELS: Record<TransactionStatus, string> = {
  DRAFT: "Draft",
  CONFIRMED: "Confirmed",
  RECONCILED: "Reconciled",
};

export const BANK_ACCOUNT_TYPE_LABELS: Record<BankAccountType, string> = {
  CURRENT: "Current",
  SAVINGS: "Savings",
};
