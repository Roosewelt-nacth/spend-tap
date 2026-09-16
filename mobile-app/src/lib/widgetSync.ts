import { Platform } from 'react-native';
import { SpendEntry } from './types';

// Pushes a fresh summary to the platform's home-screen widget. Both native
// widget modules only exist in a real dev/release build (not Expo Go), so
// every call here is wrapped defensively — it's a no-op if the module isn't
// linked yet (e.g. before you've run `expo prebuild`).

export interface WidgetSummary {
  todaySpend: number;
  dailyBudget: number;
  weeklySpend: number;
  weeklyBudget: number;
  topCategory: string;
}

export function buildSummary(
  entries: SpendEntry[],
  dailyBudget: number,
  weeklyBudget: number
): WidgetSummary {
  const now = new Date();
  const dayStart = new Date(now);
  dayStart.setHours(0, 0, 0, 0);
  const weekStart = new Date(dayStart);
  weekStart.setDate(weekStart.getDate() - weekStart.getDay());

  const todayEntries = entries.filter((e) => new Date(e.created_at) >= dayStart);
  const weekEntries = entries.filter((e) => new Date(e.created_at) >= weekStart);

  const todaySpend = todayEntries.reduce((s, e) => s + Number(e.amount), 0);
  const weeklySpend = weekEntries.reduce((s, e) => s + Number(e.amount), 0);

  const byCategory: Record<string, number> = {};
  for (const e of weekEntries) {
    byCategory[e.category] = (byCategory[e.category] || 0) + Number(e.amount);
  }
  const topCategory =
    Object.entries(byCategory).sort((a, b) => b[1] - a[1])[0]?.[0] || '—';

  return { todaySpend, dailyBudget, weeklySpend, weeklyBudget, topCategory };
}

export async function syncWidget(summary: WidgetSummary): Promise<void> {
  if (Platform.OS === 'android') {
    await syncAndroidWidget(summary);
  } else if (Platform.OS === 'ios') {
    await syncIosWidget(summary);
  }
}

async function syncAndroidWidget(summary: WidgetSummary) {
  try {
    // requires: npm i react-native-android-widget, then `expo prebuild`
    // and registering the widget in app.json (see README-mobile.md)
    const { requestWidgetUpdate } = require('react-native-android-widget');
    const { SpendWidget } = require('../widgets/SpendWidget');
    await requestWidgetUpdate({
      widgetName: 'SpendWidget',
      renderWidget: () => SpendWidget(summary),
      widgetNotFound: () => {
        // No widget pinned to the home screen yet — nothing to update.
      },
    });
  } catch (err) {
    console.log('[widgetSync] android widget update skipped:', (err as Error).message);
  }
}

async function syncIosWidget(summary: WidgetSummary) {
  try {
    // requires: npm i react-native-shared-group-preferences, an App Group
    // configured in Xcode (e.g. group.com.yourorg.spendtracker), and the
    // SpendWidget WidgetKit extension reading the same suite (see ios-widget/).
    const SharedGroupPreferences = require('react-native-shared-group-preferences').default;
    const APP_GROUP = 'group.com.yourorg.spendtracker';
    await SharedGroupPreferences.setItem('spendWidgetSummary', summary, APP_GROUP);
  } catch (err) {
    console.log('[widgetSync] ios widget update skipped:', (err as Error).message);
  }
}
