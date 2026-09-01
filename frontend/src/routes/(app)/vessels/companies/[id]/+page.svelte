<script lang="ts">
  import { onMount } from 'svelte';
  import { page } from '$app/state';
  import { api } from '$lib/api';
  import type { Company, Vessel, Contract, ConcessionLedger } from '$lib/types';
  import { authStore } from '$lib/stores/auth.svelte';
  import StatusBadge from '$lib/components/shared/StatusBadge.svelte';
  import { Ship, Plus, Pencil, ArrowLeft, Loader2, X, DollarSign, FileText } from '@lucide/svelte';
  import { toast } from 'svelte-sonner';

  const companyId = $derived(page.params.id);

  let company = $state<Company | null>(null);
  let vessels = $state<Vessel[]>([]);
  let contracts = $state<Contract[]>([]);
  let concessions = $state<ConcessionLedger[]>([]);
  let loading = $state(true);
  let showAddVessel = $state(false);
  let vesselForm = $state<Partial<Vessel>>({});
  let saving = $state(false);
  let editMode = $state(false);
  let editForm = $state<Partial<Company>>({});

  const canEdit = $derived(authStore.hasRole('DG_SHIPPING_ADMIN', 'COMPANY_ADMIN'));
  const isAdmin = $derived(authStore.hasRole('DG_SHIPPING_ADMIN'));

  onMount(async () => {
    loading = true;
    try {
      [company, vessels] = await Promise.all([
        api.get<Company>(`/api/v1/companies/${companyId}`),
        api.get<Vessel[]>(`/api/v1/companies/${companyId}/vessels`)
      ]);
      if (canEdit) {
        [contracts, concessions] = await Promise.all([
          api.get<Contract[]>(`/api/v1/companies/${companyId}/contracts`).catch(() => []),
          api.get<ConcessionLedger[]>(`/api/v1/companies/${companyId}/concessions`).catch(() => [])
        ]);
      }
    } finally { loading = false; }
  });

  async function updateCompany() {
    saving = true;
    try {
      company = await api.put<Company>(`/api/v1/companies/${companyId}`, editForm);
      editMode = false; toast.success('Company updated');
    } finally { saving = false; }
  }

  async function addVessel() {
    saving = true;
    try {
      const created = await api.post<Vessel>(`/api/v1/companies/${companyId}/vessels`, vesselForm);
      vessels = [...vessels, created];
      showAddVessel = false; vesselForm = {};
      toast.success('Vessel added');
    } finally { saving = false; }
  }
</script>

<svelte:head><title>{company?.name ?? 'Company'} — Artemis</title></svelte:head>

{#if loading}
  <div class="flex items-center justify-center py-24"><Loader2 class="size-8 animate-spin text-primary" /></div>
{:else if !company}
  <p class="text-center text-muted-foreground py-16">Company not found</p>
{:else}
  <div class="space-y-6 max-w-6xl mx-auto">
    <div class="flex items-center gap-2 text-sm text-muted-foreground">
      <a href="/vessels" class="hover:text-foreground flex items-center gap-1 transition-colors"><ArrowLeft class="size-3.5" />Companies</a>
      <span>/</span><span class="text-foreground font-medium">{company.name}</span>
    </div>

    <!-- Company card -->
    <div class="rounded-2xl border border-border bg-card p-6">
      <div class="flex items-start justify-between flex-wrap gap-4 mb-4">
        <div class="flex items-center gap-4">
          <div class="size-14 rounded-2xl bg-blue-500/10 flex items-center justify-center"><Ship class="size-7 text-blue-500" /></div>
          <div>
            <h1 class="text-2xl font-bold text-foreground">{company.name}</h1>
            {#if company.imoCompanyNumber}<p class="font-mono text-sm text-muted-foreground">IMO {company.imoCompanyNumber}</p>{/if}
          </div>
        </div>
        {#if canEdit && !editMode}
          <button onclick={() => { editMode = true; editForm = { ...company! }; }} class="flex items-center gap-2 px-3 py-2 rounded-lg border border-border text-sm font-medium hover:bg-accent transition-all">
            <Pencil class="size-3.5" />Edit
          </button>
        {/if}
      </div>

      {#if editMode}
        <div class="grid grid-cols-2 gap-3 border border-border rounded-xl p-4 bg-muted/20">
          {#each [['name','Company Name *','text'],['imoCompanyNumber','IMO Number','text'],['address','Address','text'],['country','Country','text'],['contactEmail','Email','email'],['contactPhone','Phone','tel']] as [f, l, t]}
            <div class="space-y-1.5">
              <label class="text-xs font-medium text-foreground">{l}</label>
              <input type={t} bind:value={(editForm as Record<string, string>)[f]} class="w-full px-3 py-2 rounded-lg border border-input bg-background text-sm outline-none focus:ring-2 focus:ring-ring transition-all" />
            </div>
          {/each}
          <div class="col-span-2 flex gap-2 pt-1">
            <button onclick={() => (editMode = false)} class="px-3 py-1.5 rounded-lg border border-border text-xs font-medium hover:bg-accent transition-all">Cancel</button>
            <button onclick={updateCompany} disabled={saving} class="flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-primary text-primary-foreground text-xs font-semibold hover:bg-primary/90 disabled:opacity-60 transition-all">
              {#if saving}<Loader2 class="size-3 animate-spin" />{/if}Save
            </button>
          </div>
        </div>
      {:else}
        <div class="grid grid-cols-2 sm:grid-cols-3 gap-4 text-sm">
          {#each [['Country', company.country],['Email', company.contactEmail],['Phone', company.contactPhone],['Address', company.address]] as [l, v]}
            {#if v}<div class="space-y-0.5"><p class="text-xs text-muted-foreground uppercase tracking-wide font-medium">{l}</p><p class="font-medium text-foreground">{v}</p></div>{/if}
          {/each}
        </div>
      {/if}
    </div>

    <!-- Stats row -->
    <div class="grid grid-cols-3 gap-4">
      {#each [[vessels.length,'Vessels','blue'],[contracts.length,'Contracts','violet'],[concessions.reduce((s,c) => s + c.credits, 0),'Concession Credits','amber']] as [val, lbl, col]}
        <div class="rounded-xl border border-border bg-card p-4 text-center">
          <p class="text-3xl font-bold text-foreground">{val}</p>
          <p class="text-xs text-muted-foreground mt-1">{lbl}</p>
        </div>
      {/each}
    </div>

    <!-- Vessels list -->
    <div class="space-y-3">
      <div class="flex items-center justify-between">
        <h2 class="text-lg font-bold text-foreground flex items-center gap-2"><Ship class="size-5 text-blue-500" />Fleet</h2>
        {#if canEdit}
          <button onclick={() => (showAddVessel = true)} class="flex items-center gap-2 px-3 py-2 rounded-lg bg-primary text-primary-foreground text-sm font-semibold hover:bg-primary/90 transition-all">
            <Plus class="size-4" />Add Vessel
          </button>
        {/if}
      </div>
      {#if vessels.length === 0}
        <div class="rounded-xl border border-border bg-card p-8 text-center"><Ship class="size-10 text-muted-foreground/30 mx-auto mb-2" /><p class="text-muted-foreground">No vessels registered</p></div>
      {:else}
        <div class="grid grid-cols-1 sm:grid-cols-2 gap-3">
          {#each vessels as v}
            <a href="/vessels/{v.id}" class="group rounded-xl border border-border bg-card p-4 hover:shadow-md hover:-translate-y-0.5 transition-all flex items-center gap-4">
              <div class="size-10 rounded-xl bg-blue-500/10 flex items-center justify-center shrink-0"><Ship class="size-5 text-blue-500" /></div>
              <div class="flex-1 min-w-0">
                <p class="font-bold text-foreground truncate">{v.name}</p>
                <div class="flex gap-3 text-xs text-muted-foreground mt-0.5">
                  {#if v.imoNumber}<span class="font-mono">IMO {v.imoNumber}</span>{/if}
                  {#if v.flag}<span>{v.flag}</span>{/if}
                  {#if v.type}<span>{v.type}</span>{/if}
                </div>
              </div>
            </a>
          {/each}
        </div>
      {/if}
    </div>

    <!-- Contracts summary -->
    {#if contracts.length > 0}
      <div class="space-y-3">
        <h2 class="text-lg font-bold text-foreground flex items-center gap-2"><FileText class="size-5 text-violet-500" />Recent Contracts</h2>
        <div class="rounded-xl border border-border bg-card overflow-hidden">
          <table class="w-full text-sm">
            <thead><tr class="border-b border-border bg-muted/40">
              <th class="text-left px-4 py-3 text-xs font-semibold text-muted-foreground uppercase tracking-wide">Seafarer</th>
              <th class="text-left px-4 py-3 text-xs font-semibold text-muted-foreground uppercase tracking-wide">Vessel</th>
              <th class="text-left px-4 py-3 text-xs font-semibold text-muted-foreground uppercase tracking-wide">Status</th>
              <th class="px-4 py-3"></th>
            </tr></thead>
            <tbody>
              {#each contracts.slice(0, 5) as c}
                <tr class="border-b border-border last:border-0 hover:bg-accent/20 transition-colors">
                  <td class="px-4 py-3 font-medium">{c.indosMaster?.firstName} {c.indosMaster?.lastName}</td>
                  <td class="px-4 py-3 text-muted-foreground">{c.vessel?.name ?? '—'}</td>
                  <td class="px-4 py-3"><StatusBadge status={c.status ?? 'DRAFT'} /></td>
                  <td class="px-4 py-3 text-right"><a href="/contracts/{c.id}" class="text-xs text-primary hover:underline">View</a></td>
                </tr>
              {/each}
            </tbody>
          </table>
        </div>
      </div>
    {/if}

    <!-- Concession Ledger -->
    {#if concessions.length > 0}
      <div class="space-y-3">
        <h2 class="text-lg font-bold text-foreground flex items-center gap-2"><DollarSign class="size-5 text-amber-500" />Concession Ledger</h2>
        <div class="rounded-xl border border-border bg-card overflow-hidden">
          <table class="w-full text-sm">
            <thead><tr class="border-b border-border bg-muted/40">
              <th class="text-left px-4 py-3 text-xs font-semibold text-muted-foreground uppercase tracking-wide">Description</th>
              <th class="text-right px-4 py-3 text-xs font-semibold text-muted-foreground uppercase tracking-wide">Credits</th>
              <th class="text-right px-4 py-3 text-xs font-semibold text-muted-foreground uppercase tracking-wide">Date</th>
            </tr></thead>
            <tbody>
              {#each concessions as c}
                <tr class="border-b border-border last:border-0">
                  <td class="px-4 py-3 text-muted-foreground">{c.description ?? '—'}</td>
                  <td class="px-4 py-3 text-right font-bold text-foreground">{c.credits}</td>
                  <td class="px-4 py-3 text-right text-muted-foreground text-xs">{c.createdAt ? new Date(c.createdAt).toLocaleDateString() : '—'}</td>
                </tr>
              {/each}
            </tbody>
          </table>
        </div>
      </div>
    {/if}
  </div>

  {#if showAddVessel}
    <div class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-sm">
      <div class="bg-card rounded-2xl border border-border shadow-2xl w-full max-w-md p-6 space-y-4">
        <div class="flex items-center justify-between"><h2 class="text-lg font-bold">Add Vessel</h2><button onclick={() => (showAddVessel = false)}><X class="size-4" /></button></div>
        <div class="space-y-3">
          {#each [['name','Vessel Name *','text'],['imoNumber','IMO Number','text'],['flag','Flag','text'],['type','Type','text'],['callSign','Call Sign','text'],['grossTonnage','Gross Tonnage','number']] as [f, l, t]}
            <div class="space-y-1.5">
              <label class="text-sm font-medium text-foreground">{l}</label>
              <input type={t} bind:value={(vesselForm as Record<string, string>)[f]} class="w-full px-3 py-2 rounded-lg border border-input bg-background text-sm outline-none focus:ring-2 focus:ring-ring transition-all" />
            </div>
          {/each}
        </div>
        <div class="flex gap-3">
          <button onclick={() => (showAddVessel = false)} class="flex-1 py-2.5 rounded-lg border border-border text-sm font-medium hover:bg-accent transition-all">Cancel</button>
          <button onclick={addVessel} disabled={saving || !vesselForm.name} class="flex-1 flex items-center justify-center gap-2 py-2.5 rounded-lg bg-primary text-primary-foreground text-sm font-semibold hover:bg-primary/90 disabled:opacity-60 transition-all">
            {#if saving}<Loader2 class="size-4 animate-spin" />Saving…{:else}Add Vessel{/if}
          </button>
        </div>
      </div>
    </div>
  {/if}
{/if}
