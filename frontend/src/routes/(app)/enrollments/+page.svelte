<script lang="ts">
  import { onMount } from 'svelte';
  import { api } from '$lib/api';
  import type { Enrollment } from '$lib/types';
  import { authStore } from '$lib/stores/auth.svelte';
  import StatusBadge from '$lib/components/shared/StatusBadge.svelte';
  import { BookOpen, Loader2 } from '@lucide/svelte';

  let enrollments = $state<Enrollment[]>([]);
  let loading = $state(true);

  onMount(async () => {
    loading = true;
    try {
      const indosMapping = await api.get<{ indosMaster: { id: string } }>('/api/v1/seafarers/link').catch(() => null);
      if (indosMapping?.indosMaster?.id) {
        enrollments = await api.get<Enrollment[]>(`/api/v1/institutes/candidates/${indosMapping.indosMaster.id}/enrollments`);
      }
    } finally { loading = false; }
  });
</script>

<svelte:head><title>My Enrollments — Artemis</title></svelte:head>

<div class="space-y-6 max-w-5xl mx-auto">
  <div>
    <h1 class="text-2xl font-bold text-foreground">My Enrollments</h1>
    <p class="text-sm text-muted-foreground">Your training course history and status</p>
  </div>

  {#if loading}
    <div class="space-y-3">
      {#each Array(4) as _}<div class="rounded-xl border border-border bg-card p-4 animate-pulse h-20"></div>{/each}
    </div>
  {:else if enrollments.length === 0}
    <div class="rounded-2xl border border-border bg-card p-16 text-center">
      <BookOpen class="size-14 text-muted-foreground/30 mx-auto mb-4" />
      <h2 class="font-bold text-foreground text-lg mb-2">No Enrollments Yet</h2>
      <p class="text-muted-foreground text-sm">Browse institutes to find and enroll in pre-sea training courses.</p>
      <a href="/institutes" class="inline-block mt-4 px-4 py-2 rounded-lg bg-primary text-primary-foreground text-sm font-semibold hover:bg-primary/90 transition-all">Browse Institutes</a>
    </div>
  {:else}
    <div class="space-y-3">
      {#each enrollments as e}
        <div class="rounded-xl border border-border bg-card p-5 flex items-center gap-4 hover:shadow-sm transition-all">
          <div class="size-12 rounded-xl flex items-center justify-center shrink-0 {e.status === 'COMPLETED' ? 'bg-emerald-500/10' : e.status === 'IN_PROGRESS' ? 'bg-amber-500/10' : 'bg-blue-500/10'}">
            <BookOpen class="size-6 {e.status === 'COMPLETED' ? 'text-emerald-500' : e.status === 'IN_PROGRESS' ? 'text-amber-500' : 'text-blue-500'}" />
          </div>
          <div class="flex-1 min-w-0">
            <p class="font-bold text-foreground">{e.course?.name ?? 'Course'}</p>
            <p class="text-sm text-muted-foreground">{e.course?.institute?.name ?? '—'}</p>
            <div class="flex items-center gap-3 mt-1 text-xs text-muted-foreground">
              {#if e.enrolledAt}<span>Enrolled {new Date(e.enrolledAt).toLocaleDateString()}</span>{/if}
              {#if e.completedAt}<span>Completed {new Date(e.completedAt).toLocaleDateString()}</span>{/if}
              {#if e.course?.durationWeeks}<span>{e.course.durationWeeks} weeks</span>{/if}
            </div>
          </div>
          <div class="shrink-0">
            <StatusBadge status={e.status} />
          </div>
          {#if e.status === 'COMPLETED'}
            <a href="/institutes/{e.course?.institute?.id}/courses/{e.course?.id}" class="px-3 py-1.5 rounded-lg bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 text-xs font-semibold hover:bg-emerald-500/20 transition-colors whitespace-nowrap">View Cert</a>
          {/if}
        </div>
      {/each}
    </div>
  {/if}
</div>
