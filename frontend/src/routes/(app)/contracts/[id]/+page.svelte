<script lang="ts">
  import { onMount } from 'svelte';
  import { page } from '$app/state';
  import { api } from '$lib/api';
  import type { Contract } from '$lib/types';
  import { authStore } from '$lib/stores/auth.svelte';
  import StatusBadge from '$lib/components/shared/StatusBadge.svelte';
  import { ArrowLeft, Loader2, LogIn, LogOut, CalendarClock, Award } from '@lucide/svelte';
  import { toast } from 'svelte-sonner';

  const contractId = $derived(page.params.id);
  let contract = $state<Contract | null>(null);
  let loading = $state(true);
  let acting = $state(false);

  let showSignOn = $state(false);
  let showSignOff = $state(false);
  let showExtend = $state(false);

  let signOnForm = $state({ actualSignOnDate: '', port: '', country: '' });
  let signOffForm = $state({ actualSignOffDate: '', port: '', country: '', remarks: '' });
  let extendDate = $state('');

  const canSignOnOff = $derived(authStore.hasRole('DG_SHIPPING_ADMIN', 'COMPANY_ADMIN', 'COMPANY_USER'));
  const canExtend = $derived(authStore.hasRole('DG_SHIPPING_ADMIN', 'INSTITUTE_ADMIN'));
  const canEnqueue = $derived(authStore.hasRole('DG_SHIPPING_ADMIN', 'COMPANY_ADMIN'));

  onMount(async () => {
    loading = true;
    try { contract = await api.get<Contract>(`/api/v1/contracts/${contractId}`); }
    finally { loading = false; }
  });

  async function doSignOn() {
    acting = true;
    try {
      contract = await api.post<Contract>(`/api/v1/contracts/${contractId}/sign-on`, undefined, {
        actualSignOnDate: new Date(signOnForm.actualSignOnDate).toISOString(),
        port: signOnForm.port, country: signOnForm.country
      });
      showSignOn = false; toast.success('Signed on successfully');
    } finally { acting = false; }
  }

  async function doSignOff() {
    acting = true;
    try {
      contract = await api.post<Contract>(`/api/v1/contracts/${contractId}/sign-off`, undefined, {
        actualSignOffDate: new Date(signOffForm.actualSignOffDate).toISOString(),
        port: signOffForm.port, country: signOffForm.country,
        ...(signOffForm.remarks ? { remarks: signOffForm.remarks } : {})
      });
      showSignOff = false; toast.success('Signed off successfully');
    } finally { acting = false; }
  }

  async function doExtend() {
    acting = true;
    try {
      contract = await api.put<Contract>(`/api/v1/contracts/${contractId}/extension`, undefined, {
        extendedSignOffDate: new Date(extendDate).toISOString()
      });
      showExtend = false; toast.success('Contract extended');
    } finally { acting = false; }
  }

  async function enqueueForCertificate() {
    acting = true;
    try {
      await api.post('/api/v1/certificates/enqueue', undefined, { contractId });
      toast.success('Certificate enqueued for review');
    } finally { acting = false; }
  }

  function fmt(d?: string) {
    return d ? new Date(d).toLocaleString() : '—';
  }
</script>

<svelte:head><title>Contract — Artemis</title></svelte:head>

{#if loading}
  <div class="flex items-center justify-center py-24"><Loader2 class="size-8 animate-spin text-primary" /></div>
{:else if !contract}
  <p class="text-center text-muted-foreground py-16">Contract not found</p>
{:else}
  <div class="space-y-6 max-w-4xl mx-auto">
    <div class="flex items-center gap-2 text-sm text-muted-foreground">
      <a href="/contracts" class="hover:text-foreground flex items-center gap-1 transition-colors"><ArrowLeft class="size-3.5" />Contracts</a>
      <span>/</span><span class="text-foreground font-medium">Contract Details</span>
    </div>

    <!-- Header card -->
    <div class="rounded-2xl border border-border bg-card overflow-hidden">
      <div class="h-2 bg-gradient-to-r from-violet-500/50 to-blue-400/30"></div>
      <div class="p-6">
        <div class="flex items-start justify-between flex-wrap gap-4 mb-5">
          <div>
            <h1 class="text-2xl font-bold text-foreground">
              {contract.indosMaster?.firstName} {contract.indosMaster?.lastName}
            </h1>
            <p class="text-muted-foreground mt-0.5">{contract.rank?.name ?? '—'} · {contract.vessel?.name ?? '—'}</p>
            <div class="mt-2"><StatusBadge status={contract.status ?? 'DRAFT'} /></div>
          </div>
          <!-- Action buttons -->
          <div class="flex flex-wrap gap-2">
            {#if canSignOnOff && !contract.actualSignOnDate}
              <button onclick={() => (showSignOn = true)} class="flex items-center gap-2 px-3 py-2 rounded-lg bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 border border-emerald-500/20 text-sm font-medium hover:bg-emerald-500/20 transition-all">
                <LogIn class="size-4" />Sign On
              </button>
            {/if}
            {#if canSignOnOff && contract.actualSignOnDate && !contract.actualSignOffDate}
              <button onclick={() => (showSignOff = true)} class="flex items-center gap-2 px-3 py-2 rounded-lg bg-amber-500/10 text-amber-600 dark:text-amber-400 border border-amber-500/20 text-sm font-medium hover:bg-amber-500/20 transition-all">
                <LogOut class="size-4" />Sign Off
              </button>
            {/if}
            {#if canExtend}
              <button onclick={() => (showExtend = true)} class="flex items-center gap-2 px-3 py-2 rounded-lg border border-border bg-secondary/50 text-sm font-medium hover:bg-secondary transition-all">
                <CalendarClock class="size-4" />Extend
              </button>
            {/if}
            {#if canEnqueue && contract.actualSignOffDate}
              <button onclick={enqueueForCertificate} disabled={acting} class="flex items-center gap-2 px-3 py-2 rounded-lg bg-primary/10 text-primary border border-primary/20 text-sm font-medium hover:bg-primary/20 transition-all disabled:opacity-50">
                <Award class="size-4" />Enqueue Certificate
              </button>
            {/if}
          </div>
        </div>

        <!-- Details grid -->
        <div class="grid grid-cols-2 sm:grid-cols-3 gap-5 border-t border-border pt-5">
          {#each [
            ['Vessel', contract.vessel?.name],
            ['Rank', contract.rank?.name],
            ['Wage (USD/mo)', contract.wageUsd ? `$${contract.wageUsd.toLocaleString()}` : null],
            ['Planned Sign-On', fmt(contract.plannedSignOnDate)],
            ['Planned Sign-Off', fmt(contract.plannedSignOffDate)],
            ['Extended Sign-Off', fmt(contract.extendedSignOffDate)],
            ['Actual Sign-On', fmt(contract.actualSignOnDate)],
            ['Sign-On Port', contract.actualSignOnPort && contract.actualSignOnCountry ? `${contract.actualSignOnPort}, ${contract.actualSignOnCountry}` : null],
            ['Actual Sign-Off', fmt(contract.actualSignOffDate)],
            ['Sign-Off Port', contract.actualSignOffPort && contract.actualSignOffCountry ? `${contract.actualSignOffPort}, ${contract.actualSignOffCountry}` : null],
            ['Remarks', contract.signOffRemarks]
          ] as [l, v]}
            {#if v}
              <div class="space-y-0.5">
                <p class="text-xs text-muted-foreground uppercase tracking-wide font-medium">{l}</p>
                <p class="text-sm font-semibold text-foreground">{v}</p>
              </div>
            {/if}
          {/each}
        </div>
      </div>
    </div>
  </div>

  <!-- Sign-On Modal -->
  {#if showSignOn}
    <div class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-sm">
      <div class="bg-card rounded-2xl border border-border shadow-2xl w-full max-w-sm p-6 space-y-4">
        <h2 class="text-lg font-bold">Sign On</h2>
        <div class="space-y-3">
          {#each [['actualSignOnDate','Date & Time *','datetime-local'],['port','Port *','text'],['country','Country *','text']] as [f, l, t]}
            <div class="space-y-1.5">
              <label class="text-sm font-medium text-foreground">{l}</label>
              <input type={t} bind:value={(signOnForm as Record<string, string>)[f]} class="w-full px-3 py-2 rounded-lg border border-input bg-background text-sm outline-none focus:ring-2 focus:ring-ring transition-all" />
            </div>
          {/each}
        </div>
        <div class="flex gap-3">
          <button onclick={() => (showSignOn = false)} class="flex-1 py-2.5 rounded-lg border border-border text-sm font-medium hover:bg-accent transition-all">Cancel</button>
          <button onclick={doSignOn} disabled={acting} class="flex-1 flex items-center justify-center gap-2 py-2.5 rounded-lg bg-emerald-600 text-white text-sm font-semibold hover:bg-emerald-700 disabled:opacity-60 transition-all">
            {#if acting}<Loader2 class="size-4 animate-spin" />{/if}Sign On
          </button>
        </div>
      </div>
    </div>
  {/if}

  <!-- Sign-Off Modal -->
  {#if showSignOff}
    <div class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-sm">
      <div class="bg-card rounded-2xl border border-border shadow-2xl w-full max-w-sm p-6 space-y-4">
        <h2 class="text-lg font-bold">Sign Off</h2>
        <div class="space-y-3">
          {#each [['actualSignOffDate','Date & Time *','datetime-local'],['port','Port *','text'],['country','Country *','text'],['remarks','Remarks','text']] as [f, l, t]}
            <div class="space-y-1.5">
              <label class="text-sm font-medium text-foreground">{l}</label>
              <input type={t} bind:value={(signOffForm as Record<string, string>)[f]} class="w-full px-3 py-2 rounded-lg border border-input bg-background text-sm outline-none focus:ring-2 focus:ring-ring transition-all" />
            </div>
          {/each}
        </div>
        <div class="flex gap-3">
          <button onclick={() => (showSignOff = false)} class="flex-1 py-2.5 rounded-lg border border-border text-sm font-medium hover:bg-accent transition-all">Cancel</button>
          <button onclick={doSignOff} disabled={acting} class="flex-1 flex items-center justify-center gap-2 py-2.5 rounded-lg bg-amber-500 text-white text-sm font-semibold hover:bg-amber-600 disabled:opacity-60 transition-all">
            {#if acting}<Loader2 class="size-4 animate-spin" />{/if}Sign Off
          </button>
        </div>
      </div>
    </div>
  {/if}

  <!-- Extend Modal -->
  {#if showExtend}
    <div class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-sm">
      <div class="bg-card rounded-2xl border border-border shadow-2xl w-full max-w-sm p-6 space-y-4">
        <h2 class="text-lg font-bold">Extend Contract</h2>
        <div class="space-y-1.5">
          <label class="text-sm font-medium text-foreground">New Sign-Off Date & Time *</label>
          <input type="datetime-local" bind:value={extendDate} class="w-full px-3 py-2 rounded-lg border border-input bg-background text-sm outline-none focus:ring-2 focus:ring-ring transition-all" />
        </div>
        <div class="flex gap-3">
          <button onclick={() => (showExtend = false)} class="flex-1 py-2.5 rounded-lg border border-border text-sm font-medium hover:bg-accent transition-all">Cancel</button>
          <button onclick={doExtend} disabled={acting || !extendDate} class="flex-1 flex items-center justify-center gap-2 py-2.5 rounded-lg bg-primary text-primary-foreground text-sm font-semibold hover:bg-primary/90 disabled:opacity-60 transition-all">
            {#if acting}<Loader2 class="size-4 animate-spin" />{/if}Extend
          </button>
        </div>
      </div>
    </div>
  {/if}
{/if}
