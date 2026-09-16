import React from 'react';
import { FlexWidget, TextWidget } from 'react-native-android-widget';
import { WidgetSummary } from '../lib/widgetSync';

// Rendered natively on the Android home screen by react-native-android-widget
// (it translates this tree to RemoteViews/Jetpack Glance under the hood).
// Kept deliberately simple: widgets can't run arbitrary JS, only this
// constrained layout API.
export function SpendWidget(summary: WidgetSummary) {
  const pct = summary.dailyBudget > 0 ? Math.min(1, summary.todaySpend / summary.dailyBudget) : 0;
  const barColor = pct >= 1 ? '#f87171' : pct >= 0.7 ? '#fbbf24' : '#6ee7b7';

  return (
    <FlexWidget
      style={{
        height: 'match_parent',
        width: 'match_parent',
        backgroundColor: '#181a20',
        borderRadius: 16,
        padding: 12,
        flexDirection: 'column',
        justifyContent: 'space-between',
      }}
    >
      <TextWidget
        text="Today's spend"
        style={{ fontSize: 11, color: '#9a9da6' }}
      />
      <TextWidget
        text={`₹${summary.todaySpend.toFixed(0)} / ₹${summary.dailyBudget}`}
        style={{ fontSize: 18, color: '#f0f1f4', fontWeight: 'bold' }}
      />
      <FlexWidget
        style={{
          height: 6,
          width: 'match_parent',
          backgroundColor: '#2a2d35',
          borderRadius: 3,
        }}
      >
        <FlexWidget
          style={{
            height: 6,
            width: `${Math.max(4, pct * 100)}%` as any,
            backgroundColor: barColor,
            borderRadius: 3,
          }}
        />
      </FlexWidget>
      <TextWidget
        text={`Top this week: ${summary.topCategory}`}
        style={{ fontSize: 10, color: '#9a9da6' }}
      />
    </FlexWidget>
  );
}
