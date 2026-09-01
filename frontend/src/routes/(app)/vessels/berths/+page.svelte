<script lang="ts">
  import { onMount } from 'svelte';
  import { api } from '$lib/api';
  import type { Berth, BerthSeafarerAllocation, TrainingBerthRequest } from '$lib/types';
  import { authStore } from '$lib/stores/auth.svelte';
  import { Anchor, Plus, X, Loader2, Check } from '@lucide/svelte';
  import { toast } from 'svelte-sonner';

  let berths = $state<Berth[]>([]);
  let allocations = $state<BerthSeafarerAllocation[]>([]);
  let loading = $state(true);
  let showAddBerth = $state(false);
  let berthForm = $state<Partial<Berth>>({});
  let saving = $state(false);
  let showAllocateBerth = $state(false);
  let berthAllocForm = $state<{ berthId: string; vesselId: string; startDate: string; endDate: string }>({ berthId: '', vesselId: '', startDate: '', endDate: '' });
  let approvingId = $state<string | null>(null);
  let approveForm = $state<{ approvedSlots: number; concessionRate: number }>({ approvedSlots: 1, concessionRate: 0 });

  const isAdmin = $derived(authStore.hasRole('DG_SHIPPING_ADMIN'));
  const canAllocateBerth = $derived(authStore.hasRole('DG_SHIPPING_ADMIN'));

  onMount(async () => {
    loading = true;
    try {
      [berths, allocations] = await Promise.all([
        api.get<Berth[]>('/api/v1/vessels/berths'),
        api.get<BerthSeafarerAllocation[]>('/api/v1/vessels/seafarer-allocations/timeline')
      ]);
    } finally { loading = false; }
  });

  async function addBerth() {
    saving = true;
    try {
      const created = await api.post<Berth>('/api/v1/vessels/berths', berthForm);
      berths = [...berths, created];
      showAddBerth = false; berthForm = {};
      toast.success('Berth created');
    } finally { saving = false; }
  }

  async function allocateBerth() {
    saving = true;
    try {
      await api.post('/api/v1/vessels/berth-allocations', undefined, {
        berthId: berthAllocForm.berthId, vesselId: berthAllocForm.vesselId,
        startDate: new Date(berthAllocForm.startDate).toISOString(),
        endDate: new Date(berthAllocForm.endDate).toISOString()
      });
      showAllocateBerth = false;
      toast.success('Berth allocated to vessel');
    } finally { saving = false; }
  }

  // Gantt helpers
  const now = new Date();
  const DAYS = 60;
  const start = new Date(now); start.setDate(start.getDate() - 10);
  const end = new Date(start); end.setDate(end.getDate() + DAYS);
  const totalMs = end.getTime() - start.getTime();

  function pct(date: string | Date) {
    const ms = new Date(date).getTime() - start.getTime();
    return Math.max(0, Math.min(100, (ms / totalMs) * 100));
  }
  function width(s: string, e: string) {
    return Math.max(1, pct(e) - pct(s));
  }

  const ganttRows = $derived(
    allocations.map((a) => ({
      name: `${a.indosMaster?.firstName ?? ''} ${a.indosMaster?.lastName ?? ''}`.trim(),
      berth: a.berth?.name ?? '',
      start: a.startDate,
      end: a.endDate,
      leftPct: pct(a.startDate),
      widthPct: width(a.startDate, a.endDate)
    }))
  );

  // Today marker
  const todayPct = $derived(pct(now));
</script>

<svelte:head><title>Berths & Timeline — Artemis</title></svelte:head>

<div class="space-y-8 max-w-7xl mx-auto">
  <div class="flex items-center justify-between flex-wrap gap-3">
    <div>
      <div class="flex items-center gap-2 text-sm text-muted-foreground mb-1">
        <a href="/vessels" class="hover:text-foreground transition-colors">Companies</a><span>/</span>
        <span class="text-foreground font-medium">Berths & Timeline</span>
      </div>
      <h1 class="text-2xl font-bold text-foreground">Berths & Timeline</h1>
    </div>
    <div class="flex gap-2">
      {#if canAllocateBerth}
        <button onclick={() => (showAllocateBerth = true)} class="px-3 py-2 rounded-lg border border-border bg-secondary/50 text-sm font-medium hover:bg-secondary transition-all">Allocate Berth to Vessel</button>
      {/if}
      {#if isAdmin}
        <button onclick={() => (showAddBerth = true)} class="flex items-center gap-2 px-4 py-2 rounded-lg bg-primary text-primary-foreground text-sm font-semibold hover:bg-primary/90 transition-all shadow-sm">
          <Plus class="size-4" />Add Berth
        </button>
      {/if}
    </div>
  </div>

  <!-- Berths list -->
  <div class="space-y-3">
    <h2 class="text-lg font-bold text-foreground flex items-center gap-2"><Anchor class="size-5 text-primary" />Berths</h2>
    {#if loading}
      <div class="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-3">
        {#each Array(4) as _}<div class="rounded-xl border border-border bg-card p-4 animate-pulse h-20"></div>{/each}
      </div>
    {:else if berths.length === 0}
      <div class="rounded-xl border border-border bg-card p-8 text-center"><Anchor class="size-10 text-muted-foreground/30 mx-auto mb-2" /><p class="text-muted-foreground">No berths defined</p></div>
    {:else}
      <div class="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-3">
        {#each berths as b}
          <div class="rounded-xl border border-border bg-card p-4">
            <div class="size-8 rounded-lg bg-primary/10 flex items-center justify-center mb-2"><Anchor class="size-4 text-primary" /></div>
            <p class="font-bold text-foreground truncate">{b.name}</p>
            {#if b.location}<p class="text-xs text-muted-foreground">{b.location}</p>{/if}
            <p class="text-xs text-muted-foreground mt-1">Capacity: <span class="font-semibold text-foreground">{b.capacity}</span></p>
          </div>
        {/each}
      </div>
    {/if}
  </div>

  <!-- Gantt Chart -->
  <div class="space-y-3">
    <h2 class="text-lg font-bold text-foreground">Seafarer Allocation Timeline (60 days)</h2>
    <div class="rounded-xl border border-border bg-card overflow-hidden">
      {#if loading}
        <div class="p-8 text-center"><Loader2 class="size-8 animate-spin text-primary mx-auto" /></div>
      {:else if ganttRows.length === 0}
        <div class="p-12 text-center"><p class="text-muted-foreground">No active allocations in the next 60 days</p></div>
      {:else}
        <!-- Header: date markers -->
        <div class="border-b border-border bg-muted/30 px-4 py-2 relative overflow-hidden" style="height:36px">
          {#each Array(5) as _, i}
            {@const markerDate = new Date(start); markerDate.setDate(markerDate.getDate() + Math.floor((DAYS / 4) * i));}
            <span class="absolute text-xs text-muted-foreground" style="left:{(i / 4) * 100}%">{markerDate.toLocaleDateString(undefined, { month: 'short', day: 'numeric' })}</span>
          {/each}
        </div>
        <!-- Rows -->
        <div class="divide-y divide-border">
          {#each ganttRows as row}
            <div class="flex items-center gap-0 hover:bg-accent/10 transition-colors group">
              <!-- Label -->
              <div class="w-40 shrink-0 px-4 py-3 border-r border-border">
                <p class="text-xs font-semibold text-foreground truncate">{row.name}</p>
                <p class="text-[10px] text-muted-foreground truncate">{row.berth}</p>
              </div>
              <!-- Bar track -->
              <div class="flex-1 relative h-12 overflow-hidden">
                <!-- Today line -->
                <div class="absolute top-0 bottom-0 w-px bg-primary/40 z-10" style="left:{todayPct}%"></div>
                <!-- Allocation bar -->
                <div
                  class="absolute top-2 bottom-2 rounded-md bg-primary/70 group-hover:bg-primary transition-colors flex items-center px-2 overflow-hidden"
                  style="left:{row.leftPct}%;width:{row.widthPct}%"
                >
                  <span class="text-[10px] text-primary-foreground font-semibold truncate whitespace-nowrap">
                    {new Date(row.start).toLocaleDateString()} – {new Date(row.end).toLocaleDateString()}
                  </span>
                </div>
              </div>
            </div>
          {/each}
        </div>
      {/if}
    </div>
  </div>
</div>

<!-- Add Berth Modal -->
{#if showAddBerth}
  <div class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-sm">
    <div class="bg-card rounded-2xl border border-border shadow-2xl w-full max-w-sm p-6 space-y-4">
      <div class="flex items-center justify-between"><h2 class="text-lg font-bold">New Berth</h2><button onclick={() => (showAddBerth = false)}><X class="size-4" /></button></div>
      <div class="space-y-3">
        {#each [['name','Berth Name *','text'],['location','Location','text'],['capacity','Capacity *','number']] as [f, l, t]}
          <div class="space-y-1.5">
            <label class="text-sm font-medium text-foreground">{l}</label>
            <input type={t} bind:value={(berthForm as Record<string, string | number>)[f]} class="w-full px-3 py-2 rounded-lg border border-input bg-background text-sm outline-none focus:ring-2 focus:ring-ring transition-all" />
          </div>
        {/each}
      </div>
      <div class="flex gap-3">
        <button onclick={() => (showAddBerth = false)} class="flex-1 py-2.5 rounded-lg border border-border text-sm font-medium hover:bg-accent transition-all">Cancel</button>
        <button onclick={addBerth} disabled={saving || !berthForm.name} class="flex-1 flex items-center justify-center gap-2 py-2.5 rounded-lg bg-primary text-primary-foreground text-sm font-semibold hover:bg-primary/90 disabled:opacity-60 transition-all">
          {#if saving}<Loader2 class="size-4 animate-spin" />Saving…{:else}<Check class="size-4" />Create Berth{/if}
        </button>
      </div>
    </div>
  </div>
{/if}

<!-- Allocate Berth to Vessel Modal -->
{#if showAllocateBerth}
  <div class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-sm">
    <div class="bg-card rounded-2xl border border-border shadow-2xl w-full max-w-md p-6 space-y-4">
      <div class="flex items-center justify-between"><h2 class="text-lg font-bold">Allocate Berth to Vessel</h2><button onclick={() => (showAllocateBerth = false)}><X class="size-4" /></button></div>
      <div class="space-y-3">
        {#each [['berthId','Berth ID (UUID)','text'],['vesselId','Vessel ID (UUID)','text'],['startDate','Start Date','datetime-local'],['endDate','End Date','datetime-local']] as [f, l, t]}
          <div class="space-y-1.5">
            <label class="text-sm font-medium text-foreground">{l}</label>
            <input type={t} bind:value={(berthAllocForm as Record<string, string>)[f]} class="w-full px-3 py-2 rounded-lg border border-input bg-background text-sm outline-none focus:ring-2 focus:ring-ring transition-all font-mono text-xs" />
          </div>
        {/each}
      </div>
      <div class="flex gap-3">
        <button onclick={() => (showAllocateBerth = false)} class="flex-1 py-2.5 rounded-lg border border-border text-sm font-medium hover:bg-accent transition-all">Cancel</button>
        <button onclick={allocateBerth} disabled={saving} class="flex-1 flex items-center justify-center gap-2 py-2.5 rounded-lg bg-primary text-primary-foreground text-sm font-semibold hover:bg-primary/90 disabled:opacity-60 transition-all">
          {#if saving}<Loader2 class="size-4 animate-spin" />Saving…{:else}Allocate{/if}
        </button>
      </div>
    </div>
  </div>
{/if}
