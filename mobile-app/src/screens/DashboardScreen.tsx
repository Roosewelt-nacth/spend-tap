import React, { useCallback, useEffect, useState } from 'react';
import {
  View,
  Text,
  TextInput,
  TouchableOpacity,
  StyleSheet,
  ScrollView,
  Switch,
  Alert,
  RefreshControl,
} from 'react-native';
import { AppConfig, PaymentMethod, SpendEntry } from '../lib/types';
import { addEntry, listEntries } from '../lib/db';
import { getAdvice } from '../lib/advice';
import { buildSummary, syncWidget } from '../lib/widgetSync';

interface Props {
  config: AppConfig;
  deviceId: string;
  onOpenSettings: () => void;
}

export function DashboardScreen({ config, deviceId, onOpenSettings }: Props) {
  const [entries, setEntries] = useState<SpendEntry[]>([]);
  const [amount, setAmount] = useState('');
  const [category, setCategory] = useState(config.categories[0] || '');
  const [note, setNote] = useState('');
  const [paymentMethod, setPaymentMethod] = useState<PaymentMethod>('upi');
  const [isImpulse, setIsImpulse] = useState(false);
  const [refreshing, setRefreshing] = useState(false);
  const [advice, setAdvice] = useState('');
  const [adviceLoading, setAdviceLoading] = useState(false);

  const refresh = useCallback(async () => {
    setRefreshing(true);
    try {
      const list = await listEntries(deviceId);
      setEntries(list);
      const summary = buildSummary(list, config.dailyBudget, config.weeklyBudget);
      await syncWidget(summary);
    } catch (err) {
      Alert.alert('Failed to load', (err as Error).message);
    } finally {
      setRefreshing(false);
    }
  }, [deviceId, config.dailyBudget, config.weeklyBudget]);

  useEffect(() => {
    refresh();
  }, [refresh]);

  async function handleAdd() {
    const value = parseFloat(amount);
    if (!value || value <= 0) {
      Alert.alert('Enter a valid amount');
      return;
    }
    if (!category) {
      Alert.alert('Pick a category');
      return;
    }
    try {
      await addEntry(deviceId, { amount: value, category, note, paymentMethod, isImpulse });
      setAmount('');
      setNote('');
      setIsImpulse(false);
      await refresh();
    } catch (err) {
      Alert.alert('Could not add entry', (err as Error).message);
    }
  }

  async function handleAdvice() {
    setAdviceLoading(true);
    setAdvice('');
    try {
      const text = await getAdvice(entries, config.categories, config.dailyBudget, config.weeklyBudget);
      setAdvice(text);
    } catch (err) {
      setAdvice(`⚠ ${(err as Error).message}`);
    } finally {
      setAdviceLoading(false);
    }
  }

  const summary = buildSummary(entries, config.dailyBudget, config.weeklyBudget);
  const dailyPct = config.dailyBudget > 0 ? Math.min(1, summary.todaySpend / config.dailyBudget) : 0;
  const weeklyPct = config.weeklyBudget > 0 ? Math.min(1, summary.weeklySpend / config.weeklyBudget) : 0;
  const barColor = (pct: number) => (pct >= 1 ? '#f87171' : pct >= 0.7 ? '#fbbf24' : '#6ee7b7');

  return (
    <ScrollView
      style={s.container}
      contentContainerStyle={{ padding: 16 }}
      refreshControl={<RefreshControl refreshing={refreshing} onRefresh={refresh} />}
    >
      <View style={s.headerRow}>
        <Text style={s.title}>Spend Tracker</Text>
        <TouchableOpacity onPress={onOpenSettings}>
          <Text style={s.settingsIcon}>⚙️</Text>
        </TouchableOpacity>
      </View>

      <View style={s.budgetPanel}>
        <View style={s.budgetRow}>
          <Text style={s.budgetLabel}>Today</Text>
          <Text style={s.budgetValue}>₹{summary.todaySpend.toFixed(0)} / ₹{config.dailyBudget}</Text>
        </View>
        <View style={s.bar}>
          <View style={[s.barFill, { width: `${Math.max(4, dailyPct * 100)}%`, backgroundColor: barColor(dailyPct) }]} />
        </View>
        <View style={[s.budgetRow, { marginTop: 10 }]}>
          <Text style={s.budgetLabelMuted}>This week</Text>
          <Text style={s.budgetValueMuted}>₹{summary.weeklySpend.toFixed(0)} / ₹{config.weeklyBudget}</Text>
        </View>
        <View style={s.bar}>
          <View style={[s.barFill, { width: `${Math.max(4, weeklyPct * 100)}%`, backgroundColor: barColor(weeklyPct) }]} />
        </View>
      </View>

      <Text style={s.sectionLabel}>Add a spend</Text>
      <TextInput
        style={s.input}
        value={amount}
        onChangeText={setAmount}
        placeholder="Amount ₹"
        placeholderTextColor="#666"
        keyboardType="numeric"
      />
      <View style={s.chipRow}>
        {config.categories.map((c) => (
          <TouchableOpacity
            key={c}
            style={[s.chip, category === c && s.chipActive]}
            onPress={() => setCategory(c)}
          >
            <Text style={[s.chipText, category === c && s.chipTextActive]}>{c}</Text>
          </TouchableOpacity>
        ))}
      </View>
      <TextInput
        style={s.input}
        value={note}
        onChangeText={setNote}
        placeholder="Why this spend? (note)"
        placeholderTextColor="#666"
      />
      <View style={s.row}>
        <TouchableOpacity
          style={[s.pill, paymentMethod === 'upi' && s.pillActive]}
          onPress={() => setPaymentMethod('upi')}
        >
          <Text style={s.pillText}>UPI</Text>
        </TouchableOpacity>
        <TouchableOpacity
          style={[s.pill, paymentMethod === 'cash' && s.pillActive]}
          onPress={() => setPaymentMethod('cash')}
        >
          <Text style={s.pillText}>Cash</Text>
        </TouchableOpacity>
        <View style={[s.pill, { marginLeft: 'auto', flexDirection: 'row', alignItems: 'center', gap: 6 }]}>
          <Text style={s.pillText}>Impulse</Text>
          <Switch value={isImpulse} onValueChange={setIsImpulse} />
        </View>
      </View>
      <TouchableOpacity style={s.addBtn} onPress={handleAdd}>
        <Text style={s.addBtnText}>Add spend</Text>
      </TouchableOpacity>

      <TouchableOpacity style={s.adviceBtn} onPress={handleAdvice} disabled={adviceLoading}>
        <Text style={s.adviceBtnText}>{adviceLoading ? 'Thinking...' : 'Get AI advice'}</Text>
      </TouchableOpacity>
      {advice ? <Text style={s.adviceText}>{advice}</Text> : null}

      <Text style={s.sectionLabel}>Recent</Text>
      {entries.slice(0, 20).map((e) => (
        <View key={e.id} style={[s.entryItem, e.is_impulse && s.entryItemImpulse]}>
          <Text style={s.entryAmount}>₹{Number(e.amount).toFixed(0)} · {e.category}</Text>
          <Text style={s.entryMeta}>
            {e.payment_method.toUpperCase()} · {new Date(e.created_at).toLocaleString()}
            {e.note ? ` · ${e.note}` : ''}
          </Text>
        </View>
      ))}
    </ScrollView>
  );
}

const s = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#14161b' },
  headerRow: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12 },
  title: { fontSize: 20, fontWeight: '700', color: '#f0f1f4' },
  settingsIcon: { fontSize: 20 },
  budgetPanel: { backgroundColor: '#1f222a', borderColor: '#2a2d35', borderWidth: 1, borderRadius: 12, padding: 12, marginBottom: 16 },
  budgetRow: { flexDirection: 'row', justifyContent: 'space-between' },
  budgetLabel: { color: '#f0f1f4', fontSize: 13 },
  budgetValue: { color: '#f0f1f4', fontSize: 13, fontWeight: '600' },
  budgetLabelMuted: { color: '#9a9da6', fontSize: 12 },
  budgetValueMuted: { color: '#9a9da6', fontSize: 12 },
  bar: { height: 6, backgroundColor: '#2a2d35', borderRadius: 3, marginTop: 4, overflow: 'hidden' },
  barFill: { height: 6, borderRadius: 3 },
  sectionLabel: { fontSize: 11, color: '#9a9da6', textTransform: 'uppercase', marginTop: 16, marginBottom: 6 },
  input: {
    backgroundColor: '#1f222a',
    borderColor: '#2a2d35',
    borderWidth: 1,
    borderRadius: 8,
    padding: 10,
    color: '#f0f1f4',
    marginBottom: 8,
  },
  row: { flexDirection: 'row', gap: 8, alignItems: 'center' },
  chipRow: { flexDirection: 'row', flexWrap: 'wrap', gap: 6, marginBottom: 8 },
  chip: { backgroundColor: '#1f222a', borderColor: '#2a2d35', borderWidth: 1, borderRadius: 999, paddingVertical: 5, paddingHorizontal: 12 },
  chipActive: { backgroundColor: '#6ee7b7', borderColor: '#6ee7b7' },
  chipText: { color: '#f0f1f4', fontSize: 12 },
  chipTextActive: { color: '#0a2f22', fontWeight: '600' },
  pill: { backgroundColor: '#1f222a', borderColor: '#2a2d35', borderWidth: 1, borderRadius: 999, paddingVertical: 6, paddingHorizontal: 12 },
  pillActive: { backgroundColor: '#2a2d35', borderColor: '#6ee7b7' },
  pillText: { color: '#f0f1f4', fontSize: 12 },
  addBtn: { backgroundColor: '#6ee7b7', borderRadius: 8, padding: 12, marginTop: 12, alignItems: 'center' },
  addBtnText: { color: '#0a2f22', fontWeight: '700' },
  adviceBtn: { borderColor: '#2a2d35', borderWidth: 1, borderRadius: 8, padding: 10, marginTop: 10, alignItems: 'center' },
  adviceBtnText: { color: '#f0f1f4', fontWeight: '600' },
  adviceText: { color: '#f0f1f4', fontSize: 12, backgroundColor: '#1f222a', borderRadius: 8, padding: 10, marginTop: 8, lineHeight: 18 },
  entryItem: { backgroundColor: '#1f222a', borderColor: '#2a2d35', borderWidth: 1, borderRadius: 8, padding: 8, marginBottom: 6 },
  entryItemImpulse: { borderColor: 'rgba(248,113,113,0.5)' },
  entryAmount: { color: '#f0f1f4', fontWeight: '600', fontSize: 13 },
  entryMeta: { color: '#9a9da6', fontSize: 11, marginTop: 2 },
});
