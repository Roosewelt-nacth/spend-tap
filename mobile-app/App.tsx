import React, { useEffect, useState } from 'react';
import { SafeAreaView, StyleSheet, ActivityIndicator, View } from 'react-native';
import { AppConfig } from './src/lib/types';
import { getConfig, saveConfig, getDeviceId } from './src/lib/config';
import { initSupabase } from './src/lib/supabase';
import { upsertSettings } from './src/lib/db';
import { SetupScreen } from './src/screens/SetupScreen';
import { DashboardScreen } from './src/screens/DashboardScreen';

export default function App() {
  const [loading, setLoading] = useState(true);
  const [config, setConfig] = useState<AppConfig | null>(null);
  const [deviceId, setDeviceId] = useState('');
  const [showSetup, setShowSetup] = useState(false);

  useEffect(() => {
    (async () => {
      const cfg = await getConfig();
      const id = await getDeviceId();
      setDeviceId(id);
      setConfig(cfg);
      if (cfg.onboarded && cfg.supabaseUrl && cfg.supabaseAnonKey) {
        initSupabase(cfg.supabaseUrl, cfg.supabaseAnonKey);
        setShowSetup(false);
      } else {
        setShowSetup(true);
      }
      setLoading(false);
    })();
  }, []);

  async function handleSaveSetup(partial: Partial<AppConfig>) {
    const next = await saveConfig(partial);
    initSupabase(next.supabaseUrl, next.supabaseAnonKey);
    await upsertSettings(deviceId, next.categories, next.dailyBudget, next.weeklyBudget);
    setConfig(next);
    setShowSetup(false);
  }

  if (loading || !config) {
    return (
      <View style={s.loading}>
        <ActivityIndicator color="#6ee7b7" />
      </View>
    );
  }

  return (
    <SafeAreaView style={s.safe}>
      {showSetup ? (
        <SetupScreen initial={config} onSave={handleSaveSetup} />
      ) : (
        <DashboardScreen config={config} deviceId={deviceId} onOpenSettings={() => setShowSetup(true)} />
      )}
    </SafeAreaView>
  );
}

const s = StyleSheet.create({
  safe: { flex: 1, backgroundColor: '#14161b' },
  loading: { flex: 1, backgroundColor: '#14161b', justifyContent: 'center', alignItems: 'center' },
});
