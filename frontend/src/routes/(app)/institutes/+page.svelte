<script lang="ts">
  import { onMount } from 'svelte';
  import { api } from '$lib/api';
  import type { Institute } from '$lib/types';
  import { authStore } from '$lib/stores/auth.svelte';
  import { GraduationCap, Plus, Search, ArrowRight, Loader2, X } from '@lucide/svelte';
  import { toast } from 'svelte-sonner';

  let institutes = $state<Institute[]>([]);
  let filtered = $state<Institute[]>([]);
  let loading = $state(true);
  let search = $state('');
  let showAdd = $state(false);
  let form = $state<Partial<Institute>>({});
  let saving = $state(false);

  const isAdmin = $derived(authStore.hasRole('DG_SHIPPING_ADMIN'));

  $effect(() => {
    const q = search.toLowerCase();
    filtered = institutes.filter((i) => !q || i.name.toLowerCase().includes(q) || i.code?.toLowerCase().includes(q));
  });

  onMount(async () => {
    try { institutes = await api.get<Institute[]>('/api/v1/institutes'); filtered = institutes; }
    finally { loading = false; }
  });

  async function create() {
    saving = true;
    try {
      const created = await api.post<Institute>('/api/v1/institutes', form);
      institutes = [created, ...institutes];
      showAdd = false; form = {};
      toast.success('Institute created');
    } finally { saving = false; }
  }
</script>

<svelte:head><title>Institutes — Artemis</title></svelte:head>

<div class="space-y-6 max-w-7xl mx-auto">
  <div class="flex items-center justify-between flex-wrap gap-3">
    <div>
      <h1 class="text-2xl font-bold text-foreground">Training Institutes</h1>
      <p class="text-sm text-muted-foreground">Accredited pre-sea training institutions</p>
    </div>
    {#if isAdmin}
      <button onclick={() => (showAdd = true)} class="flex items-center gap-2 px-4 py-2 rounded-lg bg-primary text-primary-foreground text-sm font-semibold hover:bg-primary/90 transition-all shadow-sm">
        <Plus class="size-4" />Add Institute
      </button>
    {/if}
  </div>

  <div class="relative max-w-sm">
    <Search class="absolute left-3 top-1/2 -translate-y-1/2 size-4 text-muted-foreground pointer-events-none" />
    <input type="text" bind:value={search} placeholder="Search institutes…" class="w-full pl-10 pr-4 py-2.5 rounded-lg border border-input bg-background text-sm outline-none focus:ring-2 focus:ring-ring transition-all" />
  </div>

  {#if loading}
    <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
      {#each Array(6) as _}<div class="rounded-xl border border-border bg-card p-5 animate-pulse h-36"></div>{/each}
    </div>
  {:else if filtered.length === 0}
    <div class="rounded-xl border border-border bg-card p-12 text-center">
      <GraduationCap class="size-12 text-muted-foreground/30 mx-auto mb-3" />
      <p class="text-muted-foreground font-medium">No institutes found</p>
    </div>
  {:else}
    <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
      {#each filtered as inst}
        <a href="/institutes/{inst.id}" class="group rounded-xl border border-border bg-card p-5 hover:shadow-md hover:-translate-y-0.5 transition-all duration-200 block">
          <div class="flex items-start justify-between mb-3">
            <div class="size-10 rounded-xl bg-emerald-500/10 flex items-center justify-center"><GraduationCap class="size-5 text-emerald-500" /></div>
            <ArrowRight class="size-4 text-muted-foreground/40 group-hover:text-primary group-hover:translate-x-0.5 transition-all" />
          </div>
          <h3 class="font-bold text-foreground">{inst.name}</h3>
          {#if inst.code}<p class="text-xs font-mono text-muted-foreground mt-0.5">{inst.code}</p>{/if}
          <div class="flex flex-wrap gap-2 mt-2">
            {#if inst.accreditationStatus}<span class="text-xs px-2 py-0.5 rounded-full bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 font-medium">{inst.accreditationStatus}</span>{/if}
            {#if inst.contactEmail}<span class="text-xs text-muted-foreground truncate">{inst.contactEmail}</span>{/if}
          </div>
        </a>
      {/each}
    </div>
  {/if}
</div>

{#if showAdd}
  <div class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-sm">
    <div class="bg-card rounded-2xl border border-border shadow-2xl w-full max-w-md p-6 space-y-4">
      <div class="flex items-center justify-between"><h2 class="text-lg font-bold">New Institute</h2><button onclick={() => (showAdd = false)}><X class="size-4" /></button></div>
      <div class="space-y-3">
        {#each [['name','Name *','text'],['code','Code','text'],['address','Address','text'],['contactEmail','Contact Email','email'],['contactPhone','Contact Phone','tel'],['accreditationStatus','Accreditation Status','text']] as [f, l, t]}
          <div class="space-y-1.5">
            <label class="text-sm font-medium text-foreground">{l}</label>
            <input type={t} bind:value={(form as Record<string, string>)[f]} class="w-full px-3 py-2 rounded-lg border border-input bg-background text-sm outline-none focus:ring-2 focus:ring-ring transition-all" />
          </div>
        {/each}
      </div>
      <div class="flex gap-3">
        <button onclick={() => (showAdd = false)} class="flex-1 py-2.5 rounded-lg border border-border text-sm font-medium hover:bg-accent transition-all">Cancel</button>
        <button onclick={create} disabled={saving || !form.name} class="flex-1 flex items-center justify-center gap-2 py-2.5 rounded-lg bg-primary text-primary-foreground text-sm font-semibold hover:bg-primary/90 disabled:opacity-60 transition-all">
          {#if saving}<Loader2 class="size-4 animate-spin" />Saving…{:else}Create{/if}
        </button>
      </div>
    </div>
  </div>
{/if}
