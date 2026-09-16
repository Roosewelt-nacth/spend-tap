import AsyncStorage from '@react-native-async-storage/async-storage';
import 'react-native-get-random-values';
import { v4 as uuidv4 } from 'uuid';
import { AppConfig } from './types';

const KEY = 'spend-tracker-config-v1';
const DEVICE_ID_KEY = 'spend-tracker-device-id-v1';

const defaultConfig: AppConfig = {
  onboarded: false,
  supabaseUrl: '',
  supabaseAnonKey: '',
  categories: [],
  dailyBudget: 0,
  weeklyBudget: 0,
};

export async function getDeviceId(): Promise<string> {
  let id = await AsyncStorage.getItem(DEVICE_ID_KEY);
  if (!id) {
    id = uuidv4();
    await AsyncStorage.setItem(DEVICE_ID_KEY, id);
  }
  return id;
}

export async function getConfig(): Promise<AppConfig> {
  const raw = await AsyncStorage.getItem(KEY);
  if (!raw) return defaultConfig;
  try {
    return { ...defaultConfig, ...JSON.parse(raw) };
  } catch {
    return defaultConfig;
  }
}

export async function saveConfig(partial: Partial<AppConfig>): Promise<AppConfig> {
  const current = await getConfig();
  const next = { ...current, ...partial };
  await AsyncStorage.setItem(KEY, JSON.stringify(next));
  return next;
}
