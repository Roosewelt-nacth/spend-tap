import 'react-native-url-polyfill/auto';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { createClient, SupabaseClient } from '@supabase/supabase-js';

let client: SupabaseClient | null = null;

// The app is reconfigured with real credentials at runtime (see config.ts),
// since the user pastes their own Supabase project URL/key on first launch
// rather than us baking one project into the app.
export function initSupabase(url: string, anonKey: string): SupabaseClient {
  client = createClient(url, anonKey, {
    auth: {
      storage: AsyncStorage,
      autoRefreshToken: true,
      persistSession: true,
      detectSessionInUrl: false,
    },
  });
  return client;
}

export function getSupabase(): SupabaseClient {
  if (!client) {
    throw new Error('Supabase not initialized yet. Complete setup first.');
  }
  return client;
}
