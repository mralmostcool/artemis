<script lang="ts">
  import { onMount } from 'svelte';
  import { page } from '$app/state';
  import { api } from '$lib/api';
  import type { Certificate } from '$lib/types';
  import { authStore } from '$lib/stores/auth.svelte';
  import StatusBadge from '$lib/components/shared/StatusBadge.svelte';
  import { ArrowLeft, Award, CheckCircle2, ShieldCheck, Tag, Loader2 } from '@lucide/svelte';
  import { toast } from 'svelte-sonner';

  const certId = $derived(page.params.id);
  let cert = $state<Certificate | null>(null);
  let loading = $state(true);
  let acting = $state(false);

  let showL1 = $state(false);
  let showL2 = $state(false);
  let showAllot = $state(false);
  let l1Remarks = $state('');
  let l2Remarks = $state('');
  let allotForm = $state({ companyId: '', certificateNo: '', expiryDate: '' });

  const canL1 = $derived(authStore.hasRole('DG_SHIPPING_ADMIN', 'DG_SHIPPING_L1'));
  const canL2 = $derived(authStore.hasRole('DG_SHIPPING_ADMIN', 'DG_SHIPPING_L2'));
  const canAllot = $derived(authStore.hasRole('DG_SHIPPING_ADMIN', 'COMPANY_ADMIN'));

  onMount(async () => {
    loading = true;
    try { cert = await api.get<Certificate>(`/api/v1/certificates/${certId}`); }
    finally { loading = false; }
  });

  async function reviewL1() {
    acting = true;
    try {
      cert = await api.post<Certificate>(`/api/v1/certificates/${certId}/review/l1`, undefined, { remarks: l1Remarks });
      showL1 = false; l1Remarks = ''; toast.success('L1 review submitted');
    } finally { acting = false; }
  }

  async function reviewL2() {
    acting = true;
    try {
      cert = await api.post<Certificate>(`/api/v1/certificates/${certId}/review/l2`, undefined, { remarks: l2Remarks });
      showL2 = false; l2Remarks = ''; toast.success('L2 approval submitted');
    } finally { acting = false; }
  }

  async function allot() {
    acting = true;
    try {
      cert = await api.post<Certificate>(`/api/v1/certificates/${certId}/allot`, undefined, {
        companyId: allotForm.companyId,
        certificateNo: allotForm.certificateNo,
        expiryDate: allotForm.expiryDate
      });
      showAllot = false; toast.success('Certificate allotted');
    } finally { acting = false; }
  }

  function fmt(d?: string) { return d ? new Date(d).toLocaleString() : '—'; }
</script>

<svelte:head><title>Certificate Review — Artemis</title></svelte:head>

{#if loading}
  <div class="flex items-center justify-center py-24"><Loader2 class="size-8 animate-spin text-primary" /></div>
{:else if !cert}
  <p class="text-center text-muted-foreground py-16">Certificate not found</p>
{:else}
  <div class="space-y-6 max-w-4xl mx-auto">
    <div class="flex items-center gap-2 text-sm text-muted-foreground">
      <a href="/certificates" class="hover:text-foreground flex items-center gap-1 transition-colors"><ArrowLeft class="size-3.5" />Certificates</a>
      <span>/</span><span class="text-foreground font-medium">Review</span>
    </div>

    <!-- Main card -->
    <div class="rounded-2xl border border-border bg-card overflow-hidden">
      <div class="h-2 bg-gradient-to-r from-amber-500/50 to-primary/30"></div>
      <div class="p-6">
        <div class="flex items-start justify-between flex-wrap gap-4 mb-5">
          <div class="flex items-center gap-4">
            <div class="size-14 rounded-2xl bg-amber-500/10 flex items-center justify-center"><Award class="size-7 text-amber-500" /></div>
            <div>
              <h1 class="text-xl font-bold text-foreground">Certificate Request</h1>
              <p class="text-muted-foreground text-sm">{cert.contract?.indosMaster?.firstName} {cert.contract?.indosMaster?.lastName} · {cert.contract?.vessel?.name}</p>
              <div class="mt-1"><StatusBadge status={cert.status} /></div>
            </div>
          </div>
          <!-- Actions -->
          <div class="flex flex-wrap gap-2">
            {#if canL1 && cert.status === 'INITIATED'}
              <button onclick={() => (showL1 = true)} class="flex items-center gap-2 px-3 py-2 rounded-lg bg-blue-500/10 text-blue-600 dark:text-blue-400 border border-blue-500/20 text-sm font-medium hover:bg-blue-500/20 transition-all">
                <CheckCircle2 class="size-4" />L1 Review
              </button>
            {/if}
            {#if canL2 && cert.status === 'L1_REVIEWED'}
              <button onclick={() => (showL2 = true)} class="flex items-center gap-2 px-3 py-2 rounded-lg bg-violet-500/10 text-violet-600 dark:text-violet-400 border border-violet-500/20 text-sm font-medium hover:bg-violet-500/20 transition-all">
                <ShieldCheck class="size-4" />L2 Approve
              </button>
            {/if}
            {#if canAllot && cert.status === 'L2_APPROVED'}
              <button onclick={() => (showAllot = true)} class="flex items-center gap-2 px-3 py-2 rounded-lg bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 border border-emerald-500/20 text-sm font-medium hover:bg-emerald-500/20 transition-all">
                <Tag class="size-4" />Allot Certificate
              </button>
            {/if}
          </div>
        </div>

        <!-- Timeline -->
        <div class="space-y-4 border-t border-border pt-5">
          <h2 class="font-semibold text-foreground text-sm">Review Timeline</h2>
          <ol class="relative border-l border-border ml-3 space-y-5">
            <li class="ml-6">
              <div class="absolute -left-1.5 mt-1.5 size-3 rounded-full border-2 border-primary bg-background"></div>
              <p class="text-xs text-muted-foreground">{fmt(cert.enqueuedAt)}</p>
              <p class="text-sm font-medium text-foreground">Certificate Enqueued</p>
            </li>
            {#if cert.l1Officer}
              <li class="ml-6">
                <div class="absolute -left-1.5 mt-1.5 size-3 rounded-full border-2 border-blue-500 bg-background"></div>
                <p class="text-xs text-muted-foreground">{fmt(cert.l1ReviewedAt)} · {cert.l1Officer.displayName}</p>
                <p class="text-sm font-medium text-foreground">L1 Review Completed</p>
                {#if cert.l1Remarks}<p class="text-xs text-muted-foreground italic mt-0.5">"{cert.l1Remarks}"</p>{/if}
              </li>
            {/if}
            {#if cert.l2Officer}
              <li class="ml-6">
                <div class="absolute -left-1.5 mt-1.5 size-3 rounded-full border-2 border-violet-500 bg-background"></div>
                <p class="text-xs text-muted-foreground">{fmt(cert.l2ApprovedAt)} · {cert.l2Officer.displayName}</p>
                <p class="text-sm font-medium text-foreground">L2 Approval Completed</p>
                {#if cert.l2Remarks}<p class="text-xs text-muted-foreground italic mt-0.5">"{cert.l2Remarks}"</p>{/if}
              </li>
            {/if}
            {#if cert.status === 'ALLOTTED'}
              <li class="ml-6">
                <div class="absolute -left-1.5 mt-1.5 size-3 rounded-full border-2 border-emerald-500 bg-background"></div>
                <p class="text-xs text-muted-foreground">Cert No: {cert.certificateNo} · Expires: {cert.expiryDate}</p>
                <p class="text-sm font-medium text-foreground">Certificate Allotted to {cert.allottedCompany?.name}</p>
              </li>
            {/if}
          </ol>
        </div>

        {#if cert.qrHash && cert.status === 'ALLOTTED'}
          <div class="border-t border-border pt-4 mt-4">
            <a href="/verify/{cert.qrHash}" target="_blank" class="text-sm text-primary hover:underline flex items-center gap-1.5">
              <ShieldCheck class="size-4" />Verify Certificate (Public Link)
            </a>
          </div>
        {/if}
      </div>
    </div>
  </div>

  <!-- L1 Modal -->
  {#if showL1}
    <div class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-sm">
      <div class="bg-card rounded-2xl border border-border shadow-2xl w-full max-w-sm p-6 space-y-4">
        <h2 class="text-lg font-bold">L1 Review</h2>
        <div class="space-y-1.5">
          <label class="text-sm font-medium text-foreground">Remarks *</label>
          <textarea bind:value={l1Remarks} rows="3" class="w-full px-3 py-2 rounded-lg border border-input bg-background text-sm outline-none focus:ring-2 focus:ring-ring transition-all resize-none"></textarea>
        </div>
        <div class="flex gap-3">
          <button onclick={() => (showL1 = false)} class="flex-1 py-2.5 rounded-lg border border-border text-sm font-medium hover:bg-accent transition-all">Cancel</button>
          <button onclick={reviewL1} disabled={acting || !l1Remarks.trim()} class="flex-1 flex items-center justify-center gap-2 py-2.5 rounded-lg bg-blue-600 text-white text-sm font-semibold hover:bg-blue-700 disabled:opacity-60 transition-all">
            {#if acting}<Loader2 class="size-4 animate-spin" />{/if}Submit L1
          </button>
        </div>
      </div>
    </div>
  {/if}

  <!-- L2 Modal -->
  {#if showL2}
    <div class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-sm">
      <div class="bg-card rounded-2xl border border-border shadow-2xl w-full max-w-sm p-6 space-y-4">
        <h2 class="text-lg font-bold">L2 Approval</h2>
        <div class="space-y-1.5">
          <label class="text-sm font-medium text-foreground">Remarks *</label>
          <textarea bind:value={l2Remarks} rows="3" class="w-full px-3 py-2 rounded-lg border border-input bg-background text-sm outline-none focus:ring-2 focus:ring-ring transition-all resize-none"></textarea>
        </div>
        <div class="flex gap-3">
          <button onclick={() => (showL2 = false)} class="flex-1 py-2.5 rounded-lg border border-border text-sm font-medium hover:bg-accent transition-all">Cancel</button>
          <button onclick={reviewL2} disabled={acting || !l2Remarks.trim()} class="flex-1 flex items-center justify-center gap-2 py-2.5 rounded-lg bg-violet-600 text-white text-sm font-semibold hover:bg-violet-700 disabled:opacity-60 transition-all">
            {#if acting}<Loader2 class="size-4 animate-spin" />{/if}Approve L2
          </button>
        </div>
      </div>
    </div>
  {/if}

  <!-- Allot Modal -->
  {#if showAllot}
    <div class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-sm">
      <div class="bg-card rounded-2xl border border-border shadow-2xl w-full max-w-sm p-6 space-y-4">
        <h2 class="text-lg font-bold">Allot Certificate</h2>
        <div class="space-y-3">
          {#each [['companyId','Company ID (UUID) *','text'],['certificateNo','Certificate Number *','text'],['expiryDate','Expiry Date *','date']] as [f, l, t]}
            <div class="space-y-1.5">
              <label class="text-sm font-medium text-foreground">{l}</label>
              <input type={t} bind:value={(allotForm as Record<string, string>)[f]} class="w-full px-3 py-2 rounded-lg border border-input bg-background text-sm outline-none focus:ring-2 focus:ring-ring transition-all font-mono text-xs" />
            </div>
          {/each}
        </div>
        <div class="flex gap-3">
          <button onclick={() => (showAllot = false)} class="flex-1 py-2.5 rounded-lg border border-border text-sm font-medium hover:bg-accent transition-all">Cancel</button>
          <button onclick={allot} disabled={acting || !allotForm.certificateNo || !allotForm.expiryDate} class="flex-1 flex items-center justify-center gap-2 py-2.5 rounded-lg bg-emerald-600 text-white text-sm font-semibold hover:bg-emerald-700 disabled:opacity-60 transition-all">
            {#if acting}<Loader2 class="size-4 animate-spin" />{/if}Allot
          </button>
        </div>
      </div>
    </div>
  {/if}
{/if}
