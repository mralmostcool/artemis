<script lang="ts">
  import { onMount } from 'svelte';
  import { page } from '$app/state';
  import { api } from '$lib/api';
  import type { Vessel, BerthSeafarerAllocation } from '$lib/types';
  import { authStore } from '$lib/stores/auth.svelte';
  import { Ship, Users, ArrowLeft, Loader2, Plus, X } from '@lucide/svelte';
  import { toast } from 'svelte-sonner';

  const vesselId = $derived(page.params.id);
  let vessel = $state<Vessel | null>(null);
  let crewList = $state<Record<string, unknown>[]>([]);
  let loading = $state(true);
  let showBerthReq = $state(false);
  let berthReqForm = $state<{ requestedSlots: number }>({ requestedSlots: 1 });
  let saving = $state(false);
  let showAllocate = $state(false);
  let allocForm = $state<{ berthId: string; indosId: string; berthAllocationId: string; startDate: string; endDate: string }>({ berthId: '', indosId: '', berthAllocationId: '', startDate: '', endDate: '' });

  const canManage = $derived(authStore.hasRole('DG_SHIPPING_ADMIN', 'COMPANY_ADMIN'));
  const canAllocate = $derived(authStore.hasRole('DG_SHIPPING_ADMIN', 'COMPANY_ADMIN', 'COMPANY_USER'));

  onMount(async () => {
    loading = true;
    try {
      vessel = await api.get<Vessel>(`/api/v1/vessels/${vesselId}`);
      if (canManage) {
        const rawCrew = await api.get<string>(`/api/v1/vessels/${vesselId}/crew-list`).catch(() => '[]');
        try { crewList = JSON.parse(rawCrew as string); } catch { crewList = []; }
      }
    } finally { loading = false; }
  });

  async function submitBerthRequest() {
    saving = true;
    try {
      await api.post(`/api/v1/vessels/${vesselId}/training-berth-requests`, berthReqForm);
      showBerthReq = false; berthReqForm = { requestedSlots: 1 };
      toast.success('Training berth request submitted');
    } finally { saving = false; }
  }

  async function submitAllocation() {
    saving = true;
    try {
      await api.post('/api/v1/vessels/seafarer-allocations', undefined, {
        berthId: allocForm.berthId,
        indosId: allocForm.indosId,
        berthAllocationId: allocForm.berthAllocationId,
        startDate: new Date(allocForm.startDate).toISOString(),
        endDate: new Date(allocForm.endDate).toISOString()
      });
      showAllocate = false;
      toast.success('Seafarer allocated to berth');
    } finally { saving = false; }
  }
</script>

<svelte:head><title>{vessel?.name ?? 'Vessel'} — Artemis</title></svelte:head>

{#if loading}
  <div class="flex items-center justify-center py-24"><Loader2 class="size-8 animate-spin text-primary" /></div>
{:else if !vessel}
  <p class="text-center text-muted-foreground py-16">Vessel not found</p>
{:else}
  <div class="space-y-6 max-w-5xl mx-auto">
    <div class="flex items-center gap-2 text-sm text-muted-foreground">
      <a href="/vessels/companies/{vessel.company?.id}" class="hover:text-foreground flex items-center gap-1 transition-colors"><ArrowLeft class="size-3.5" />{vessel.company?.name ?? 'Company'}</a>
      <span>/</span><span class="text-foreground font-medium">{vessel.name}</span>
    </div>

    <!-- Vessel header -->
    <div class="rounded-2xl border border-border bg-card overflow-hidden">
      <div class="h-2 bg-gradient-to-r from-blue-500/50 to-cyan-400/30"></div>
      <div class="p-6">
        <div class="flex items-center gap-4 mb-5">
          <div class="size-16 rounded-2xl bg-blue-500/10 flex items-center justify-center"><Ship class="size-8 text-blue-500" /></div>
          <div>
            <h1 class="text-2xl font-bold text-foreground">{vessel.name}</h1>
            {#if vessel.imoNumber}<p class="font-mono text-sm text-muted-foreground">IMO {vessel.imoNumber}</p>{/if}
          </div>
        </div>
        <div class="grid grid-cols-2 sm:grid-cols-4 gap-4">
          {#each [['Flag', vessel.flag],['Type', vessel.type],['Gross Tonnage', vessel.grossTonnage ? vessel.grossTonnage + ' GT' : null],['Call Sign', vessel.callSign]] as [l, v]}
            {#if v}<div class="space-y-0.5"><p class="text-xs text-muted-foreground uppercase tracking-wide font-medium">{l}</p><p class="text-sm font-semibold text-foreground">{v}</p></div>{/if}
          {/each}
        </div>
        <div class="flex gap-2 mt-5">
          {#if canManage}
            <button onclick={() => (showBerthReq = true)} class="flex items-center gap-2 px-3 py-2 rounded-lg border border-border bg-secondary/50 text-sm font-medium hover:bg-secondary transition-all">
              <Plus class="size-3.5" />Training Berth Request
            </button>
          {/if}
          {#if canAllocate}
            <button onclick={() => (showAllocate = true)} class="flex items-center gap-2 px-3 py-2 rounded-lg border border-border bg-secondary/50 text-sm font-medium hover:bg-secondary transition-all">
              <Users class="size-3.5" />Allocate Seafarer
            </button>
          {/if}
        </div>
      </div>
    </div>

    <!-- IMO Crew List -->
    {#if canManage && crewList.length > 0}
      <div class="space-y-3">
        <h2 class="text-lg font-bold text-foreground flex items-center gap-2"><Users class="size-5 text-blue-500" />IMO Crew List</h2>
        <div class="rounded-xl border border-border bg-card overflow-hidden">
          <table class="w-full text-sm">
            <thead>
              <tr class="border-b border-border bg-muted/40">
                {#each Object.keys(crewList[0] ?? {}).slice(0, 6) as key}
                  <th class="text-left px-4 py-3 text-xs font-semibold text-muted-foreground uppercase tracking-wide">{key}</th>
                {/each}
              </tr>
            </thead>
            <tbody>
              {#each crewList as row}
                <tr class="border-b border-border last:border-0 hover:bg-accent/20 transition-colors">
                  {#each Object.values(row).slice(0, 6) as val}
                    <td class="px-4 py-3 text-foreground">{val ?? '—'}</td>
                  {/each}
                </tr>
              {/each}
            </tbody>
          </table>
        </div>
      </div>
    {:else if canManage}
      <div class="rounded-xl border border-border bg-card p-8 text-center">
        <Users class="size-10 text-muted-foreground/30 mx-auto mb-2" />
        <p class="text-muted-foreground">No crew allocated yet</p>
      </div>
    {/if}
  </div>

  <!-- Berth Request Modal -->
  {#if showBerthReq}
    <div class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-sm">
      <div class="bg-card rounded-2xl border border-border shadow-2xl w-full max-w-sm p-6 space-y-4">
        <div class="flex items-center justify-between"><h2 class="text-lg font-bold">Training Berth Request</h2><button onclick={() => (showBerthReq = false)}><X class="size-4" /></button></div>
        <div class="space-y-1.5">
          <label class="text-sm font-medium text-foreground">Requested Slots</label>
          <input type="number" min="1" bind:value={berthReqForm.requestedSlots} class="w-full px-3 py-2 rounded-lg border border-input bg-background text-sm outline-none focus:ring-2 focus:ring-ring transition-all" />
        </div>
        <div class="flex gap-3">
          <button onclick={() => (showBerthReq = false)} class="flex-1 py-2.5 rounded-lg border border-border text-sm font-medium hover:bg-accent transition-all">Cancel</button>
          <button onclick={submitBerthRequest} disabled={saving} class="flex-1 flex items-center justify-center gap-2 py-2.5 rounded-lg bg-primary text-primary-foreground text-sm font-semibold hover:bg-primary/90 disabled:opacity-60 transition-all">
            {#if saving}<Loader2 class="size-4 animate-spin" />Submitting…{:else}Submit Request{/if}
          </button>
        </div>
      </div>
    </div>
  {/if}

  <!-- Allocate Seafarer Modal -->
  {#if showAllocate}
    <div class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-sm">
      <div class="bg-card rounded-2xl border border-border shadow-2xl w-full max-w-md p-6 space-y-4">
        <div class="flex items-center justify-between"><h2 class="text-lg font-bold">Allocate Seafarer to Berth</h2><button onclick={() => (showAllocate = false)}><X class="size-4" /></button></div>
        <div class="space-y-3">
          {#each [['berthId','Berth ID (UUID)','text'],['indosId','INDoS ID (UUID)','text'],['berthAllocationId','Berth Allocation ID (UUID)','text'],['startDate','Start Date','datetime-local'],['endDate','End Date','datetime-local']] as [f, l, t]}
            <div class="space-y-1.5">
              <label class="text-sm font-medium text-foreground">{l}</label>
              <input type={t} bind:value={(allocForm as Record<string, string>)[f]} class="w-full px-3 py-2 rounded-lg border border-input bg-background text-sm outline-none focus:ring-2 focus:ring-ring transition-all font-mono text-xs" />
            </div>
          {/each}
        </div>
        <div class="flex gap-3">
          <button onclick={() => (showAllocate = false)} class="flex-1 py-2.5 rounded-lg border border-border text-sm font-medium hover:bg-accent transition-all">Cancel</button>
          <button onclick={submitAllocation} disabled={saving} class="flex-1 flex items-center justify-center gap-2 py-2.5 rounded-lg bg-primary text-primary-foreground text-sm font-semibold hover:bg-primary/90 disabled:opacity-60 transition-all">
            {#if saving}<Loader2 class="size-4 animate-spin" />Saving…{:else}Allocate{/if}
          </button>
        </div>
      </div>
    </div>
  {/if}
{/if}
