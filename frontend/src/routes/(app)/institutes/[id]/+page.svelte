<script lang="ts">
  import { onMount } from 'svelte';
  import { page } from '$app/state';
  import { api } from '$lib/api';
  import type { Institute, PreSeaCourse } from '$lib/types';
  import { authStore } from '$lib/stores/auth.svelte';
  import { GraduationCap, Plus, ArrowLeft, Pencil, Loader2, X } from '@lucide/svelte';
  import { toast } from 'svelte-sonner';

  const instId = $derived(page.params.id);
  let institute = $state<Institute | null>(null);
  let courses = $state<PreSeaCourse[]>([]);
  let loading = $state(true);
  let editMode = $state(false);
  let editForm = $state<Partial<Institute>>({});
  let showAddCourse = $state(false);
  let courseForm = $state<Partial<PreSeaCourse & { durationWeeks: number; fee: number }>>({});
  let saving = $state(false);
  let approvingId = $state<string | null>(null);
  let quotaForm = $state<{ permittedCapacity: number }>({ permittedCapacity: 0 });

  const isAdmin = $derived(authStore.hasRole('DG_SHIPPING_ADMIN'));
  const isInstAdmin = $derived(authStore.hasRole('DG_SHIPPING_ADMIN', 'INSTITUTE_ADMIN'));

  onMount(async () => {
    loading = true;
    try {
      [institute, courses] = await Promise.all([
        api.get<Institute>(`/api/v1/institutes/${instId}`),
        api.get<PreSeaCourse[]>(`/api/v1/institutes/${instId}/courses`)
      ]);
    } finally { loading = false; }
  });

  async function updateInstitute() {
    saving = true;
    try {
      institute = await api.put<Institute>(`/api/v1/institutes/${instId}`, editForm);
      editMode = false; toast.success('Institute updated');
    } finally { saving = false; }
  }

  async function addCourse() {
    saving = true;
    try {
      const created = await api.post<PreSeaCourse>(`/api/v1/institutes/${instId}/courses`, courseForm);
      courses = [...courses, created];
      showAddCourse = false; courseForm = {};
      toast.success('Course added');
    } finally { saving = false; }
  }

  async function setQuota(courseId: string) {
    approvingId = courseId;
    try {
      const updated = await api.put<PreSeaCourse>(`/api/v1/institutes/courses/${courseId}/quota`, undefined, {
        permittedCapacity: quotaForm.permittedCapacity
      });
      courses = courses.map((c) => c.id === courseId ? updated : c);
      approvingId = null;
      toast.success('Quota updated');
    } finally { if (approvingId === courseId) approvingId = null; }
  }
</script>

<svelte:head><title>{institute?.name ?? 'Institute'} — Artemis</title></svelte:head>

{#if loading}
  <div class="flex items-center justify-center py-24"><Loader2 class="size-8 animate-spin text-primary" /></div>
{:else if !institute}
  <p class="text-center text-muted-foreground py-16">Institute not found</p>
{:else}
  <div class="space-y-6 max-w-5xl mx-auto">
    <div class="flex items-center gap-2 text-sm text-muted-foreground">
      <a href="/institutes" class="hover:text-foreground flex items-center gap-1 transition-colors"><ArrowLeft class="size-3.5" />Institutes</a>
      <span>/</span><span class="text-foreground font-medium">{institute.name}</span>
    </div>

    <!-- Institute card -->
    <div class="rounded-2xl border border-border bg-card overflow-hidden">
      <div class="h-2 bg-gradient-to-r from-emerald-500/50 to-teal-400/30"></div>
      <div class="p-6">
        <div class="flex items-start justify-between flex-wrap gap-4 mb-4">
          <div class="flex items-center gap-4">
            <div class="size-14 rounded-2xl bg-emerald-500/10 flex items-center justify-center"><GraduationCap class="size-7 text-emerald-500" /></div>
            <div>
              <h1 class="text-2xl font-bold text-foreground">{institute.name}</h1>
              {#if institute.code}<p class="font-mono text-sm text-muted-foreground">{institute.code}</p>{/if}
              {#if institute.accreditationStatus}<span class="text-xs px-2 py-0.5 rounded-full bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 font-medium mt-1 inline-block">{institute.accreditationStatus}</span>{/if}
            </div>
          </div>
          {#if isInstAdmin && !editMode}
            <button onclick={() => { editMode = true; editForm = { ...institute! }; }} class="flex items-center gap-2 px-3 py-2 rounded-lg border border-border text-sm font-medium hover:bg-accent transition-all">
              <Pencil class="size-3.5" />Edit
            </button>
          {/if}
        </div>

        {#if editMode}
          <div class="grid grid-cols-2 gap-3 border border-border rounded-xl p-4 bg-muted/20">
            {#each [['name','Name *','text'],['code','Code','text'],['address','Address','text'],['contactEmail','Email','email'],['contactPhone','Phone','tel'],['accreditationStatus','Accreditation Status','text']] as [f, l, t]}
              <div class="space-y-1.5">
                <label class="text-xs font-medium text-foreground">{l}</label>
                <input type={t} bind:value={(editForm as Record<string, string>)[f]} class="w-full px-3 py-2 rounded-lg border border-input bg-background text-sm outline-none focus:ring-2 focus:ring-ring transition-all" />
              </div>
            {/each}
            <div class="col-span-2 flex gap-2 pt-1">
              <button onclick={() => (editMode = false)} class="px-3 py-1.5 rounded-lg border border-border text-xs font-medium hover:bg-accent transition-all">Cancel</button>
              <button onclick={updateInstitute} disabled={saving} class="flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-primary text-primary-foreground text-xs font-semibold hover:bg-primary/90 disabled:opacity-60 transition-all">
                {#if saving}<Loader2 class="size-3 animate-spin" />{/if}Save
              </button>
            </div>
          </div>
        {:else}
          <div class="grid grid-cols-2 sm:grid-cols-3 gap-4 text-sm">
            {#each [['Email', institute.contactEmail],['Phone', institute.contactPhone],['Address', institute.address]] as [l, v]}
              {#if v}<div class="space-y-0.5"><p class="text-xs text-muted-foreground uppercase tracking-wide font-medium">{l}</p><p class="font-medium text-foreground">{v}</p></div>{/if}
            {/each}
          </div>
        {/if}
      </div>
    </div>

    <!-- Courses -->
    <div class="space-y-4">
      <div class="flex items-center justify-between">
        <h2 class="text-lg font-bold text-foreground">Pre-Sea Courses</h2>
        {#if isInstAdmin}
          <button onclick={() => (showAddCourse = true)} class="flex items-center gap-2 px-3 py-2 rounded-lg bg-primary text-primary-foreground text-sm font-semibold hover:bg-primary/90 transition-all">
            <Plus class="size-4" />Add Course
          </button>
        {/if}
      </div>

      {#if courses.length === 0}
        <div class="rounded-xl border border-border bg-card p-8 text-center"><GraduationCap class="size-10 text-muted-foreground/30 mx-auto mb-2" /><p class="text-muted-foreground">No courses yet</p></div>
      {:else}
        <div class="rounded-xl border border-border bg-card overflow-hidden">
          <table class="w-full text-sm">
            <thead><tr class="border-b border-border bg-muted/40">
              {#each ['Course','Code','Duration','Capacity','Fee','Status',''] as h}
                <th class="text-left px-4 py-3 text-xs font-semibold text-muted-foreground uppercase tracking-wide">{h}</th>
              {/each}
            </tr></thead>
            <tbody>
              {#each courses as c}
                <tr class="border-b border-border last:border-0 hover:bg-accent/20 transition-colors">
                  <td class="px-4 py-3 font-medium text-foreground">{c.name}</td>
                  <td class="px-4 py-3 font-mono text-xs text-muted-foreground">{c.code ?? '—'}</td>
                  <td class="px-4 py-3 text-muted-foreground">{c.durationWeeks ? `${c.durationWeeks}w` : '—'}</td>
                  <td class="px-4 py-3 text-muted-foreground">{c.permittedCapacity ?? '—'}</td>
                  <td class="px-4 py-3 text-muted-foreground">{c.fee ? `₹${c.fee.toLocaleString()}` : '—'}</td>
                  <td class="px-4 py-3 text-muted-foreground text-xs">{c.status ?? '—'}</td>
                  <td class="px-4 py-3">
                    <div class="flex items-center gap-2 justify-end">
                      {#if isAdmin}
                        <div class="flex items-center gap-1">
                          <input type="number" min="0" placeholder="Quota" class="w-16 px-2 py-1 rounded border border-input bg-background text-xs outline-none focus:ring-1 focus:ring-ring" oninput={(e) => (quotaForm.permittedCapacity = +(e.target as HTMLInputElement).value)} />
                          <button onclick={() => setQuota(c.id)} disabled={approvingId === c.id} class="px-2 py-1 rounded bg-primary/10 text-primary text-xs font-semibold hover:bg-primary/20 transition-colors disabled:opacity-50">
                            {#if approvingId === c.id}<Loader2 class="size-3 animate-spin" />{:else}Set{/if}
                          </button>
                        </div>
                      {/if}
                      <a href="/institutes/{instId}/courses/{c.id}" class="px-3 py-1.5 rounded-lg bg-primary/10 text-primary text-xs font-semibold hover:bg-primary/20 transition-colors whitespace-nowrap">Enrollments</a>
                    </div>
                  </td>
                </tr>
              {/each}
            </tbody>
          </table>
        </div>
      {/if}
    </div>
  </div>

  {#if showAddCourse}
    <div class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-sm">
      <div class="bg-card rounded-2xl border border-border shadow-2xl w-full max-w-md p-6 space-y-4">
        <div class="flex items-center justify-between"><h2 class="text-lg font-bold">Add Course</h2><button onclick={() => (showAddCourse = false)}><X class="size-4" /></button></div>
        <div class="grid grid-cols-2 gap-3">
          {#each [['name','Course Name *','text'],['code','Code','text'],['durationWeeks','Duration (weeks)','number'],['fee','Fee (₹)','number'],['status','Status','text']] as [f, l, t]}
            <div class="space-y-1.5 {f === 'name' ? 'col-span-2' : ''}">
              <label class="text-sm font-medium text-foreground">{l}</label>
              <input type={t} bind:value={(courseForm as Record<string, string | number>)[f]} class="w-full px-3 py-2 rounded-lg border border-input bg-background text-sm outline-none focus:ring-2 focus:ring-ring transition-all" />
            </div>
          {/each}
        </div>
        <div class="flex gap-3">
          <button onclick={() => (showAddCourse = false)} class="flex-1 py-2.5 rounded-lg border border-border text-sm font-medium hover:bg-accent transition-all">Cancel</button>
          <button onclick={addCourse} disabled={saving || !courseForm.name} class="flex-1 flex items-center justify-center gap-2 py-2.5 rounded-lg bg-primary text-primary-foreground text-sm font-semibold hover:bg-primary/90 disabled:opacity-60 transition-all">
            {#if saving}<Loader2 class="size-4 animate-spin" />Saving…{:else}Add Course{/if}
          </button>
        </div>
      </div>
    </div>
  {/if}
{/if}
