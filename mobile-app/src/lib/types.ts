export type PaymentMethod = 'upi' | 'cash';

export interface SpendEntry {
  id: string;
  device_id: string;
  amount: number;
  category: string;
  note: string;
  payment_method: PaymentMethod;
  is_impulse: boolean;
  created_at: string;
}

export interface Settings {
  device_id: string;
  categories: string[];
  daily_budget: number;
  weekly_budget: number;
}

export interface AppConfig {
  onboarded: boolean;
  supabaseUrl: string;
  supabaseAnonKey: string;
  categories: string[];
  dailyBudget: number;
  weeklyBudget: number;
}
