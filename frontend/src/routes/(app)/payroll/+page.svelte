<script lang="ts">
  import { onMount } from 'svelte';
  import { api } from '$lib/api';
  import type { PayrollRun, PaySlip } from '$lib/types';
  import { authStore } from '$lib/stores/auth.svelte';
  import StatusBadge from '$lib/components/shared/StatusBadge.svelte';
  import { Banknote, Plus, Play, ArrowRight, Loader2, X, Receipt } from '@lucide/svelte';
  import { toast } from 'svelte-sonner';

  let runs = $state<PayrollRun[]>([]);
  let mySlips = $state<PaySlip[]>([]);
  let loading = $state(true);
  let showNewRun = $state(false);
  let runForm = $state<{ companyId: string; month: number; year: number }>({ companyId: '', month: new Date().getMonth() + 1, year: new Date().getFullYear() });
  let saving = $state(false);
  let processingId = $state<string | null>(null);

  const isAdmin = $derived(authStore.hasRole('DG_SHIPPING_ADMIN'));
  const isCompanyAdmin = $derived(authStore.hasRole('DG_SHIPPING_ADMIN', 'COMPANY_ADMIN'));
  const isCandidate = $derived(authStore.profile?.role === 'CANDIDATE');

  onMount(async () => {
    loading = true;
    try {
      if (isCandidate) {
        const indosLink = await api.get<{ indosMaster: { id: string } }>('/api/v1/seafarers/link').catch(() => null);
        if (indosLink?.indosMaster?.id) {
          mySlips = await api.get<PaySlip[]>(`/api/v1/payroll/seafarers/${indosLink.indosMaster.id}/slips`);
        }
      } else {
        const orgId = authStore.profile?.organizationId;
        if (isAdmin) {
          runs = await api.get<PayrollRun[]>('/api/v1/payroll/runs');
        } else if (orgId) {
          runs = await api.get<PayrollRun[]>(`/api/v1/payroll/companies/${orgId}/runs`);
        }
      }
    } finally { loading = false; }
  });

  async function createRun() {
    saving = true;
    try {
      const created = await api.post<PayrollRun>('/api/v1/payroll/runs', {
        company: { id: runForm.companyId },
        month: runForm.month,
        year: runForm.year
      });
      runs = [created, ...runs];
      showNewRun = false;
      toast.success('Payroll run created');
    } finally { saving = false; }
  }

  async function processRun(run: PayrollRun) {
    processingId = run.id;
    try {
      const updated = await api.post<PayrollRun>(`/api/v1/payroll/runs/${run.id}/process`);
      runs = runs.map((r) => r.id === run.id ? updated : r);
      toast.success('Payroll processed');
    } finally { processingId = null; }
  }

  const months = ['Jan','Feb','Mar','Apr','May','Jun','Jul','Aug','Sep','Oct','Nov','Dec'];
</script>

<svelte:head><title>Payroll — Artemis</title></svelte:head>

<div class="space-y-6 max-w-7xl mx-auto">
  <div class="flex items-center justify-between flex-wrap gap-3">
    <div>
      <h1 class="text-2xl font-bold text-foreground">Payroll</h1>
      <p class="text-sm text-muted-foreground">{isCandidate ? 'Your earnings and pay slip history' : 'Monthly seafarer payroll runs and slips'}</p>
    </div>
    {#if isCompanyAdmin && !isCandidate}
      <button onclick={() => (showNewRun = true)} class="flex items-center gap-2 px-4 py-2 rounded-lg bg-primary text-primary-foreground text-sm font-semibold hover:bg-primary/90 transition-all shadow-sm">
        <Plus class="size-4" />New Payroll Run
      </button>
    {/if}
  </div>

  {#if loading}
    <div class="space-y-3">
      {#each Array(4) as _}<div class="rounded-xl border border-border bg-card p-5 animate-pulse h-20"></div>{/each}
    </div>
  {:else if isCandidate}
    <!-- Candidate: my pay slips -->
    {#if mySlips.length === 0}
      <div class="rounded-2xl border border-border bg-card p-16 text-center">
        <Receipt class="size-14 text-muted-foreground/30 mx-auto mb-4" />
        <h2 class="font-bold text-foreground text-lg mb-2">No Pay Slips Yet</h2>
        <p class="text-muted-foreground text-sm">Pay slips will appear here after your company runs monthly payroll.</p>
      </div>
    {:else}
      <div class="space-y-3">
        {#each mySlips as slip}
          <a href="/payroll/slips/{slip.id}" class="group rounded-xl border border-border bg-card p-5 flex items-center gap-4 hover:shadow-md hover:-translate-y-0.5 transition-all block">
            <div class="size-12 rounded-xl bg-emerald-500/10 flex items-center justify-center shrink-0">
              <Receipt class="size-6 text-emerald-500" />
            </div>
            <div class="flex-1 min-w-0">
              <p class="font-bold text-foreground">{months[(slip.payrollRun?.month ?? 1) - 1]} {slip.payrollRun?.year}</p>
              <p class="text-sm text-muted-foreground">{slip.payrollRun?.company?.name}</p>
            </div>
            <div class="text-right shrink-0">
              <p class="text-xl font-bold text-foreground tabular-nums">${slip.netPayUsd?.toLocaleString() ?? '—'}</p>
              <StatusBadge status={slip.status ?? 'PENDING'} />
            </div>
            <ArrowRight class="size-4 text-muted-foreground/40 group-hover:text-primary group-hover:translate-x-0.5 transition-all" />
          </a>
        {/each}
      </div>
    {/if}
  {:else}
    <!-- Admin/Company: payroll runs -->
    {#if runs.length === 0}
      <div class="rounded-2xl border border-border bg-card p-16 text-center">
        <Banknote class="size-14 text-muted-foreground/30 mx-auto mb-4" />
        <h2 class="font-bold text-foreground text-lg mb-2">No Payroll Runs</h2>
        <p class="text-muted-foreground text-sm">Create a new payroll run to generate pay slips for the month.</p>
      </div>
    {:else}
      <div class="space-y-3">
        {#each runs as run}
          <div class="rounded-xl border border-border bg-card p-5 flex items-center gap-4 hover:bg-accent/10 transition-colors">
            <div class="size-12 rounded-xl bg-amber-500/10 flex items-center justify-center shrink-0">
              <Banknote class="size-6 text-amber-500" />
            </div>
            <div class="flex-1 min-w-0">
              <p class="font-bold text-foreground">{run.company?.name} — {months[(run.month ?? 1) - 1]} {run.year}</p>
              <div class="flex items-center gap-3 mt-0.5 text-xs text-muted-foreground">
                <span>{run.slipCount ?? 0} slips</span>
                {#if run.totalPayableUsd}<span>Total: ${run.totalPayableUsd.toLocaleString()}</span>{/if}
              </div>
            </div>
            <div class="flex items-center gap-3 shrink-0">
              <StatusBadge status={run.status ?? 'PENDING'} />
              {#if isCompanyAdmin && run.status === 'PENDING'}
                <button
                  onclick={() => processRun(run)}
                  disabled={processingId === run.id}
                  class="flex items-center gap-2 px-3 py-2 rounded-lg bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 border border-emerald-500/20 text-sm font-medium hover:bg-emerald-500/20 transition-all disabled:opacity-50"
                >
                  {#if processingId === run.id}<Loader2 class="size-4 animate-spin" />{:else}<Play class="size-4" />{/if}
                  Process
                </button>
              {/if}
              <a href="/payroll/slips?runId={run.id}" class="px-3 py-2 rounded-lg bg-primary/10 text-primary text-sm font-semibold hover:bg-primary/20 transition-colors flex items-center gap-1.5">
                View Slips <ArrowRight class="size-3.5" />
              </a>
            </div>
          </div>
        {/each}
      </div>
    {/if}
  {/if}
</div>

{#if showNewRun}
  <div class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-sm">
    <div class="bg-card rounded-2xl border border-border shadow-2xl w-full max-w-sm p-6 space-y-4">
      <div class="flex items-center justify-between"><h2 class="text-lg font-bold">New Payroll Run</h2><button onclick={() => (showNewRun = false)}><X class="size-4" /></button></div>
      <div class="space-y-3">
        <div class="space-y-1.5">
          <label class="text-sm font-medium text-foreground">Company ID (UUID) *</label>
          <input type="text" bind:value={runForm.companyId} placeholder="Company UUID" class="w-full px-3 py-2 rounded-lg border border-input bg-background text-sm outline-none focus:ring-2 focus:ring-ring transition-all font-mono text-xs" />
        </div>
        <div class="grid grid-cols-2 gap-3">
          <div class="space-y-1.5">
            <label class="text-sm font-medium text-foreground">Month</label>
            <select bind:value={runForm.month} class="w-full px-3 py-2 rounded-lg border border-input bg-background text-sm outline-none focus:ring-2 focus:ring-ring transition-all">
              {#each months as m, i}<option value={i + 1}>{m}</option>{/each}
            </select>
          </div>
          <div class="space-y-1.5">
            <label class="text-sm font-medium text-foreground">Year</label>
            <input type="number" bind:value={runForm.year} min="2020" max="2099" class="w-full px-3 py-2 rounded-lg border border-input bg-background text-sm outline-none focus:ring-2 focus:ring-ring transition-all" />
          </div>
        </div>
      </div>
      <div class="flex gap-3">
        <button onclick={() => (showNewRun = false)} class="flex-1 py-2.5 rounded-lg border border-border text-sm font-medium hover:bg-accent transition-all">Cancel</button>
        <button onclick={createRun} disabled={saving || !runForm.companyId} class="flex-1 flex items-center justify-center gap-2 py-2.5 rounded-lg bg-primary text-primary-foreground text-sm font-semibold hover:bg-primary/90 disabled:opacity-60 transition-all">
          {#if saving}<Loader2 class="size-4 animate-spin" />Creating…{:else}Create Run{/if}
        </button>
      </div>
    </div>
  </div>
{/if}
