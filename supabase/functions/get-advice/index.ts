// Supabase Edge Function: proxies the Claude advice call so the Anthropic
// API key never ships inside the mobile app bundle. Deploy with:
//   supabase functions deploy get-advice
//   supabase secrets set ANTHROPIC_API_KEY=sk-ant-...
//
// Call it from the app with the user's Supabase anon/session JWT in the
// Authorization header (the Supabase client does this automatically).

// deno-lint-ignore-file no-explicit-any
import { serve } from 'https://deno.land/std@0.203.0/http/server.ts';

const corsHeaders = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Headers': 'authorization, x-client-info, apikey, content-type',
};

serve(async (req) => {
  if (req.method === 'OPTIONS') {
    return new Response('ok', { headers: corsHeaders });
  }

  try {
    const { entries, categories, dailyBudget, weeklyBudget } = await req.json();

    const apiKey = Deno.env.get('ANTHROPIC_API_KEY');
    if (!apiKey) {
      return new Response(JSON.stringify({ error: 'ANTHROPIC_API_KEY not configured on the server.' }), {
        status: 500,
        headers: { ...corsHeaders, 'Content-Type': 'application/json' },
      });
    }

    const summaryLines = (entries || [])
      .slice(0, 40)
      .map(
        (e: any) =>
          `- ₹${e.amount} | ${e.category} | ${e.payment_method} | impulse:${e.is_impulse} | "${e.note || ''}" | ${e.created_at}`
      )
      .join('\n');

    const prompt = `You are a calm, non-judgmental personal finance coach. Here are a user's recent spend entries (currency INR), their categories, and budgets.

Categories: ${JSON.stringify(categories)}
Daily budget: ${dailyBudget}
Weekly budget: ${weeklyBudget}

Recent entries (most recent first):
${summaryLines || '(no entries yet)'}

In under 150 words: point out concrete patterns (which category dominates, any impulsive spend clusters, notable notes), and give 2-3 specific, practical suggestions to spend more intentionally. Be direct but encouraging, not preachy. No generic advice — ground it in their actual entries.`;

    const res = await fetch('https://api.anthropic.com/v1/messages', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'x-api-key': apiKey,
        'anthropic-version': '2023-06-01',
      },
      body: JSON.stringify({
        model: 'claude-sonnet-5',
        max_tokens: 400,
        messages: [{ role: 'user', content: prompt }],
      }),
    });

    if (!res.ok) {
      const text = await res.text();
      return new Response(JSON.stringify({ error: `Anthropic API error (${res.status}): ${text}` }), {
        status: 502,
        headers: { ...corsHeaders, 'Content-Type': 'application/json' },
      });
    }

    const json = await res.json();
    const advice = (json.content || []).map((c: any) => c.text).join('').trim();

    return new Response(JSON.stringify({ advice }), {
      headers: { ...corsHeaders, 'Content-Type': 'application/json' },
    });
  } catch (err) {
    return new Response(JSON.stringify({ error: String(err) }), {
      status: 400,
      headers: { ...corsHeaders, 'Content-Type': 'application/json' },
    });
  }
});
