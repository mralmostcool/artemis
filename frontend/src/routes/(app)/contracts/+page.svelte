<script lang="ts">
  import { onMount } from 'svelte';
  import { api } from '$lib/api';
  import type { Contract, IndosMaster, Vessel } from '$lib/types';
  import { authStore } from '$lib/stores/auth.svelte';
  import StatusBadge from '$lib/components/shared/StatusBadge.svelte';
  import { FileText, Plus, Search, ArrowRight, Loader2, X } from '@lucide/svelte';
  import { toast } from 'svelte-sonner';

  let contracts = $state<Contract[]>([]);
  let loading = $state(true);
  let search = $state('');
  let showAdd = $state(false);
  let form = $state<Partial<Contract & { indosId: string; vesselId: string; rankId: string; plannedSignOnDate: string; plannedSignOffDate: string; wageUsd: number }>>({});
  let saving = $state(false);

  const isAdmin = $derived(authStore.hasRole('DG_SHIPPING_ADMIN'));
  const isCompanyAdmin = $derived(authStore.hasRole('DG_SHIPPING_ADMIN', 'COMPANY_ADMIN'));

  const filtered = $derived(
    !search ? contracts : contracts.filter((c) =>
      c.indosMaster?.firstName?.toLowerCase().includes(search.toLowerCase()) ||
      c.indosMaster?.lastName?.toLowerCase().includes(search.toLowerCase()) ||
      c.vessel?.name?.toLowerCase().includes(search.toLowerCase())
    )
  );

  onMount(async () => {
    loading = true;
    try {
      const orgId = authStore.profile?.organizationId;
      if (isAdmin) {
        contracts = await api.get<Contract[]>('/api/v1/contracts');
      } else if (orgId && isCompanyAdmin) {
        contracts = await api.get<Contract[]>(`/api/v1/companies/${orgId}/contracts`);
      }
    } finally { loading = false; }
  });

  async function createContract() {
    saving = true;
    try {
      const payload = {
        indosMaster: { id: form.indosId },
        vessel: { id: form.vesselId },
        rank: { id: form.rankId },
        plannedSignOnDate: form.plannedSignOnDate,
        plannedSignOffDate: form.plannedSignOffDate,
        wageUsd: form.wageUsd
      };
      const created = await api.post<Contract>('/api/v1/contracts', payload);
      contracts = [created, ...contracts];
      showAdd = false; form = {};
      toast.success('Contract drafted');
    } finally { saving = false; }
  }
</script>

<svelte:head><title>Contracts — Artemis</title></svelte:head>

<div class="space-y-6 max-w-7xl mx-auto">
  <div class="flex items-center justify-between flex-wrap gap-3">
    <div>
      <h1 class="text-2xl font-bold text-foreground">Contracts</h1>
      <p class="text-sm text-muted-foreground">Seafarer employment contracts</p>
    </div>
    {#if isCompanyAdmin}
      <button onclick={() => (showAdd = true)} class="flex items-center gap-2 px-4 py-2 rounded-lg bg-primary text-primary-foreground text-sm font-semibold hover:bg-primary/90 transition-all shadow-sm">
        <Plus class="size-4" />Draft Contract
      </button>
    {/if}
  </div>

  <div class="relative max-w-sm">
    <Search class="absolute left-3 top-1/2 -translate-y-1/2 size-4 text-muted-foreground pointer-events-none" />
    <input type="text" bind:value={search} placeholder="Search by seafarer or vessel…" class="w-full pl-10 pr-4 py-2.5 rounded-lg border border-input bg-background text-sm outline-none focus:ring-2 focus:ring-ring transition-all" />
  </div>

  <div class="rounded-xl border border-border bg-card overflow-hidden">
    <table class="w-full text-sm">
      <thead><tr class="border-b border-border bg-muted/40">
        {#each ['Seafarer','Vessel','Rank','Sign On','Sign Off','Status',''] as h}
          <th class="text-left px-4 py-3 text-xs font-semibold text-muted-foreground uppercase tracking-wide">{h}</th>
        {/each}
      </tr></thead>
      <tbody>
        {#if loading}
          {#each Array(6) as _}<tr class="border-b border-border">{#each Array(7) as _}<td class="px-4 py-3"><div class="h-4 rounded bg-muted animate-pulse"></div></td>{/each}</tr>{/each}
        {:else if filtered.length === 0}
          <tr><td colspan="7" class="px-4 py-12 text-center">
            <FileText class="size-10 text-muted-foreground/30 mx-auto mb-2" />
            <p class="text-muted-foreground">{contracts.length === 0 ? 'No contracts yet' : 'No matching contracts'}</p>
          </td></tr>
        {:else}
          {#each filtered as c}
            <tr class="border-b border-border last:border-0 hover:bg-accent/20 transition-colors">
              <td class="px-4 py-3 font-medium text-foreground">{c.indosMaster?.firstName} {c.indosMaster?.lastName}</td>
              <td class="px-4 py-3 text-muted-foreground">{c.vessel?.name ?? '—'}</td>
              <td class="px-4 py-3 text-muted-foreground">{c.rank?.name ?? '—'}</td>
              <td class="px-4 py-3 text-muted-foreground text-xs">{c.actualSignOnDate ? new Date(c.actualSignOnDate).toLocaleDateString() : c.plannedSignOnDate ? new Date(c.plannedSignOnDate).toLocaleDateString() : '—'}</td>
              <td class="px-4 py-3 text-muted-foreground text-xs">{c.actualSignOffDate ? new Date(c.actualSignOffDate).toLocaleDateString() : c.plannedSignOffDate ? new Date(c.plannedSignOffDate).toLocaleDateString() : '—'}</td>
              <td class="px-4 py-3"><StatusBadge status={c.status ?? 'DRAFT'} /></td>
              <td class="px-4 py-3 text-right"><a href="/contracts/{c.id}" class="px-3 py-1.5 rounded-lg bg-primary/10 text-primary text-xs font-semibold hover:bg-primary/20 transition-colors">View</a></td>
            </tr>
          {/each}
        {/if}
      </tbody>
    </table>
  </div>
</div>

{#if showAdd}
  <div class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-sm">
    <div class="bg-card rounded-2xl border border-border shadow-2xl w-full max-w-md p-6 space-y-4 max-h-[90vh] overflow-y-auto">
      <div class="flex items-center justify-between sticky top-0 bg-card pb-2"><h2 class="text-lg font-bold">Draft Contract</h2><button onclick={() => (showAdd = false)}><X class="size-4" /></button></div>
      <div class="space-y-3">
        {#each [['indosId','INDoS ID (UUID) *','text'],['vesselId','Vessel ID (UUID) *','text'],['rankId','Rank ID (UUID)','text'],['plannedSignOnDate','Planned Sign-On','datetime-local'],['plannedSignOffDate','Planned Sign-Off','datetime-local'],['wageUsd','Wage (USD/month)','number']] as [f, l, t]}
          <div class="space-y-1.5">
            <label class="text-sm font-medium text-foreground">{l}</label>
            <input type={t} bind:value={(form as Record<string, string | number>)[f]} class="w-full px-3 py-2 rounded-lg border border-input bg-background text-sm outline-none focus:ring-2 focus:ring-ring transition-all font-mono text-xs" />
          </div>
        {/each}
      </div>
      <div class="flex gap-3 sticky bottom-0 bg-card pt-2">
        <button onclick={() => (showAdd = false)} class="flex-1 py-2.5 rounded-lg border border-border text-sm font-medium hover:bg-accent transition-all">Cancel</button>
        <button onclick={createContract} disabled={saving} class="flex-1 flex items-center justify-center gap-2 py-2.5 rounded-lg bg-primary text-primary-foreground text-sm font-semibold hover:bg-primary/90 disabled:opacity-60 transition-all">
          {#if saving}<Loader2 class="size-4 animate-spin" />Saving…{:else}Draft Contract{/if}
        </button>
      </div>
    </div>
  </div>
{/if}
