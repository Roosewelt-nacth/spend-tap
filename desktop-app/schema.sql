-- Run this in the Supabase SQL editor (Project -> SQL Editor -> New query)

create table if not exists public.settings (
  device_id text primary key,
  categories jsonb not null default '[]'::jsonb,
  daily_budget numeric not null default 0,
  weekly_budget numeric not null default 0,
  updated_at timestamptz not null default now()
);

create table if not exists public.entries (
  id uuid primary key default gen_random_uuid(),
  device_id text not null,
  amount numeric not null check (amount > 0),
  category text not null,
  note text default '',
  payment_method text not null check (payment_method in ('upi', 'cash')),
  is_impulse boolean not null default false,
  created_at timestamptz not null default now()
);

create index if not exists entries_device_created_idx
  on public.entries (device_id, created_at desc);

-- This app uses a locally generated device_id instead of full user auth,
-- since it's a single-user personal tracker in testing. Row Level Security
-- is left open to the anon key scoped by device_id in every query below.
-- Before sharing this app with others or across untrusted devices, replace
-- this with Supabase Auth (email/magic link) and RLS policies keyed on auth.uid().
alter table public.settings enable row level security;
alter table public.entries enable row level security;

create policy "anon full access settings" on public.settings
  for all using (true) with check (true);

create policy "anon full access entries" on public.entries
  for all using (true) with check (true);
