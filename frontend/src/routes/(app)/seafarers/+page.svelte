<script lang="ts">
  import { onMount } from 'svelte';
  import { api } from '$lib/api';
  import type { IndosMaster, RankMaster } from '$lib/types';
  import { authStore } from '$lib/stores/auth.svelte';
  import StatusBadge from '$lib/components/shared/StatusBadge.svelte';
  import { Search, Plus, ToggleLeft, ToggleRight, Loader2, Users, X } from '@lucide/svelte';
  import { toast } from 'svelte-sonner';

  let indos = $state<IndosMaster[]>([]);
  let ranks = $state<RankMaster[]>([]);
  let loading = $state(true);
  let searchIndos = $state('');
  let searchName = $state('');
  let showAddModal = $state(false);
  let toggling = $state<string | null>(null);

  // New record form
  let form = $state<Partial<IndosMaster & { rankId: string }>>({});
  let saving = $state(false);

  const isAdmin = $derived(authStore.hasRole('DG_SHIPPING_ADMIN'));

  async function load() {
    loading = true;
    const params: Record<string, string> = {};
    if (searchIndos) params.indos = searchIndos;
    if (searchName) params.firstName = searchName;
    try {
      [indos, ranks] = await Promise.all([
        api.get<IndosMaster[]>('/api/v1/seafarers/indos', params),
        api.get<RankMaster[]>('/api/v1/seafarers/ranks')
      ]);
    } finally {
      loading = false;
    }
  }

  onMount(load);

  let debounce: ReturnType<typeof setTimeout>;
  function onSearch() {
    clearTimeout(debounce);
    debounce = setTimeout(load, 400);
  }

  async function toggleStatus(rec: IndosMaster) {
    toggling = rec.id;
    try {
      const updated = await api.put<IndosMaster>(`/api/v1/seafarers/indos/${rec.id}/status`, undefined, { active: !rec.active });
      indos = indos.map((i) => (i.id === rec.id ? updated : i));
      toast.success(`INDoS ${updated.active ? 'activated' : 'deactivated'}`);
    } catch {
      /* toast shown by api.ts */
    } finally {
      toggling = null;
    }
  }

  async function createRecord() {
    saving = true;
    try {
      const created = await api.post<IndosMaster>('/api/v1/seafarers/indos', form);
      indos = [created, ...indos];
      showAddModal = false;
      form = {};
      toast.success('INDoS record created');
    } finally {
      saving = false;
    }
  }
</script>

<svelte:head><title>Seafarers — Artemis</title></svelte:head>

<div class="space-y-6 max-w-7xl mx-auto">
  <!-- Header -->
  <div class="flex items-center justify-between flex-wrap gap-3">
    <div>
      <h1 class="text-2xl font-bold text-foreground">Seafarers</h1>
      <p class="text-sm text-muted-foreground">Search and manage INDoS seafarer records</p>
    </div>
    <div class="flex gap-2">
      {#if isAdmin}
        <a href="/seafarers/ranks" class="flex items-center gap-2 px-3 py-2 rounded-lg border border-border bg-secondary/50 text-sm font-medium hover:bg-secondary transition-all">Rank Master</a>
        <button onclick={() => (showAddModal = true)} class="flex items-center gap-2 px-4 py-2 rounded-lg bg-primary text-primary-foreground text-sm font-semibold hover:bg-primary/90 transition-all shadow-sm">
          <Plus class="size-4" />Add INDoS
        </button>
      {/if}
    </div>
  </div>

  <!-- Search bar -->
  <div class="flex gap-3 flex-wrap">
    <div class="relative flex-1 min-w-48">
      <Search class="absolute left-3 top-1/2 -translate-y-1/2 size-4 text-muted-foreground pointer-events-none" />
      <input type="text" bind:value={searchIndos} oninput={onSearch} placeholder="Search by INDoS number…" class="w-full pl-10 pr-4 py-2.5 rounded-lg border border-input bg-background text-sm outline-none focus:ring-2 focus:ring-ring transition-all" />
    </div>
    <div class="relative flex-1 min-w-48">
      <Search class="absolute left-3 top-1/2 -translate-y-1/2 size-4 text-muted-foreground pointer-events-none" />
      <input type="text" bind:value={searchName} oninput={onSearch} placeholder="Search by first name…" class="w-full pl-10 pr-4 py-2.5 rounded-lg border border-input bg-background text-sm outline-none focus:ring-2 focus:ring-ring transition-all" />
    </div>
  </div>

  <!-- Table -->
  <div class="rounded-xl border border-border bg-card overflow-hidden">
    <table class="w-full text-sm">
      <thead>
        <tr class="border-b border-border bg-muted/40">
          <th class="text-left px-4 py-3 font-semibold text-muted-foreground text-xs uppercase tracking-wide">INDoS No.</th>
          <th class="text-left px-4 py-3 font-semibold text-muted-foreground text-xs uppercase tracking-wide">Name</th>
          <th class="text-left px-4 py-3 font-semibold text-muted-foreground text-xs uppercase tracking-wide">Rank</th>
          <th class="text-left px-4 py-3 font-semibold text-muted-foreground text-xs uppercase tracking-wide">Nationality</th>
          <th class="text-left px-4 py-3 font-semibold text-muted-foreground text-xs uppercase tracking-wide">Status</th>
          <th class="px-4 py-3"></th>
        </tr>
      </thead>
      <tbody>
        {#if loading}
          {#each Array(5) as _}
            <tr class="border-b border-border last:border-0">
              {#each Array(6) as _}
                <td class="px-4 py-3"><div class="h-4 rounded bg-muted animate-pulse"></div></td>
              {/each}
            </tr>
          {/each}
        {:else if indos.length === 0}
          <tr>
            <td colspan="6" class="px-4 py-12 text-center">
              <Users class="size-10 text-muted-foreground/40 mx-auto mb-3" />
              <p class="text-muted-foreground font-medium">No seafarers found</p>
              <p class="text-xs text-muted-foreground/60 mt-1">Try adjusting your search filters</p>
            </td>
          </tr>
        {:else}
          {#each indos as rec}
            <tr class="border-b border-border last:border-0 hover:bg-accent/20 transition-colors">
              <td class="px-4 py-3 font-mono font-semibold text-primary">{rec.indosNo}</td>
              <td class="px-4 py-3 font-medium text-foreground">{rec.firstName} {rec.middleName ? rec.middleName + ' ' : ''}{rec.lastName}</td>
              <td class="px-4 py-3 text-muted-foreground">{rec.rank?.name ?? '—'}</td>
              <td class="px-4 py-3 text-muted-foreground">{rec.nationality ?? '—'}</td>
              <td class="px-4 py-3">
                <StatusBadge status={rec.active ? 'active' : 'inactive'} />
              </td>
              <td class="px-4 py-3">
                <div class="flex items-center gap-2 justify-end">
                  {#if isAdmin}
                    <button onclick={() => toggleStatus(rec)} disabled={toggling === rec.id} class="p-1.5 rounded-lg hover:bg-accent transition-colors text-muted-foreground hover:text-foreground disabled:opacity-50">
                      {#if toggling === rec.id}
                        <Loader2 class="size-4 animate-spin" />
                      {:else if rec.active}
                        <ToggleRight class="size-4 text-emerald-500" />
                      {:else}
                        <ToggleLeft class="size-4" />
                      {/if}
                    </button>
                  {/if}
                  <a href="/seafarers/{rec.id}" class="px-3 py-1.5 rounded-lg bg-primary/10 text-primary text-xs font-semibold hover:bg-primary/20 transition-colors">View</a>
                </div>
              </td>
            </tr>
          {/each}
        {/if}
      </tbody>
    </table>
  </div>
</div>

<!-- Add INDoS Modal -->
{#if showAddModal}
  <div class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-sm" role="dialog" aria-modal="true">
    <div class="bg-card rounded-2xl border border-border shadow-2xl w-full max-w-md p-6 space-y-4">
      <div class="flex items-center justify-between">
        <h2 class="text-lg font-bold text-foreground">Add INDoS Record</h2>
        <button onclick={() => (showAddModal = false)} class="p-1.5 rounded-lg hover:bg-accent transition-colors"><X class="size-4" /></button>
      </div>
      <div class="grid grid-cols-2 gap-3">
        {#each [['indosNo','INDoS Number *','text'],['firstName','First Name *','text'],['middleName','Middle Name','text'],['lastName','Last Name *','text'],['nationality','Nationality','text'],['dateOfBirth','Date of Birth','date']] as [field, label, type]}
          <div class="space-y-1.5 {field === 'indosNo' ? 'col-span-2' : ''}">
            <label class="text-sm font-medium text-foreground">{label}</label>
            <input {type} bind:value={(form as Record<string, string>)[field]} class="w-full px-3 py-2 rounded-lg border border-input bg-background text-sm outline-none focus:ring-2 focus:ring-ring transition-all" />
          </div>
        {/each}
        <div class="space-y-1.5 col-span-2">
          <label class="text-sm font-medium text-foreground">Rank</label>
          <select bind:value={form.rankId} class="w-full px-3 py-2 rounded-lg border border-input bg-background text-sm outline-none focus:ring-2 focus:ring-ring transition-all">
            <option value="">Select rank…</option>
            {#each ranks as r}<option value={r.id}>{r.name}</option>{/each}
          </select>
        </div>
      </div>
      <div class="flex gap-3 pt-2">
        <button onclick={() => (showAddModal = false)} class="flex-1 py-2.5 rounded-lg border border-border text-sm font-medium hover:bg-accent transition-all">Cancel</button>
        <button onclick={createRecord} disabled={saving} class="flex-1 flex items-center justify-center gap-2 py-2.5 rounded-lg bg-primary text-primary-foreground text-sm font-semibold hover:bg-primary/90 disabled:opacity-60 transition-all">
          {#if saving}<Loader2 class="size-4 animate-spin" />Saving…{:else}Create Record{/if}
        </button>
      </div>
    </div>
  </div>
{/if}
