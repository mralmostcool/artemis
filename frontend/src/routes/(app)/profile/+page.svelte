<script lang="ts">
  import { onMount } from 'svelte';
  import { authStore } from '$lib/stores/auth.svelte';
  import { api } from '$lib/api';
  import type { ProfileRequest, ProfileIndosMapping } from '$lib/types';
  import { toast } from 'svelte-sonner';
  import {
    UserCircle, Mail, Phone, Calendar, Shield,
    Anchor, Link2, Loader2, Save, CheckCircle2
  } from '@lucide/svelte';

  let editing = $state(false);
  let savingProfile = $state(false);
  let linkingIndos = $state(false);
  let indosInput = $state('');
  let indosMapping = $state<ProfileIndosMapping | null>(null);
  let loadingIndos = $state(false);

  // Editable profile fields
  let firstName = $state(authStore.profile?.firstName ?? '');
  let lastName = $state(authStore.profile?.lastName ?? '');
  let displayName = $state(authStore.profile?.displayName ?? '');
  let phoneNumber = $state(authStore.profile?.phoneNumber ?? '');
  let gender = $state(authStore.profile?.gender ?? '');
  let dateOfBirth = $state(authStore.profile?.dateOfBirth ?? '');

  const roleLabels: Record<string, string> = {
    DG_SHIPPING_ADMIN: 'DG Shipping Administrator',
    DG_SHIPPING_L1: 'DG Shipping Officer L1',
    DG_SHIPPING_L2: 'DG Shipping Officer L2',
    COMPANY_ADMIN: 'Company Administrator',
    COMPANY_USER: 'Company Staff',
    INSTITUTE_ADMIN: 'Institute Administrator',
    INSTITUTE_USER: 'Institute Staff',
    CANDIDATE: 'Seafarer Candidate'
  };

  const isCandidate = $derived(authStore.profile?.role === 'CANDIDATE');

  onMount(async () => {
    if (isCandidate) {
      loadingIndos = true;
      try {
        indosMapping = await api.get<ProfileIndosMapping>('/api/v1/seafarers/link');
      } catch {
        indosMapping = null;
      } finally {
        loadingIndos = false;
      }
    }
  });

  async function saveProfile() {
    savingProfile = true;
    const body: ProfileRequest = { firstName, lastName, displayName, phoneNumber, gender, dateOfBirth };
    try {
      await api.put('/api/v1/auth', body);
      await authStore.loadProfile();
      toast.success('Profile updated');
      editing = false;
    } catch {
      toast.error('Failed to update profile');
    } finally {
      savingProfile = false;
    }
  }

  async function linkIndos() {
    if (!indosInput.trim()) return;
    linkingIndos = true;
    try {
      const result = await api.post<ProfileIndosMapping>(`/api/v1/seafarers/link?indos=${encodeURIComponent(indosInput.trim())}`);
      indosMapping = result;
      indosInput = '';
      toast.success('INDoS linked successfully!');
    } catch {
      toast.error('Could not link INDoS number. Verify it exists in the system.');
    } finally {
      linkingIndos = false;
    }
  }

  const initials = $derived(() => {
    const p = authStore.profile;
    if (!p) return '?';
    return ((p.firstName?.[0] ?? '') + (p.lastName?.[0] ?? '')).toUpperCase() || p.displayName?.[0]?.toUpperCase() || '?';
  });
</script>

<svelte:head>
  <title>Profile — Artemis</title>
</svelte:head>

<div class="max-w-3xl mx-auto space-y-6">
  <div>
    <h1 class="text-2xl font-bold text-foreground">My Profile</h1>
    <p class="text-sm text-muted-foreground mt-1">Manage your personal information and settings</p>
  </div>

  <!-- Profile Card -->
  <div class="rounded-2xl border border-border bg-card overflow-hidden">
    <!-- Header band -->
    <div class="h-24 bg-gradient-to-r from-primary/30 via-primary/10 to-amber-400/10 relative">
      <div class="absolute -bottom-10 left-6">
        <div class="size-20 rounded-2xl border-4 border-card bg-primary/15 flex items-center justify-center">
          {#if authStore.profile?.avatarUrl}
            <img src={authStore.profile.avatarUrl} alt="avatar" class="size-20 rounded-xl object-cover" />
          {:else}
            <span class="text-2xl font-bold text-primary">{initials()}</span>
          {/if}
        </div>
      </div>
      <div class="absolute top-3 right-4">
        {#if !editing}
          <button onclick={() => { editing = true; firstName = authStore.profile?.firstName ?? ''; lastName = authStore.profile?.lastName ?? ''; displayName = authStore.profile?.displayName ?? ''; phoneNumber = authStore.profile?.phoneNumber ?? ''; gender = authStore.profile?.gender ?? ''; dateOfBirth = authStore.profile?.dateOfBirth ?? ''; }}
            class="flex items-center gap-2 px-3 py-1.5 rounded-lg bg-card/80 backdrop-blur text-sm font-medium text-foreground border border-border hover:bg-card transition-all">
            Edit profile
          </button>
        {/if}
      </div>
    </div>

    <div class="pt-14 p-6">
      <div class="mb-6">
        <h2 class="text-xl font-bold text-foreground">{authStore.profile?.displayName}</h2>
        <div class="flex items-center gap-2 mt-1 flex-wrap">
          <span class="flex items-center gap-1.5 text-sm text-muted-foreground">
            <Mail class="size-3.5" />{authStore.profile?.email}
          </span>
          <span class="text-muted-foreground/40">·</span>
          <span class="flex items-center gap-1.5 text-xs font-medium text-primary bg-primary/10 px-2 py-0.5 rounded-full">
            <Shield class="size-3" />
            {roleLabels[authStore.profile?.role ?? ''] ?? authStore.profile?.role}
          </span>
          {#if authStore.profile?.organizationName}
            <span class="text-xs text-muted-foreground bg-muted px-2 py-0.5 rounded-full">{authStore.profile.organizationName}</span>
          {/if}
        </div>
      </div>

      {#if !editing}
        <!-- View mode -->
        <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <div class="space-y-1">
            <p class="text-xs text-muted-foreground uppercase tracking-wide font-medium">First Name</p>
            <p class="text-sm font-medium text-foreground">{authStore.profile?.firstName || '—'}</p>
          </div>
          <div class="space-y-1">
            <p class="text-xs text-muted-foreground uppercase tracking-wide font-medium">Last Name</p>
            <p class="text-sm font-medium text-foreground">{authStore.profile?.lastName || '—'}</p>
          </div>
          <div class="space-y-1">
            <p class="text-xs text-muted-foreground uppercase tracking-wide font-medium flex items-center gap-1.5"><Phone class="size-3" />Phone</p>
            <p class="text-sm font-medium text-foreground">{authStore.profile?.phoneNumber || '—'}</p>
          </div>
          <div class="space-y-1">
            <p class="text-xs text-muted-foreground uppercase tracking-wide font-medium flex items-center gap-1.5"><Calendar class="size-3" />Date of Birth</p>
            <p class="text-sm font-medium text-foreground">{authStore.profile?.dateOfBirth || '—'}</p>
          </div>
          <div class="space-y-1">
            <p class="text-xs text-muted-foreground uppercase tracking-wide font-medium">Gender</p>
            <p class="text-sm font-medium text-foreground capitalize">{authStore.profile?.gender?.toLowerCase() || '—'}</p>
          </div>
        </div>
      {:else}
        <!-- Edit mode -->
        <div class="space-y-4">
          <div class="grid grid-cols-2 gap-3">
            <div class="space-y-1.5">
              <label class="text-sm font-medium text-foreground">First Name</label>
              <input type="text" bind:value={firstName} class="w-full px-3 py-2 rounded-lg border border-input bg-background text-sm outline-none focus:ring-2 focus:ring-ring transition-all" />
            </div>
            <div class="space-y-1.5">
              <label class="text-sm font-medium text-foreground">Last Name</label>
              <input type="text" bind:value={lastName} class="w-full px-3 py-2 rounded-lg border border-input bg-background text-sm outline-none focus:ring-2 focus:ring-ring transition-all" />
            </div>
          </div>
          <div class="space-y-1.5">
            <label class="text-sm font-medium text-foreground">Display Name <span class="text-destructive">*</span></label>
            <input type="text" bind:value={displayName} class="w-full px-3 py-2 rounded-lg border border-input bg-background text-sm outline-none focus:ring-2 focus:ring-ring transition-all" />
          </div>
          <div class="grid grid-cols-2 gap-3">
            <div class="space-y-1.5">
              <label class="text-sm font-medium text-foreground">Phone</label>
              <input type="tel" bind:value={phoneNumber} class="w-full px-3 py-2 rounded-lg border border-input bg-background text-sm outline-none focus:ring-2 focus:ring-ring transition-all" />
            </div>
            <div class="space-y-1.5">
              <label class="text-sm font-medium text-foreground">Date of Birth</label>
              <input type="date" bind:value={dateOfBirth} class="w-full px-3 py-2 rounded-lg border border-input bg-background text-sm outline-none focus:ring-2 focus:ring-ring transition-all" />
            </div>
          </div>
          <div class="space-y-1.5">
            <label class="text-sm font-medium text-foreground">Gender</label>
            <select bind:value={gender} class="w-full px-3 py-2 rounded-lg border border-input bg-background text-sm outline-none focus:ring-2 focus:ring-ring transition-all">
              <option value="">Prefer not to say</option>
              <option value="MALE">Male</option>
              <option value="FEMALE">Female</option>
              <option value="OTHER">Other</option>
            </select>
          </div>
          <div class="flex gap-3 pt-2">
            <button onclick={() => (editing = false)} class="px-4 py-2 rounded-lg border border-border bg-secondary/50 text-secondary-foreground text-sm font-medium hover:bg-secondary transition-all">Cancel</button>
            <button onclick={saveProfile} disabled={savingProfile} class="flex items-center gap-2 px-4 py-2 rounded-lg bg-primary text-primary-foreground text-sm font-semibold hover:bg-primary/90 disabled:opacity-60 transition-all">
              {#if savingProfile}<Loader2 class="size-4 animate-spin" />Saving…{:else}<Save class="size-4" />Save Changes{/if}
            </button>
          </div>
        </div>
      {/if}
    </div>
  </div>

  <!-- INDoS Linking (CANDIDATE only) -->
  {#if isCandidate}
    <div class="rounded-2xl border border-border bg-card p-6 space-y-4">
      <div class="flex items-center gap-3">
        <div class="size-10 rounded-xl bg-primary/10 flex items-center justify-center">
          <Anchor class="size-5 text-primary" />
        </div>
        <div>
          <h3 class="font-semibold text-foreground">INDoS Number Linking</h3>
          <p class="text-sm text-muted-foreground">Connect your seafarer record from the DG Shipping registry</p>
        </div>
      </div>

      {#if loadingIndos}
        <div class="flex items-center gap-2 text-sm text-muted-foreground">
          <Loader2 class="size-4 animate-spin" />Checking INDoS link…
        </div>
      {:else if indosMapping}
        <div class="flex items-start gap-3 rounded-xl bg-emerald-500/10 border border-emerald-500/20 p-4">
          <CheckCircle2 class="size-5 text-emerald-500 shrink-0 mt-0.5" />
          <div>
            <p class="text-sm font-semibold text-emerald-700 dark:text-emerald-400">INDoS Linked Successfully</p>
            <p class="text-sm text-muted-foreground mt-0.5">
              {indosMapping.indosMaster?.firstName} {indosMapping.indosMaster?.lastName}
              — INDoS No. <span class="font-mono font-bold">{indosMapping.indosMaster?.indosNo}</span>
            </p>
          </div>
        </div>
      {:else}
        <div class="flex gap-3">
          <input
            type="text"
            bind:value={indosInput}
            placeholder="Enter your INDoS number (e.g. IN-12345678)"
            class="flex-1 px-4 py-2.5 rounded-lg border border-input bg-background text-sm outline-none focus:ring-2 focus:ring-ring transition-all font-mono"
          />
          <button
            onclick={linkIndos}
            disabled={linkingIndos || !indosInput.trim()}
            class="flex items-center gap-2 px-4 py-2.5 rounded-lg bg-primary text-primary-foreground text-sm font-semibold hover:bg-primary/90 disabled:opacity-60 transition-all whitespace-nowrap"
          >
            {#if linkingIndos}<Loader2 class="size-4 animate-spin" />Linking…{:else}<Link2 class="size-4" />Link INDoS{/if}
          </button>
        </div>
        <p class="text-xs text-muted-foreground">Your INDoS number is provided by DG Shipping. You must link it before enrolling in courses or viewing contracts.</p>
      {/if}
    </div>
  {/if}
</div>
