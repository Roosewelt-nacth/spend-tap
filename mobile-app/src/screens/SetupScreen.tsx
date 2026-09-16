import React, { useState } from 'react';
import { View, Text, TextInput, TouchableOpacity, StyleSheet, ScrollView, Alert } from 'react-native';
import { AppConfig } from '../lib/types';

interface Props {
  initial: AppConfig;
  onSave: (cfg: Partial<AppConfig>) => Promise<void>;
}

export function SetupScreen({ initial, onSave }: Props) {
  const [supabaseUrl, setSupabaseUrl] = useState(initial.supabaseUrl);
  const [supabaseAnonKey, setSupabaseAnonKey] = useState(initial.supabaseAnonKey);
  const [categories, setCategories] = useState<string[]>(initial.categories);
  const [newCategory, setNewCategory] = useState('');
  const [dailyBudget, setDailyBudget] = useState(String(initial.dailyBudget || ''));
  const [weeklyBudget, setWeeklyBudget] = useState(String(initial.weeklyBudget || ''));
  const [saving, setSaving] = useState(false);

  function addCategory() {
    const val = newCategory.trim();
    if (!val) return;
    if (categories.length >= 5) {
      Alert.alert('Max 5 categories');
      return;
    }
    setCategories([...categories, val]);
    setNewCategory('');
  }

  function removeCategory(idx: number) {
    setCategories(categories.filter((_, i) => i !== idx));
  }

  async function handleSave() {
    if (!supabaseUrl.trim() || !supabaseAnonKey.trim()) {
      Alert.alert('Supabase URL and anon key are required');
      return;
    }
    if (categories.length < 3) {
      Alert.alert('Add at least 3 categories');
      return;
    }
    setSaving(true);
    try {
      await onSave({
        supabaseUrl: supabaseUrl.trim(),
        supabaseAnonKey: supabaseAnonKey.trim(),
        categories,
        dailyBudget: parseFloat(dailyBudget) || 0,
        weeklyBudget: parseFloat(weeklyBudget) || 0,
        onboarded: true,
      });
    } catch (err) {
      Alert.alert('Setup failed', (err as Error).message);
    } finally {
      setSaving(false);
    }
  }

  return (
    <ScrollView style={s.container} contentContainerStyle={{ padding: 20 }}>
      <Text style={s.title}>Setup</Text>

      <Text style={s.label}>Supabase project URL</Text>
      <TextInput
        style={s.input}
        value={supabaseUrl}
        onChangeText={setSupabaseUrl}
        placeholder="https://xxxx.supabase.co"
        placeholderTextColor="#666"
        autoCapitalize="none"
      />

      <Text style={s.label}>Supabase anon key</Text>
      <TextInput
        style={s.input}
        value={supabaseAnonKey}
        onChangeText={setSupabaseAnonKey}
        placeholder="anon public key"
        placeholderTextColor="#666"
        autoCapitalize="none"
        secureTextEntry
      />

      <Text style={s.label}>Categories (3-5)</Text>
      <View style={s.chipRow}>
        {categories.map((c, i) => (
          <TouchableOpacity key={c + i} style={s.chip} onPress={() => removeCategory(i)}>
            <Text style={s.chipText}>{c} ✕</Text>
          </TouchableOpacity>
        ))}
      </View>
      <View style={s.row}>
        <TextInput
          style={[s.input, { flex: 1 }]}
          value={newCategory}
          onChangeText={setNewCategory}
          placeholder="Add category"
          placeholderTextColor="#666"
        />
        <TouchableOpacity style={s.addBtn} onPress={addCategory}>
          <Text style={s.addBtnText}>+</Text>
        </TouchableOpacity>
      </View>

      <Text style={s.label}>Budgets (₹)</Text>
      <View style={s.row}>
        <TextInput
          style={[s.input, { flex: 1 }]}
          value={dailyBudget}
          onChangeText={setDailyBudget}
          placeholder="Daily"
          placeholderTextColor="#666"
          keyboardType="numeric"
        />
        <TextInput
          style={[s.input, { flex: 1 }]}
          value={weeklyBudget}
          onChangeText={setWeeklyBudget}
          placeholder="Weekly"
          placeholderTextColor="#666"
          keyboardType="numeric"
        />
      </View>

      <TouchableOpacity style={s.saveBtn} onPress={handleSave} disabled={saving}>
        <Text style={s.saveBtnText}>{saving ? 'Saving...' : 'Save & Continue'}</Text>
      </TouchableOpacity>
    </ScrollView>
  );
}

const s = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#14161b' },
  title: { fontSize: 22, fontWeight: '700', color: '#f0f1f4', marginBottom: 16 },
  label: { fontSize: 12, color: '#9a9da6', textTransform: 'uppercase', marginTop: 14, marginBottom: 6 },
  input: {
    backgroundColor: '#1f222a',
    borderColor: '#2a2d35',
    borderWidth: 1,
    borderRadius: 8,
    padding: 10,
    color: '#f0f1f4',
    marginBottom: 8,
  },
  row: { flexDirection: 'row', gap: 8 },
  chipRow: { flexDirection: 'row', flexWrap: 'wrap', gap: 6, marginBottom: 6 },
  chip: {
    backgroundColor: '#1f222a',
    borderColor: '#2a2d35',
    borderWidth: 1,
    borderRadius: 999,
    paddingVertical: 4,
    paddingHorizontal: 10,
  },
  chipText: { color: '#f0f1f4', fontSize: 12 },
  addBtn: {
    backgroundColor: '#1f222a',
    borderColor: '#2a2d35',
    borderWidth: 1,
    borderRadius: 8,
    paddingHorizontal: 16,
    justifyContent: 'center',
  },
  addBtnText: { color: '#f0f1f4', fontSize: 18 },
  saveBtn: { backgroundColor: '#6ee7b7', borderRadius: 8, padding: 14, marginTop: 20, alignItems: 'center' },
  saveBtnText: { color: '#0a2f22', fontWeight: '700' },
});
