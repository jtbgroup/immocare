export type PebScore = 'A_PLUS_PLUS' | 'A_PLUS' | 'A' | 'B' | 'C' | 'D' | 'E' | 'F' | 'G';
export type PebStatus = 'CURRENT' | 'HISTORICAL' | 'EXPIRED';
export type ExpiryWarning = 'EXPIRED' | 'EXPIRING_SOON' | 'VALID' | 'NO_DATE';
export type ImprovementDirection = 'IMPROVED' | 'DEGRADED' | 'UNCHANGED';

export interface PebScoreDTO {
  id: number;
  housingUnitId: number;
  pebScore: PebScore;
  scoreDate: string;
  certificateNumber: string | null;
  validUntil: string | null;
  createdAt: string;
  status: PebStatus;
  expiryWarning: ExpiryWarning;
}

export interface CreatePebScoreRequest {
  pebScore: PebScore;
  scoreDate: string;
  certificateNumber?: string | null;
  validUntil?: string | null;
}

export interface PebScoreStepDTO {
  fromScore: PebScore;
  toScore: PebScore;
  direction: ImprovementDirection;
  date: string;
}

export interface PebImprovementDTO {
  firstScore: PebScore;
  firstScoreDate: string;
  currentScore: PebScore;
  currentScoreDate: string;
  gradesImproved: number;
  yearsCovered: number;
  history: PebScoreStepDTO[];
}

export const PEB_SCORE_DISPLAY: Record<PebScore, { label: string; color: string; textColor: string }> = {
  A_PLUS_PLUS: { label: 'A++', color: '#1a7a1a', textColor: '#fff' },
  A_PLUS:      { label: 'A+',  color: '#2d9e2d', textColor: '#fff' },
  A:           { label: 'A',   color: '#4caf50', textColor: '#fff' },
  B:           { label: 'B',   color: '#8bc34a', textColor: '#333' },
  C:           { label: 'C',   color: '#ffeb3b', textColor: '#333' },
  D:           { label: 'D',   color: '#ff9800', textColor: '#fff' },
  E:           { label: 'E',   color: '#f44336', textColor: '#fff' },
  F:           { label: 'F',   color: '#d32f2f', textColor: '#fff' },
  G:           { label: 'G',   color: '#7b1fa2', textColor: '#fff' },
};

export const PEB_SCORE_ORDER: PebScore[] = [
  'A_PLUS_PLUS', 'A_PLUS', 'A', 'B', 'C', 'D', 'E', 'F', 'G'
];
