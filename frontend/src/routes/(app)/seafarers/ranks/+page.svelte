<script lang="ts">
  import { onMount } from 'svelte';
  import { api } from '$lib/api';
  import type { RankMaster } from '$lib/types';
  import { authStore } from '$lib/stores/auth.svelte';
  import { Plus, Pencil, Trash2, Loader2, X, Check, Award } from '@lucide/svelte';
  import { toast } from 'svelte-sonner';

  let ranks = $state<RankMaster[]>([]);
  let loading = $state(true);
  let editingId = $state<string | null>(null);
  let editForm = $state<Partial<RankMaster>>({});
  let showAdd = $state(false);
  let addForm = $state<Partial<RankMaster>>({});
  let saving = $state(false);
  let deleting = $state<string | null>(null);

  const isAdmin = $derived(authStore.hasRole('DG_SHIPPING_ADMIN'));

  onMount(async () => {
    try { ranks = await api.get<RankMaster[]>('/api/v1/seafarers/ranks'); }
    finally { loading = false; }
  });

  async function addRank() {
    saving = true;
    try {
      const created = await api.post<RankMaster>('/api/v1/seafarers/ranks', addForm);
      ranks = [...ranks, created];
      addForm = {}; showAdd = false;
      toast.success('Rank created');
    } finally { saving = false; }
  }

  async function updateRank(id: string) {
    saving = true;
    try {
      const updated = await api.put<RankMaster>(`/api/v1/seafarers/ranks/${id}`, editForm);
      ranks = ranks.map((r) => r.id === id ? updated : r);
      editingId = null;
      toast.success('Rank updated');
    } finally { saving = false; }
  }

  async function deleteRank(id: string) {
    deleting = id;
    try {
      await api.delete(`/api/v1/seafarers/ranks/${id}`);
      ranks = ranks.filter((r) => r.id !== id);
      toast.success('Rank deleted');
    } finally { deleting = null; }
  }
</script>

<svelte:head><title>Rank Master — Artemis</title></svelte:head>

<div class="space-y-6 max-w-3xl mx-auto">
  <div class="flex items-center justify-between">
    <div>
      <div class="flex items-center gap-2 text-sm text-muted-foreground mb-1">
        <a href="/seafarers" class="hover:text-foreground transition-colors">Seafarers</a>
        <span>/</span><span class="text-foreground font-medium">Rank Master</span>
      </div>
      <h1 class="text-2xl font-bold text-foreground">Rank Master</h1>
    </div>
    {#if isAdmin}
      <button onclick={() => (showAdd = true)} class="flex items-center gap-2 px-4 py-2 rounded-lg bg-primary text-primary-foreground text-sm font-semibold hover:bg-primary/90 transition-all shadow-sm">
        <Plus class="size-4" />Add Rank
      </button>
    {/if}
  </div>

  {#if showAdd}
    <div class="rounded-xl border border-primary/30 bg-primary/5 p-4 space-y-3">
      <h3 class="font-semibold text-foreground text-sm">New Rank</h3>
      <div class="grid grid-cols-3 gap-3">
        {#each [['name','Name *'],['code','Code'],['category','Category']] as [f, l]}
          <div class="space-y-1">
            <label class="text-xs font-medium text-foreground">{l}</label>
            <input type="text" bind:value={(addForm as Record<string, string>)[f]} class="w-full px-3 py-2 rounded-lg border border-input bg-background text-sm outline-none focus:ring-2 focus:ring-ring transition-all" />
          </div>
        {/each}
      </div>
      <div class="flex gap-2">
        <button onclick={() => (showAdd = false)} class="px-3 py-1.5 rounded-lg border border-border text-xs font-medium hover:bg-accent transition-all">Cancel</button>
        <button onclick={addRank} disabled={saving || !addForm.name} class="flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-primary text-primary-foreground text-xs font-semibold hover:bg-primary/90 disabled:opacity-60 transition-all">
          {#if saving}<Loader2 class="size-3 animate-spin" />{:else}<Check class="size-3" />{/if}Save
        </button>
      </div>
    </div>
  {/if}

  <div class="rounded-xl border border-border bg-card overflow-hidden">
    <table class="w-full text-sm">
      <thead>
        <tr class="border-b border-border bg-muted/40">
          <th class="text-left px-4 py-3 font-semibold text-muted-foreground text-xs uppercase tracking-wide">Name</th>
          <th class="text-left px-4 py-3 font-semibold text-muted-foreground text-xs uppercase tracking-wide">Code</th>
          <th class="text-left px-4 py-3 font-semibold text-muted-foreground text-xs uppercase tracking-wide">Category</th>
          {#if isAdmin}<th class="px-4 py-3"></th>{/if}
        </tr>
      </thead>
      <tbody>
        {#if loading}
          {#each Array(5) as _}
            <tr class="border-b border-border"><td colspan="4" class="px-4 py-3"><div class="h-4 rounded bg-muted animate-pulse"></div></td></tr>
          {/each}
        {:else if ranks.length === 0}
          <tr><td colspan="4" class="px-4 py-12 text-center">
            <Award class="size-10 text-muted-foreground/40 mx-auto mb-2" />
            <p class="text-muted-foreground">No ranks defined yet</p>
          </td></tr>
        {:else}
          {#each ranks as rank}
            <tr class="border-b border-border last:border-0 hover:bg-accent/20 transition-colors">
              {#if editingId === rank.id}
                <td class="px-4 py-2"><input type="text" bind:value={editForm.name} class="w-full px-2 py-1.5 rounded border border-input bg-background text-sm outline-none focus:ring-2 focus:ring-ring" /></td>
                <td class="px-4 py-2"><input type="text" bind:value={editForm.code} class="w-full px-2 py-1.5 rounded border border-input bg-background text-sm outline-none focus:ring-2 focus:ring-ring" /></td>
                <td class="px-4 py-2"><input type="text" bind:value={editForm.category} class="w-full px-2 py-1.5 rounded border border-input bg-background text-sm outline-none focus:ring-2 focus:ring-ring" /></td>
                <td class="px-4 py-2">
                  <div class="flex items-center gap-1.5 justify-end">
                    <button onclick={() => (editingId = null)} class="p-1.5 rounded hover:bg-accent transition-colors"><X class="size-3.5 text-muted-foreground" /></button>
                    <button onclick={() => updateRank(rank.id)} disabled={saving} class="p-1.5 rounded bg-primary/10 hover:bg-primary/20 transition-colors"><Check class="size-3.5 text-primary" /></button>
                  </div>
                </td>
              {:else}
                <td class="px-4 py-3 font-medium text-foreground">{rank.name}</td>
                <td class="px-4 py-3 font-mono text-muted-foreground text-xs">{rank.code ?? '—'}</td>
                <td class="px-4 py-3 text-muted-foreground">{rank.category ?? '—'}</td>
                {#if isAdmin}
                  <td class="px-4 py-3">
                    <div class="flex items-center gap-1.5 justify-end">
                      <button onclick={() => { editingId = rank.id; editForm = { ...rank }; }} class="p-1.5 rounded-lg hover:bg-accent transition-colors text-muted-foreground"><Pencil class="size-3.5" /></button>
                      <button onclick={() => deleteRank(rank.id)} disabled={deleting === rank.id} class="p-1.5 rounded-lg hover:bg-destructive/10 transition-colors text-muted-foreground hover:text-destructive disabled:opacity-50">
                        {#if deleting === rank.id}<Loader2 class="size-3.5 animate-spin" />{:else}<Trash2 class="size-3.5" />{/if}
                      </button>
                    </div>
                  </td>
                {/if}
              {/if}
            </tr>
          {/each}
        {/if}
      </tbody>
    </table>
  </div>
</div>
