<script lang="ts">
  import { onMount } from 'svelte';
  import { page } from '$app/state';
  import { api } from '$lib/api';
  import type { PaySlip } from '$lib/types';
  import StatusBadge from '$lib/components/shared/StatusBadge.svelte';
  import { authStore } from '$lib/stores/auth.svelte';
  import { Receipt, ArrowLeft, Check, Loader2 } from '@lucide/svelte';
  import { toast } from 'svelte-sonner';

  const slipId = $derived(page.params.id);
  let slip = $state<PaySlip | null>(null);
  let loading = $state(true);
  let marking = $state(false);

  const canMarkPaid = $derived(authStore.hasRole('DG_SHIPPING_ADMIN', 'COMPANY_ADMIN'));

  onMount(async () => {
    loading = true;
    try { slip = await api.get<PaySlip>(`/api/v1/payroll/slips/${slipId}`); }
    finally { loading = false; }
  });

  async function markPaid() {
    marking = true;
    try {
      slip = await api.post<PaySlip>(`/api/v1/payroll/slips/${slipId}/mark-paid`);
      toast.success('Slip marked as paid');
    } finally { marking = false; }
  }

  const months = ['Jan','Feb','Mar','Apr','May','Jun','Jul','Aug','Sep','Oct','Nov','Dec'];
</script>

<svelte:head><title>Pay Slip — Artemis</title></svelte:head>

{#if loading}
  <div class="flex items-center justify-center py-24"><Loader2 class="size-8 animate-spin text-primary" /></div>
{:else if !slip}
  <p class="text-center text-muted-foreground py-16">Pay slip not found</p>
{:else}
  <div class="max-w-2xl mx-auto space-y-6">
    <div class="flex items-center gap-2 text-sm text-muted-foreground">
      <a href="/payroll" class="hover:text-foreground flex items-center gap-1 transition-colors"><ArrowLeft class="size-3.5" />Payroll</a>
      <span>/</span><span class="text-foreground font-medium">Pay Slip</span>
    </div>

    <!-- Pay slip card — styled like a real payslip -->
    <div class="rounded-2xl border border-border bg-card overflow-hidden shadow-lg print:shadow-none" id="payslip">
      <!-- Header -->
      <div class="bg-gradient-to-r from-primary/20 via-primary/10 to-amber-400/5 border-b border-border p-6">
        <div class="flex items-center justify-between">
          <div>
            <p class="text-xs font-semibold text-primary/80 uppercase tracking-widest">Pay Slip</p>
            <h1 class="text-2xl font-bold text-foreground mt-0.5">
              {months[(slip.payrollRun?.month ?? 1) - 1]} {slip.payrollRun?.year}
            </h1>
            <p class="text-sm text-muted-foreground mt-1">{slip.payrollRun?.company?.name}</p>
          </div>
          <div class="text-right">
            <StatusBadge status={slip.status ?? 'PENDING'} />
            {#if canMarkPaid && slip.status === 'PENDING'}
              <button onclick={markPaid} disabled={marking} class="mt-2 flex items-center gap-2 px-3 py-2 rounded-lg bg-emerald-600 text-white text-sm font-semibold hover:bg-emerald-700 disabled:opacity-60 transition-all">
                {#if marking}<Loader2 class="size-4 animate-spin" />{:else}<Check class="size-4" />{/if}Mark Paid
              </button>
            {/if}
          </div>
        </div>
      </div>

      <div class="p-6 space-y-5">
        <!-- Seafarer info -->
        <div class="grid grid-cols-2 gap-4 pb-5 border-b border-border">
          <div class="space-y-0.5">
            <p class="text-xs text-muted-foreground uppercase tracking-wide font-medium">Employee</p>
            <p class="font-bold text-foreground">{slip.indosMaster?.firstName} {slip.indosMaster?.lastName}</p>
            <p class="font-mono text-xs text-muted-foreground">{slip.indosMaster?.indosNo}</p>
          </div>
          <div class="space-y-0.5">
            <p class="text-xs text-muted-foreground uppercase tracking-wide font-medium">Vessel</p>
            <p class="font-bold text-foreground">{slip.contract?.vessel?.name ?? '—'}</p>
            <p class="text-xs text-muted-foreground">{slip.contract?.rank?.name ?? '—'}</p>
          </div>
          <div class="space-y-0.5">
            <p class="text-xs text-muted-foreground uppercase tracking-wide font-medium">Sign-On Date</p>
            <p class="font-medium text-foreground">{slip.contract?.actualSignOnDate ? new Date(slip.contract.actualSignOnDate).toLocaleDateString() : '—'}</p>
          </div>
          <div class="space-y-0.5">
            <p class="text-xs text-muted-foreground uppercase tracking-wide font-medium">Days at Sea</p>
            <p class="font-bold text-foreground">{slip.daysAtSea ?? '—'}</p>
          </div>
        </div>

        <!-- Earnings breakdown -->
        <div>
          <h2 class="font-semibold text-foreground text-sm mb-3">Earnings</h2>
          <div class="space-y-2">
            {#each [
              ['Basic Wage', slip.basicWageUsd],
              ['Overtime', slip.overtimeUsd],
              ['Allowances', slip.allowancesUsd]
            ] as [label, amount]}
              {#if amount !== undefined && amount !== null}
                <div class="flex items-center justify-between py-2 border-b border-border/50">
                  <span class="text-sm text-muted-foreground">{label}</span>
                  <span class="text-sm font-medium text-foreground">${(amount ?? 0).toLocaleString(undefined, { minimumFractionDigits: 2 })}</span>
                </div>
              {/if}
            {/each}
          </div>
        </div>

        <!-- Deductions -->
        {#if slip.deductionsUsd}
          <div>
            <h2 class="font-semibold text-foreground text-sm mb-3">Deductions</h2>
            <div class="flex items-center justify-between py-2 border-b border-border/50">
              <span class="text-sm text-muted-foreground">Total Deductions</span>
              <span class="text-sm font-medium text-destructive">-${slip.deductionsUsd.toLocaleString(undefined, { minimumFractionDigits: 2 })}</span>
            </div>
          </div>
        {/if}

        <!-- Net Pay -->
        <div class="rounded-xl bg-primary/5 border border-primary/20 p-4 flex items-center justify-between">
          <div>
            <p class="text-xs font-semibold text-primary uppercase tracking-wide">Net Pay</p>
            <p class="text-xs text-muted-foreground mt-0.5">{slip.paymentMethod ?? 'Bank Transfer'}</p>
          </div>
          <p class="text-3xl font-bold text-primary tabular-nums">${(slip.netPayUsd ?? 0).toLocaleString(undefined, { minimumFractionDigits: 2 })}</p>
        </div>

        {#if slip.paidAt}
          <p class="text-xs text-muted-foreground text-center">Paid on {new Date(slip.paidAt).toLocaleString()}</p>
        {/if}
      </div>
    </div>

    <!-- Print button -->
    <div class="text-center print:hidden">
      <button onclick={() => window.print()} class="px-6 py-2.5 rounded-lg border border-border bg-secondary/50 text-sm font-medium hover:bg-secondary transition-all">
        🖨️ Print Pay Slip
      </button>
    </div>
  </div>
{/if}
