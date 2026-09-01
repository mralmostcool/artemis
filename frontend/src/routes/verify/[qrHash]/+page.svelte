<script lang="ts">
  import { onMount } from 'svelte';
  import { page } from '$app/state';
  import { api } from '$lib/api';
  import type { Certificate } from '$lib/types';
  import { ShieldCheck, ShieldX, Loader2, Ship, Calendar, Tag } from '@lucide/svelte';

  const qrHash = $derived(page.params.qrHash);
  let cert = $state<Certificate | null>(null);
  let loading = $state(true);
  let notFound = $state(false);

  onMount(async () => {
    loading = true;
    try { cert = await api.get<Certificate>(`/api/v1/certificates/verify/${qrHash}`); }
    catch { notFound = true; }
    finally { loading = false; }
  });

  const isValid = $derived(cert?.status === 'ALLOTTED' && cert.expiryDate && new Date(cert.expiryDate) > new Date());
</script>

<svelte:head>
  <title>Certificate Verification — Artemis</title>
  <meta name="description" content="Verify the authenticity of a DG Shipping seafarer certificate" />
</svelte:head>

<div class="min-h-screen bg-background flex items-center justify-center p-6">
  <div class="w-full max-w-sm">
    <!-- Header -->
    <div class="text-center mb-8">
      <div class="size-14 rounded-2xl bg-primary/10 flex items-center justify-center mx-auto mb-3">
        <ShieldCheck class="size-7 text-primary" />
      </div>
      <h1 class="text-xl font-bold text-foreground">Certificate Verification</h1>
      <p class="text-sm text-muted-foreground mt-1">Artemis DG Shipping Registry</p>
    </div>

    {#if loading}
      <div class="flex items-center justify-center py-12">
        <Loader2 class="size-8 animate-spin text-primary" />
      </div>
    {:else if notFound || !cert}
      <div class="rounded-2xl border border-destructive/20 bg-destructive/5 p-8 text-center">
        <ShieldX class="size-12 text-destructive mx-auto mb-3" />
        <h2 class="text-lg font-bold text-destructive">Certificate Not Found</h2>
        <p class="text-sm text-muted-foreground mt-2">This QR code does not match any certificate in our registry.</p>
      </div>
    {:else}
      <div class={['rounded-2xl border p-6 text-center', isValid ? 'border-emerald-500/30 bg-emerald-500/5' : 'border-amber-500/30 bg-amber-500/5'].join(' ')}>
        {#if isValid}
          <ShieldCheck class="size-14 text-emerald-500 mx-auto mb-3" />
          <h2 class="text-xl font-bold text-emerald-600 dark:text-emerald-400">Certificate Valid</h2>
        {:else}
          <ShieldX class="size-14 text-amber-500 mx-auto mb-3" />
          <h2 class="text-xl font-bold text-amber-600 dark:text-amber-400">Certificate Expired</h2>
        {/if}

        <div class="mt-5 space-y-3 text-left">
          <div class="rounded-xl bg-background/60 border border-border p-4 space-y-3">
            <div class="flex items-center gap-2 pb-2 border-b border-border">
              <Tag class="size-4 text-muted-foreground" />
              <span class="text-xs font-semibold text-muted-foreground uppercase tracking-wide">Certificate Details</span>
            </div>
            {#each [
              ['Seafarer', `${cert.contract?.indosMaster?.firstName ?? ''} ${cert.contract?.indosMaster?.lastName ?? ''}`.trim()],
              ['Vessel', cert.contract?.vessel?.name],
              ['Issued To', cert.allottedCompany?.name],
              ['Certificate No.', cert.certificateNo],
              ['Expiry Date', cert.expiryDate ? new Date(cert.expiryDate).toLocaleDateString(undefined, { year: 'numeric', month: 'long', day: 'numeric' }) : null]
            ] as [l, v]}
              {#if v}
                <div class="flex justify-between gap-4">
                  <span class="text-xs text-muted-foreground shrink-0">{l}</span>
                  <span class="text-xs font-semibold text-foreground text-right">{v}</span>
                </div>
              {/if}
            {/each}
          </div>
        </div>

        <p class="text-xs text-muted-foreground mt-4">
          Verified by Artemis Maritime Registry · {new Date().toLocaleDateString()}
        </p>
      </div>
    {/if}

    <p class="text-center text-xs text-muted-foreground mt-6">
      Powered by <span class="font-semibold text-foreground">Artemis DG Shipping Platform</span>
    </p>
  </div>
</div>
