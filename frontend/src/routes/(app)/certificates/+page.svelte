<script lang="ts">
  import { onMount } from 'svelte';
  import { api } from '$lib/api';
  import type { Certificate, CertificateStatus } from '$lib/types';
  import { authStore } from '$lib/stores/auth.svelte';
  import StatusBadge from '$lib/components/shared/StatusBadge.svelte';
  import { Award, Loader2, ArrowRight } from '@lucide/svelte';

  const tabs: { label: string; status: CertificateStatus }[] = [
    { label: 'Initiated', status: 'INITIATED' },
    { label: 'L1 Reviewed', status: 'L1_REVIEWED' },
    { label: 'L2 Approved', status: 'L2_APPROVED' },
    { label: 'Allotted', status: 'ALLOTTED' }
  ];

  let activeTab = $state<CertificateStatus>('INITIATED');
  let certs = $state<Certificate[]>([]);
  let loading = $state(true);

  async function load(status: CertificateStatus) {
    loading = true;
    try { certs = await api.get<Certificate[]>('/api/v1/certificates', { status }); }
    finally { loading = false; }
  }

  onMount(() => load(activeTab));

  function switchTab(status: CertificateStatus) {
    activeTab = status;
    load(status);
  }
</script>

<svelte:head><title>Certificates — Artemis</title></svelte:head>

<div class="space-y-6 max-w-7xl mx-auto">
  <div>
    <h1 class="text-2xl font-bold text-foreground">Certificate Queue</h1>
    <p class="text-sm text-muted-foreground">Manage and review seafarer Continuous Discharge Certificate (CDC) requests</p>
  </div>

  <!-- Tabs -->
  <div class="flex gap-1 p-1 bg-muted rounded-xl w-fit">
    {#each tabs as tab}
      <button
        onclick={() => switchTab(tab.status)}
        class={['px-4 py-2 rounded-lg text-sm font-medium transition-all', activeTab === tab.status ? 'bg-card text-foreground shadow-sm' : 'text-muted-foreground hover:text-foreground'].join(' ')}
      >{tab.label}</button>
    {/each}
  </div>

  <!-- Table -->
  <div class="rounded-xl border border-border bg-card overflow-hidden">
    <table class="w-full text-sm">
      <thead><tr class="border-b border-border bg-muted/40">
        {#each ['Seafarer','Vessel','Enqueued','Status',''] as h}
          <th class="text-left px-4 py-3 text-xs font-semibold text-muted-foreground uppercase tracking-wide">{h}</th>
        {/each}
      </tr></thead>
      <tbody>
        {#if loading}
          {#each Array(5) as _}<tr class="border-b border-border">{#each Array(5) as _}<td class="px-4 py-3"><div class="h-4 rounded bg-muted animate-pulse"></div></td>{/each}</tr>{/each}
        {:else if certs.length === 0}
          <tr><td colspan="5" class="px-4 py-12 text-center">
            <Award class="size-10 text-muted-foreground/30 mx-auto mb-2" />
            <p class="text-muted-foreground">No certificates in the <strong>{tabs.find(t => t.status === activeTab)?.label}</strong> queue</p>
          </td></tr>
        {:else}
          {#each certs as cert}
            <tr class="border-b border-border last:border-0 hover:bg-accent/20 transition-colors">
              <td class="px-4 py-3 font-medium text-foreground">{cert.contract?.indosMaster?.firstName} {cert.contract?.indosMaster?.lastName}</td>
              <td class="px-4 py-3 text-muted-foreground">{cert.contract?.vessel?.name ?? '—'}</td>
              <td class="px-4 py-3 text-muted-foreground text-xs">{cert.enqueuedAt ? new Date(cert.enqueuedAt).toLocaleDateString() : '—'}</td>
              <td class="px-4 py-3"><StatusBadge status={cert.status} /></td>
              <td class="px-4 py-3 text-right"><a href="/certificates/{cert.id}" class="px-3 py-1.5 rounded-lg bg-primary/10 text-primary text-xs font-semibold hover:bg-primary/20 transition-colors">Review</a></td>
            </tr>
          {/each}
        {/if}
      </tbody>
    </table>
  </div>
</div>
