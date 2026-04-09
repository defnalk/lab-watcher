/** Mirror of the Java SqliteStateRepository row shape. */
export interface ProcessedFile {
  id: number;
  file_path: string;
  file_name: string;
  sha256: string;
  file_size: number;
  processed_at: string;
  status: "PASS" | "FAIL" | "ERROR" | string;
  row_count: number;
  column_count: number;
  error_count: number;
  warning_count: number;
}

export interface DashboardStats {
  totalFiles: number;
  passCount: number;
  failCount: number;
  errorCount: number;
  passRate: number;
  lastProcessedAt: string | null;
}

export interface WsMessage {
  type: "new_file" | "snapshot";
  data: ProcessedFile | ProcessedFile[];
}
