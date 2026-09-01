<script lang="ts">
  import { onMount } from 'svelte';
  import { page } from '$app/state';
  import { api } from '$lib/api';
  import type { IndosMaster, SeafarerMedical, RankMaster } from '$lib/types';
  import { authStore } from '$lib/stores/auth.svelte';
  import StatusBadge from '$lib/components/shared/StatusBadge.svelte';
  import { Stethoscope, Plus, Loader2, X, Calendar, User, Globe, ArrowLeft, Pencil } from '@lucide/svelte';
  import { toast } from 'svelte-sonner';

  const indosId = $derived(page.params.indosId);

  let record = $state<IndosMaster | null>(null);
  let medicals = $state<SeafarerMedical[]>([]);
  let ranks = $state<RankMaster[]>([]);
  let loading = $state(true);
  let showMedModal = $state(false);
  let medForm = $state<Partial<SeafarerMedical>>({});
  let savingMed = $state(false);
  let editMode = $state(false);
  let editForm = $state<Partial<IndosMaster & { rankId: string }>>({});
  let savingEdit = $state(false);

  const isAdmin = $derived(authStore.hasRole('DG_SHIPPING_ADMIN'));
  const canAddMedical = $derived(authStore.hasRole('DG_SHIPPING_ADMIN', 'COMPANY_ADMIN'));

  onMount(async () => {
    loading = true;
    try {
      [record, medicals, ranks] = await Promise.all([
        api.get<IndosMaster>(`/api/v1/seafarers/indos/${indosId}`),
        api.get<SeafarerMedical[]>(`/api/v1/seafarers/${indosId}/medicals`),
        api.get<RankMaster[]>('/api/v1/seafarers/ranks')
      ]);
    } finally { loading = false; }
  });

  async function saveEdit() {
    savingEdit = true;
    try {
      record = await api.put<IndosMaster>(`/api/v1/seafarers/indos/${indosId}`, editForm);
      editMode = false;
      toast.success('INDoS record updated');
    } finally { savingEdit = false; }
  }

  async function addMedical() {
    savingMed = true;
    try {
      const created = await api.post<SeafarerMedical>(`/api/v1/seafarers/${indosId}/medicals`, medForm);
      medicals = [created, ...medicals];
      showMedModal = false; medForm = {};
      toast.success('Medical record added');
    } finally { savingMed = false; }
  }
</script>

<svelte:head><title>{record?.indosNo ?? 'Seafarer'} — Artemis</title></svelte:head>

{#if loading}
  <div class="flex items-center justify-center py-24"><Loader2 class="size-8 animate-spin text-primary" /></div>
{:else if !record}
  <div class="text-center py-24"><p class="text-muted-foreground">Record not found</p><a href="/seafarers" class="text-primary text-sm mt-2 inline-block hover:underline">← Back to Seafarers</a></div>
{:else}
  <div class="space-y-6 max-w-4xl mx-auto">
    <!-- Breadcrumb -->
    <div class="flex items-center gap-2 text-sm text-muted-foreground">
      <a href="/seafarers" class="hover:text-foreground flex items-center gap-1 transition-colors"><ArrowLeft class="size-3.5" />Seafarers</a>
      <span>/</span><span class="text-foreground font-medium">{record.indosNo}</span>
    </div>

    <!-- Detail card -->
    <div class="rounded-2xl border border-border bg-card overflow-hidden">
      <div class="h-2 bg-gradient-to-r from-primary/60 to-amber-400/40"></div>
      <div class="p-6">
        <div class="flex items-start justify-between flex-wrap gap-4 mb-6">
          <div class="flex items-center gap-4">
            <div class="size-16 rounded-2xl bg-primary/10 flex items-center justify-center">
              <User class="size-8 text-primary" />
            </div>
            <div>
              <h1 class="text-2xl font-bold text-foreground">{record.firstName} {record.middleName ? record.middleName + ' ' : ''}{record.lastName}</h1>
              <p class="font-mono text-primary font-semibold">{record.indosNo}</p>
              <div class="mt-1"><StatusBadge status={record.active ? 'active' : 'inactive'} /></div>
            </div>
          </div>
          {#if isAdmin && !editMode}
            <button onclick={() => { editMode = true; editForm = { ...record!, rankId: record!.rank?.id }; }} class="flex items-center gap-2 px-3 py-2 rounded-lg border border-border bg-secondary/50 text-sm font-medium hover:bg-secondary transition-all">
              <Pencil class="size-3.5" />Edit
            </button>
          {/if}
        </div>

        {#if editMode}
          <div class="grid grid-cols-2 gap-3 border border-border rounded-xl p-4 bg-muted/20">
            {#each [['firstName','First Name','text'],['middleName','Middle Name','text'],['lastName','Last Name','text'],['nationality','Nationality','text'],['dateOfBirth','Date of Birth','date']] as [f, l, t]}
              <div class="space-y-1.5">
                <label class="text-xs font-medium text-foreground">{l}</label>
                <input type={t} bind:value={(editForm as Record<string, string>)[f]} class="w-full px-3 py-2 rounded-lg border border-input bg-background text-sm outline-none focus:ring-2 focus:ring-ring transition-all" />
              </div>
            {/each}
            <div class="space-y-1.5">
              <label class="text-xs font-medium text-foreground">Rank</label>
              <select bind:value={editForm.rankId} class="w-full px-3 py-2 rounded-lg border border-input bg-background text-sm outline-none focus:ring-2 focus:ring-ring transition-all">
                <option value="">No rank</option>
                {#each ranks as r}<option value={r.id}>{r.name}</option>{/each}
              </select>
            </div>
            <div class="col-span-2 flex gap-2 pt-1">
              <button onclick={() => (editMode = false)} class="px-3 py-1.5 rounded-lg border border-border text-xs font-medium hover:bg-accent transition-all">Cancel</button>
              <button onclick={saveEdit} disabled={savingEdit} class="flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-primary text-primary-foreground text-xs font-semibold hover:bg-primary/90 disabled:opacity-60 transition-all">
                {#if savingEdit}<Loader2 class="size-3 animate-spin" />{/if}Save Changes
              </button>
            </div>
          </div>
        {:else}
          <div class="grid grid-cols-2 sm:grid-cols-3 gap-4">
            {#each [['Rank', record.rank?.name ?? '—', 'award'],['Nationality', record.nationality ?? '—', 'globe'],['Date of Birth', record.dateOfBirth ?? '—', 'calendar']] as [label, value]}
              <div class="space-y-1">
                <p class="text-xs font-medium text-muted-foreground uppercase tracking-wide">{label}</p>
                <p class="text-sm font-semibold text-foreground">{value}</p>
              </div>
            {/each}
          </div>
        {/if}
      </div>
    </div>

    <!-- Medicals -->
    <div class="space-y-4">
      <div class="flex items-center justify-between">
        <h2 class="text-lg font-bold text-foreground flex items-center gap-2"><Stethoscope class="size-5 text-primary" />Medical Records</h2>
        {#if canAddMedical}
          <button onclick={() => (showMedModal = true)} class="flex items-center gap-2 px-3 py-2 rounded-lg bg-primary text-primary-foreground text-sm font-semibold hover:bg-primary/90 transition-all">
            <Plus class="size-4" />Add Medical
          </button>
        {/if}
      </div>

      {#if medicals.length === 0}
        <div class="rounded-xl border border-border bg-card p-8 text-center">
          <Stethoscope class="size-10 text-muted-foreground/40 mx-auto mb-2" />
          <p class="text-muted-foreground">No medical records found</p>
        </div>
      {:else}
        <div class="grid gap-3">
          {#each medicals as med}
            <div class="rounded-xl border border-border bg-card p-4 flex items-start gap-4">
              <div class="size-10 rounded-xl bg-blue-500/10 flex items-center justify-center shrink-0">
                <Stethoscope class="size-5 text-blue-500" />
              </div>
              <div class="flex-1 min-w-0">
                <p class="font-semibold text-foreground">{med.medicalType}</p>
                <div class="flex flex-wrap gap-4 mt-1.5 text-sm text-muted-foreground">
                  {#if med.issuedDate}<span class="flex items-center gap-1"><Calendar class="size-3.5" />Issued: {med.issuedDate}</span>{/if}
                  {#if med.expiryDate}<span class="flex items-center gap-1"><Calendar class="size-3.5" />Expires: {med.expiryDate}</span>{/if}
                  {#if med.issuedBy}<span class="flex items-center gap-1"><Globe class="size-3.5" />{med.issuedBy}</span>{/if}
                </div>
                {#if med.remarks}<p class="text-xs text-muted-foreground mt-1 italic">{med.remarks}</p>{/if}
              </div>
            </div>
          {/each}
        </div>
      {/if}
    </div>
  </div>

  <!-- Add Medical Modal -->
  {#if showMedModal}
    <div class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-sm">
      <div class="bg-card rounded-2xl border border-border shadow-2xl w-full max-w-md p-6 space-y-4">
        <div class="flex items-center justify-between">
          <h2 class="text-lg font-bold">Add Medical Record</h2>
          <button onclick={() => (showMedModal = false)}><X class="size-4" /></button>
        </div>
        <div class="space-y-3">
          {#each [['medicalType','Medical Type *','text'],['issuedBy','Issued By','text'],['issuedDate','Issued Date','date'],['expiryDate','Expiry Date','date'],['remarks','Remarks','text']] as [f, l, t]}
            <div class="space-y-1.5">
              <label class="text-sm font-medium text-foreground">{l}</label>
              <input type={t} bind:value={(medForm as Record<string, string>)[f]} class="w-full px-3 py-2 rounded-lg border border-input bg-background text-sm outline-none focus:ring-2 focus:ring-ring transition-all" />
            </div>
          {/each}
        </div>
        <div class="flex gap-3">
          <button onclick={() => (showMedModal = false)} class="flex-1 py-2.5 rounded-lg border border-border text-sm font-medium hover:bg-accent transition-all">Cancel</button>
          <button onclick={addMedical} disabled={savingMed} class="flex-1 flex items-center justify-center gap-2 py-2.5 rounded-lg bg-primary text-primary-foreground text-sm font-semibold hover:bg-primary/90 disabled:opacity-60 transition-all">
            {#if savingMed}<Loader2 class="size-4 animate-spin" />Saving…{:else}Add Record{/if}
          </button>
        </div>
      </div>
    </div>
  {/if}
{/if}
