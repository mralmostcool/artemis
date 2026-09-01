<script lang="ts">
  import { onMount } from 'svelte';
  import { page } from '$app/state';
  import { api } from '$lib/api';
  import type { Enrollment, PreSeaCourse, IndosMaster } from '$lib/types';
  import { authStore } from '$lib/stores/auth.svelte';
  import StatusBadge from '$lib/components/shared/StatusBadge.svelte';
  import { BookOpen, Plus, ArrowLeft, Loader2, X } from '@lucide/svelte';
  import { toast } from 'svelte-sonner';

  const instId = $derived(page.params.id);
  const courseId = $derived(page.params.courseId);
  let course = $state<PreSeaCourse | null>(null);
  let enrollments = $state<Enrollment[]>([]);
  let loading = $state(true);
  let showEnroll = $state(false);
  let enrollForm = $state({ indosId: '' });
  let saving = $state(false);
  let updatingId = $state<string | null>(null);

  const canManage = $derived(authStore.hasRole('DG_SHIPPING_ADMIN', 'INSTITUTE_ADMIN', 'INSTITUTE_USER'));
  const isAdmin = $derived(authStore.hasRole('DG_SHIPPING_ADMIN'));

  const statusOptions = ['ENROLLED', 'IN_PROGRESS', 'COMPLETED', 'DROPPED'] as const;

  onMount(async () => {
    loading = true;
    try {
      [course, enrollments] = await Promise.all([
        api.get<PreSeaCourse>(`/api/v1/institutes/${instId}/courses/${courseId}`),
        api.get<Enrollment[]>(`/api/v1/institutes/${instId}/courses/${courseId}/enrollments`)
      ]);
    } finally { loading = false; }
  });

  async function enroll() {
    saving = true;
    try {
      const created = await api.post<Enrollment>(`/api/v1/institutes/${instId}/courses/${courseId}/enrollments`, {
        indosMaster: { id: enrollForm.indosId }
      });
      enrollments = [...enrollments, created];
      showEnroll = false; enrollForm.indosId = '';
      toast.success('Seafarer enrolled');
    } finally { saving = false; }
  }

  async function updateStatus(enrollment: Enrollment, status: string) {
    updatingId = enrollment.id;
    try {
      const updated = await api.put<Enrollment>(`/api/v1/institutes/enrollments/${enrollment.id}/status`, undefined, { status });
      enrollments = enrollments.map((e) => e.id === enrollment.id ? updated : e);
    } finally { updatingId = null; }
  }

  async function generateCertificate(enrollmentId: string) {
    updatingId = enrollmentId;
    try {
      await api.post(`/api/v1/institutes/enrollments/${enrollmentId}/certificate`);
      toast.success('Certificate generated');
    } finally { updatingId = null; }
  }
</script>

<svelte:head><title>{course?.name ?? 'Course'} Enrollments — Artemis</title></svelte:head>

{#if loading}
  <div class="flex items-center justify-center py-24"><Loader2 class="size-8 animate-spin text-primary" /></div>
{:else}
  <div class="space-y-6 max-w-6xl mx-auto">
    <div class="flex items-center gap-2 text-sm text-muted-foreground">
      <a href="/institutes" class="hover:text-foreground transition-colors">Institutes</a>
      <span>/</span><a href="/institutes/{instId}" class="hover:text-foreground transition-colors">{course?.institute?.name ?? instId}</a>
      <span>/</span><span class="text-foreground font-medium">{course?.name}</span>
    </div>

    <div class="flex items-center justify-between flex-wrap gap-3">
      <div>
        <h1 class="text-2xl font-bold text-foreground">{course?.name}</h1>
        <div class="flex gap-3 mt-1 text-sm text-muted-foreground">
          {#if course?.code}<span class="font-mono">{course.code}</span>{/if}
          {#if course?.durationWeeks}<span>Duration: {course.durationWeeks} weeks</span>{/if}
          {#if course?.permittedCapacity}<span>Capacity: {enrollments.length}/{course.permittedCapacity}</span>{/if}
          {#if course?.fee}<span>Fee: ₹{course.fee.toLocaleString()}</span>{/if}
        </div>
      </div>
      {#if canManage}
        <button onclick={() => (showEnroll = true)} class="flex items-center gap-2 px-4 py-2 rounded-lg bg-primary text-primary-foreground text-sm font-semibold hover:bg-primary/90 transition-all shadow-sm">
          <Plus class="size-4" />Enroll Seafarer
        </button>
      {/if}
    </div>

    <!-- Progress bar -->
    {#if course?.permittedCapacity}
      {@const pct = Math.min(100, (enrollments.length / course.permittedCapacity) * 100)}
      <div class="rounded-xl border border-border bg-card p-4">
        <div class="flex items-center justify-between mb-2 text-sm">
          <span class="font-medium text-foreground">Enrollment Capacity</span>
          <span class="text-muted-foreground">{enrollments.length} / {course.permittedCapacity}</span>
        </div>
        <div class="h-2 rounded-full bg-muted overflow-hidden">
          <div class="h-2 rounded-full bg-primary transition-all duration-500" style="width:{pct}%"></div>
        </div>
      </div>
    {/if}

    <!-- Enrollments table -->
    <div class="rounded-xl border border-border bg-card overflow-hidden">
      <table class="w-full text-sm">
        <thead><tr class="border-b border-border bg-muted/40">
          {#each ['Seafarer','INDoS No.','Enrolled','Status',''] as h}
            <th class="text-left px-4 py-3 text-xs font-semibold text-muted-foreground uppercase tracking-wide">{h}</th>
          {/each}
        </tr></thead>
        <tbody>
          {#if enrollments.length === 0}
            <tr><td colspan="5" class="px-4 py-12 text-center">
              <BookOpen class="size-10 text-muted-foreground/30 mx-auto mb-2" />
              <p class="text-muted-foreground">No enrollments yet</p>
            </td></tr>
          {:else}
            {#each enrollments as e}
              <tr class="border-b border-border last:border-0 hover:bg-accent/20 transition-colors">
                <td class="px-4 py-3 font-medium text-foreground">{e.indosMaster?.firstName} {e.indosMaster?.lastName}</td>
                <td class="px-4 py-3 font-mono text-xs text-muted-foreground">{e.indosMaster?.indosNo ?? '—'}</td>
                <td class="px-4 py-3 text-muted-foreground text-xs">{e.enrolledAt ? new Date(e.enrolledAt).toLocaleDateString() : '—'}</td>
                <td class="px-4 py-3"><StatusBadge status={e.status} /></td>
                <td class="px-4 py-3">
                  <div class="flex items-center gap-2 justify-end">
                    {#if canManage}
                      <select
                        value={e.status}
                        onchange={(ev) => updateStatus(e, (ev.target as HTMLSelectElement).value)}
                        disabled={updatingId === e.id}
                        class="px-2 py-1 rounded-lg border border-input bg-background text-xs outline-none focus:ring-1 focus:ring-ring disabled:opacity-50"
                      >
                        {#each statusOptions as s}<option value={s}>{s}</option>{/each}
                      </select>
                    {/if}
                    {#if isAdmin && e.status === 'COMPLETED'}
                      <button onclick={() => generateCertificate(e.id)} disabled={updatingId === e.id} class="px-2 py-1 rounded-lg bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 text-xs font-semibold hover:bg-emerald-500/20 transition-colors disabled:opacity-50 whitespace-nowrap">
                        {#if updatingId === e.id}<Loader2 class="size-3 animate-spin" />{:else}Gen. Cert{/if}
                      </button>
                    {/if}
                  </div>
                </td>
              </tr>
            {/each}
          {/if}
        </tbody>
      </table>
    </div>
  </div>

  {#if showEnroll}
    <div class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-sm">
      <div class="bg-card rounded-2xl border border-border shadow-2xl w-full max-w-sm p-6 space-y-4">
        <div class="flex items-center justify-between"><h2 class="text-lg font-bold">Enroll Seafarer</h2><button onclick={() => (showEnroll = false)}><X class="size-4" /></button></div>
        <div class="space-y-1.5">
          <label class="text-sm font-medium text-foreground">INDoS ID (UUID) *</label>
          <input type="text" bind:value={enrollForm.indosId} placeholder="Enter seafarer INDoS UUID" class="w-full px-3 py-2 rounded-lg border border-input bg-background text-sm outline-none focus:ring-2 focus:ring-ring transition-all font-mono" />
        </div>
        <div class="flex gap-3">
          <button onclick={() => (showEnroll = false)} class="flex-1 py-2.5 rounded-lg border border-border text-sm font-medium hover:bg-accent transition-all">Cancel</button>
          <button onclick={enroll} disabled={saving || !enrollForm.indosId.trim()} class="flex-1 flex items-center justify-center gap-2 py-2.5 rounded-lg bg-primary text-primary-foreground text-sm font-semibold hover:bg-primary/90 disabled:opacity-60 transition-all">
            {#if saving}<Loader2 class="size-4 animate-spin" />Enrolling…{:else}Enroll{/if}
          </button>
        </div>
      </div>
    </div>
  {/if}
{/if}
