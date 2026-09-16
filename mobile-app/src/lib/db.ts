import { getSupabase } from './supabase';
import { SpendEntry, PaymentMethod } from './types';

export async function upsertSettings(
  deviceId: string,
  categories: string[],
  dailyBudget: number,
  weeklyBudget: number
) {
  const sb = getSupabase();
  const { error } = await sb.from('settings').upsert({
    device_id: deviceId,
    categories,
    daily_budget: dailyBudget,
    weekly_budget: weeklyBudget,
    updated_at: new Date().toISOString(),
  });
  if (error) throw new Error(error.message);
}

export async function addEntry(
  deviceId: string,
  entry: { amount: number; category: string; note: string; paymentMethod: PaymentMethod; isImpulse: boolean }
): Promise<SpendEntry> {
  const sb = getSupabase();
  const { data, error } = await sb
    .from('entries')
    .insert({
      device_id: deviceId,
      amount: entry.amount,
      category: entry.category,
      note: entry.note,
      payment_method: entry.paymentMethod,
      is_impulse: entry.isImpulse,
    })
    .select()
    .single();
  if (error) throw new Error(error.message);
  return data as SpendEntry;
}

export async function listEntries(deviceId: string, since?: string): Promise<SpendEntry[]> {
  const sb = getSupabase();
  let query = sb
    .from('entries')
    .select('*')
    .eq('device_id', deviceId)
    .order('created_at', { ascending: false });
  if (since) query = query.gte('created_at', since);
  const { data, error } = await query;
  if (error) throw new Error(error.message);
  return (data || []) as SpendEntry[];
}
