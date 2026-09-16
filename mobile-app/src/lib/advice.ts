import { getSupabase } from './supabase';
import { SpendEntry } from './types';

// Calls the `get-advice` Supabase Edge Function instead of the Anthropic API
// directly — the API key lives server-side only (see supabase/functions/get-advice),
// so it's never bundled into the app that ships to a phone.
export async function getAdvice(
  entries: SpendEntry[],
  categories: string[],
  dailyBudget: number,
  weeklyBudget: number
): Promise<string> {
  const sb = getSupabase();
  const { data, error } = await sb.functions.invoke('get-advice', {
    body: { entries, categories, dailyBudget, weeklyBudget },
  });
  if (error) throw new Error(error.message);
  if (data?.error) throw new Error(data.error);
  return data?.advice || '';
}
