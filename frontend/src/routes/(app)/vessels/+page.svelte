<script lang="ts">
  import { onMount } from 'svelte';
  import { api } from '$lib/api';
  import type { Company } from '$lib/types';
  import { authStore } from '$lib/stores/auth.svelte';
  import { Building2, Plus, Search, ArrowRight, Loader2, X } from '@lucide/svelte';
  import { toast } from 'svelte-sonner';

  let companies = $state<Company[]>([]);
  let filtered = $state<Company[]>([]);
  let loading = $state(true);
  let search = $state('');
  let showAdd = $state(false);
  let form = $state<Partial<Company>>({});
  let saving = $state(false);

  const isAdmin = $derived(authStore.hasRole('DG_SHIPPING_ADMIN'));

  onMount(async () => {
    try { companies = await api.get<Company[]>('/api/v1/companies'); filtered = companies; }
    finally { loading = false; }
  });

  $effect(() => {
    const q = search.toLowerCase();
    filtered = companies.filter((c) => !q || c.name.toLowerCase().includes(q) || c.country?.toLowerCase().includes(q));
  });

  async function createCompany() {
    saving = true;
    try {
      const created = await api.post<Company>('/api/v1/companies', form);
      companies = [created, ...companies];
      showAdd = false; form = {};
      toast.success('Company created');
    } finally { saving = false; }
  }
</script>

<svelte:head><title>Vessels & Companies — Artemis</title></svelte:head>

<div class="space-y-6 max-w-7xl mx-auto">
  <div class="flex items-center justify-between flex-wrap gap-3">
    <div>
      <h1 class="text-2xl font-bold text-foreground">Vessels & Companies</h1>
      <p class="text-sm text-muted-foreground">Manage shipping companies and their fleets</p>
    </div>
    <div class="flex gap-2">
      <a href="/vessels/berths" class="px-3 py-2 rounded-lg border border-border bg-secondary/50 text-sm font-medium hover:bg-secondary transition-all">Berths & Timeline</a>
      {#if isAdmin}
        <button onclick={() => (showAdd = true)} class="flex items-center gap-2 px-4 py-2 rounded-lg bg-primary text-primary-foreground text-sm font-semibold hover:bg-primary/90 transition-all shadow-sm">
          <Plus class="size-4" />Add Company
        </button>
      {/if}
    </div>
  </div>

  <div class="relative max-w-sm">
    <Search class="absolute left-3 top-1/2 -translate-y-1/2 size-4 text-muted-foreground pointer-events-none" />
    <input type="text" bind:value={search} placeholder="Search companies…" class="w-full pl-10 pr-4 py-2.5 rounded-lg border border-input bg-background text-sm outline-none focus:ring-2 focus:ring-ring transition-all" />
  </div>

  {#if loading}
    <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
      {#each Array(6) as _}<div class="rounded-xl border border-border bg-card p-5 animate-pulse h-32"></div>{/each}
    </div>
  {:else if filtered.length === 0}
    <div class="rounded-xl border border-border bg-card p-12 text-center">
      <Building2 class="size-12 text-muted-foreground/30 mx-auto mb-3" />
      <p class="text-muted-foreground font-medium">No companies found</p>
    </div>
  {:else}
    <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
      {#each filtered as co}
        <a href="/vessels/companies/{co.id}" class="group rounded-xl border border-border bg-card p-5 hover:shadow-md hover:shadow-black/5 hover:-translate-y-0.5 transition-all duration-200 block">
          <div class="flex items-start justify-between mb-3">
            <div class="size-10 rounded-xl bg-blue-500/10 flex items-center justify-center">
              <Building2 class="size-5 text-blue-500" />
            </div>
            <ArrowRight class="size-4 text-muted-foreground/40 group-hover:text-primary group-hover:translate-x-0.5 transition-all" />
          </div>
          <h3 class="font-bold text-foreground truncate">{co.name}</h3>
          {#if co.imoCompanyNumber}<p class="text-xs font-mono text-muted-foreground mt-0.5">IMO {co.imoCompanyNumber}</p>{/if}
          <div class="flex items-center gap-2 mt-2 text-xs text-muted-foreground">
            {#if co.country}<span>{co.country}</span>{/if}
            {#if co.contactEmail}<span class="truncate">{co.contactEmail}</span>{/if}
          </div>
        </a>
      {/each}
    </div>
  {/if}
</div>

{#if showAdd}
  <div class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-sm">
    <div class="bg-card rounded-2xl border border-border shadow-2xl w-full max-w-md p-6 space-y-4">
      <div class="flex items-center justify-between">
        <h2 class="text-lg font-bold">New Company</h2>
        <button onclick={() => (showAdd = false)}><X class="size-4" /></button>
      </div>
      <div class="space-y-3">
        {#each [['name','Company Name *','text'],['imoCompanyNumber','IMO Company Number','text'],['address','Address','text'],['country','Country','text'],['contactEmail','Contact Email','email'],['contactPhone','Contact Phone','tel']] as [f, l, t]}
          <div class="space-y-1.5">
            <label class="text-sm font-medium text-foreground">{l}</label>
            <input type={t} bind:value={(form as Record<string, string>)[f]} class="w-full px-3 py-2 rounded-lg border border-input bg-background text-sm outline-none focus:ring-2 focus:ring-ring transition-all" />
          </div>
        {/each}
      </div>
      <div class="flex gap-3">
        <button onclick={() => (showAdd = false)} class="flex-1 py-2.5 rounded-lg border border-border text-sm font-medium hover:bg-accent transition-all">Cancel</button>
        <button onclick={createCompany} disabled={saving || !form.name} class="flex-1 flex items-center justify-center gap-2 py-2.5 rounded-lg bg-primary text-primary-foreground text-sm font-semibold hover:bg-primary/90 disabled:opacity-60 transition-all">
          {#if saving}<Loader2 class="size-4 animate-spin" />Saving…{:else}Create{/if}
        </button>
      </div>
    </div>
  </div>
{/if}
