<script lang="ts">
  import { goto } from '$app/navigation';
  import { authStore } from '$lib/stores/auth.svelte';
  import { api } from '$lib/api';
  import type { ProfileRequest } from '$lib/types';
  import { toast } from 'svelte-sonner';
  import {
    Shield, Mail, Lock, Eye, EyeOff, User, Phone, Building2,
    ArrowRight, ArrowLeft, Check, Loader2, ChevronDown
  } from '@lucide/svelte';

  // ─── Step state ──────────────────────────────────────────────────────────────
  let step = $state(1); // 1 = Account, 2 = Profile
  let loading = $state(false);

  // ─── Step 1: Account ─────────────────────────────────────────────────────────
  let email = $state('');
  let password = $state('');
  let confirmPassword = $state('');
  let showPassword = $state(false);
  let step1Errors = $state<Record<string, string>>({});

  // ─── Step 2: Profile ─────────────────────────────────────────────────────────
  let firstName = $state('');
  let lastName = $state('');
  let displayName = $state('');
  let phoneNumber = $state('');
  let gender = $state('');
  let dateOfBirth = $state('');
  let accountType = $state<'candidate' | 'company-new' | 'company-join' | 'institute'>('candidate');
  let organizationName = $state('');
  let organizationId = $state('');
  let step2Errors = $state<Record<string, string>>({});

  // Auto-populate displayName from first + last
  $effect(() => {
    if (!displayName && (firstName || lastName)) {
      displayName = [firstName, lastName].filter(Boolean).join(' ');
    }
  });

  // ─── Step 1 validation ────────────────────────────────────────────────────────
  function validateStep1(): boolean {
    step1Errors = {};
    if (!email || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) step1Errors.email = 'Valid email required';
    if (!password || password.length < 8) step1Errors.password = 'Minimum 8 characters';
    if (password !== confirmPassword) step1Errors.confirmPassword = 'Passwords do not match';
    return Object.keys(step1Errors).length === 0;
  }

  async function handleStep1(e: SubmitEvent) {
    e.preventDefault();
    if (!validateStep1()) return;
    loading = true;
    try {
      await authStore.signUp(email, password);
      toast.success('Account created! Complete your profile.');
      step = 2;
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Signup failed';
      step1Errors.form = msg;
      toast.error(msg);
    } finally {
      loading = false;
    }
  }

  // ─── Step 2 validation ────────────────────────────────────────────────────────
  function validateStep2(): boolean {
    step2Errors = {};
    if (!displayName.trim()) step2Errors.displayName = 'Display name is required';
    if (accountType === 'company-new' && !organizationName.trim()) {
      step2Errors.organizationName = 'Company name is required';
    }
    if ((accountType === 'company-join' || accountType === 'institute') && !organizationId.trim()) {
      step2Errors.organizationId = 'Organization ID is required';
    }
    return Object.keys(step2Errors).length === 0;
  }

  async function handleStep2(e: SubmitEvent) {
    e.preventDefault();
    if (!validateStep2()) return;
    loading = true;

    const body: ProfileRequest = {
      firstName,
      lastName,
      displayName,
      phoneNumber: phoneNumber || undefined,
      gender: gender || undefined,
      dateOfBirth: dateOfBirth || undefined
    };

    if (accountType === 'company-new') {
      body.organizationName = organizationName;
    } else if (accountType === 'company-join') {
      body.organizationId = organizationId;
      body.role = 'COMPANY_USER';
    } else if (accountType === 'institute') {
      body.organizationId = organizationId;
      body.role = 'INSTITUTE_USER';
    } else {
      body.role = 'CANDIDATE';
    }

    try {
      await api.post('/api/v1/auth', body);
      await authStore.loadProfile();
      toast.success('Profile created! Welcome to Artemis.');
      goto('/dashboard');
    } catch {
      toast.error('Failed to create profile. Please try again.');
    } finally {
      loading = false;
    }
  }

  const accountTypeOptions = [
    { value: 'candidate', label: '🧑‍✈️ Seafarer Candidate', desc: 'Access INDoS records, training, contracts' },
    { value: 'company-new', label: '🏢 New Shipping Company', desc: 'Create and manage your company profile' },
    { value: 'company-join', label: '🏢 Join Existing Company', desc: 'Join with your company ID' },
    { value: 'institute', label: '🎓 Training Institute', desc: 'Join an accredited institute' }
  ] as const;
</script>

<svelte:head>
  <title>Create Account — Artemis</title>
  <meta name="description" content="Create your Artemis maritime management account" />
</svelte:head>

<div class="w-full max-w-md">
  <div class="bg-card border border-border rounded-2xl shadow-xl shadow-black/5 overflow-hidden">
    <div class="h-1.5 bg-gradient-to-r from-primary via-primary/70 to-amber-400"></div>

    <div class="p-8">
      <!-- Logo -->
      <div class="flex flex-col items-center gap-3 mb-6">
        <div class="size-14 rounded-2xl bg-primary/10 flex items-center justify-center ring-4 ring-primary/10">
          <Shield class="size-7 text-primary" />
        </div>
        <div class="text-center">
          <h1 class="text-2xl font-bold text-foreground tracking-tight">Create account</h1>
          <p class="text-sm text-muted-foreground mt-1">Join the Artemis maritime platform</p>
        </div>
      </div>

      <!-- Step indicator -->
      <div class="flex items-center gap-2 mb-8">
        {#each [1, 2] as s}
          <div class={[
            'flex-1 h-1.5 rounded-full transition-all duration-300',
            step >= s ? 'bg-primary' : 'bg-muted'
          ].join(' ')}></div>
        {/each}
        <span class="text-xs text-muted-foreground shrink-0">Step {step} of 2</span>
      </div>

      <!-- ── STEP 1: Account credentials ─────────────────────────── -->
      {#if step === 1}
        <form onsubmit={handleStep1} class="space-y-4" novalidate>
          <div class="space-y-1.5">
            <label for="reg-email" class="text-sm font-medium text-foreground">Email address</label>
            <div class="relative">
              <Mail class="absolute left-3 top-1/2 -translate-y-1/2 size-4 text-muted-foreground pointer-events-none" />
              <input
                id="reg-email"
                type="email"
                bind:value={email}
                autocomplete="email"
                placeholder="you@example.com"
                class={['w-full pl-10 pr-4 py-2.5 rounded-lg border bg-background text-sm outline-none transition-all focus:ring-2 focus:ring-ring focus:border-transparent', step1Errors.email ? 'border-destructive' : 'border-input'].join(' ')}
              />
            </div>
            {#if step1Errors.email}<p class="text-xs text-destructive">{step1Errors.email}</p>{/if}
          </div>

          <div class="space-y-1.5">
            <label for="reg-password" class="text-sm font-medium text-foreground">Password</label>
            <div class="relative">
              <Lock class="absolute left-3 top-1/2 -translate-y-1/2 size-4 text-muted-foreground pointer-events-none" />
              <input
                id="reg-password"
                type={showPassword ? 'text' : 'password'}
                bind:value={password}
                autocomplete="new-password"
                placeholder="Min. 8 characters"
                class={['w-full pl-10 pr-10 py-2.5 rounded-lg border bg-background text-sm outline-none transition-all focus:ring-2 focus:ring-ring focus:border-transparent', step1Errors.password ? 'border-destructive' : 'border-input'].join(' ')}
              />
              <button type="button" onclick={() => (showPassword = !showPassword)} class="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground">
                {#if showPassword}<EyeOff class="size-4" />{:else}<Eye class="size-4" />{/if}
              </button>
            </div>
            {#if step1Errors.password}<p class="text-xs text-destructive">{step1Errors.password}</p>{/if}
          </div>

          <div class="space-y-1.5">
            <label for="reg-confirm" class="text-sm font-medium text-foreground">Confirm password</label>
            <div class="relative">
              <Lock class="absolute left-3 top-1/2 -translate-y-1/2 size-4 text-muted-foreground pointer-events-none" />
              <input
                id="reg-confirm"
                type={showPassword ? 'text' : 'password'}
                bind:value={confirmPassword}
                autocomplete="new-password"
                placeholder="Re-enter password"
                class={['w-full pl-10 pr-4 py-2.5 rounded-lg border bg-background text-sm outline-none transition-all focus:ring-2 focus:ring-ring focus:border-transparent', step1Errors.confirmPassword ? 'border-destructive' : 'border-input'].join(' ')}
              />
            </div>
            {#if step1Errors.confirmPassword}<p class="text-xs text-destructive">{step1Errors.confirmPassword}</p>{/if}
          </div>

          {#if step1Errors.form}
            <div class="rounded-lg bg-destructive/10 border border-destructive/20 px-3 py-2.5 text-sm text-destructive">{step1Errors.form}</div>
          {/if}

          <button type="submit" disabled={loading} class="w-full flex items-center justify-center gap-2 py-2.5 px-4 rounded-lg bg-primary text-primary-foreground font-semibold text-sm hover:bg-primary/90 disabled:opacity-60 transition-all shadow-sm shadow-primary/30 active:scale-[0.98]">
            {#if loading}
              <Loader2 class="size-4 animate-spin" />Creating account…
            {:else}
              Continue <ArrowRight class="size-4" />
            {/if}
          </button>
        </form>
      {/if}

      <!-- ── STEP 2: Profile ──────────────────────────────────────── -->
      {#if step === 2}
        <form onsubmit={handleStep2} class="space-y-4" novalidate>
          <!-- Name row -->
          <div class="grid grid-cols-2 gap-3">
            <div class="space-y-1.5">
              <label for="firstName" class="text-sm font-medium text-foreground">First name</label>
              <div class="relative">
                <User class="absolute left-3 top-1/2 -translate-y-1/2 size-4 text-muted-foreground pointer-events-none" />
                <input id="firstName" type="text" bind:value={firstName} placeholder="Jane" class="w-full pl-10 pr-4 py-2.5 rounded-lg border border-input bg-background text-sm outline-none focus:ring-2 focus:ring-ring focus:border-transparent transition-all" />
              </div>
            </div>
            <div class="space-y-1.5">
              <label for="lastName" class="text-sm font-medium text-foreground">Last name</label>
              <input id="lastName" type="text" bind:value={lastName} placeholder="Smith" class="w-full px-4 py-2.5 rounded-lg border border-input bg-background text-sm outline-none focus:ring-2 focus:ring-ring focus:border-transparent transition-all" />
            </div>
          </div>

          <div class="space-y-1.5">
            <label for="displayName" class="text-sm font-medium text-foreground">Display name <span class="text-destructive">*</span></label>
            <input id="displayName" type="text" bind:value={displayName} placeholder="How should we address you?" class={['w-full px-4 py-2.5 rounded-lg border bg-background text-sm outline-none focus:ring-2 focus:ring-ring focus:border-transparent transition-all', step2Errors.displayName ? 'border-destructive' : 'border-input'].join(' ')} />
            {#if step2Errors.displayName}<p class="text-xs text-destructive">{step2Errors.displayName}</p>{/if}
          </div>

          <!-- Phone & Gender -->
          <div class="grid grid-cols-2 gap-3">
            <div class="space-y-1.5">
              <label for="phone" class="text-sm font-medium text-foreground">Phone</label>
              <div class="relative">
                <Phone class="absolute left-3 top-1/2 -translate-y-1/2 size-4 text-muted-foreground pointer-events-none" />
                <input id="phone" type="tel" bind:value={phoneNumber} placeholder="+91 9999..." class="w-full pl-10 pr-4 py-2.5 rounded-lg border border-input bg-background text-sm outline-none focus:ring-2 focus:ring-ring focus:border-transparent transition-all" />
              </div>
            </div>
            <div class="space-y-1.5">
              <label for="gender" class="text-sm font-medium text-foreground">Gender</label>
              <div class="relative">
                <select id="gender" bind:value={gender} class="w-full appearance-none px-4 py-2.5 rounded-lg border border-input bg-background text-sm outline-none focus:ring-2 focus:ring-ring focus:border-transparent transition-all pr-8">
                  <option value="">Prefer not to say</option>
                  <option value="MALE">Male</option>
                  <option value="FEMALE">Female</option>
                  <option value="OTHER">Other</option>
                </select>
                <ChevronDown class="absolute right-3 top-1/2 -translate-y-1/2 size-3.5 text-muted-foreground pointer-events-none" />
              </div>
            </div>
          </div>

          <div class="space-y-1.5">
            <label for="dob" class="text-sm font-medium text-foreground">Date of birth</label>
            <input id="dob" type="date" bind:value={dateOfBirth} class="w-full px-4 py-2.5 rounded-lg border border-input bg-background text-sm outline-none focus:ring-2 focus:ring-ring focus:border-transparent transition-all" />
          </div>

          <!-- Account type -->
          <div class="space-y-2">
            <span class="text-sm font-medium text-foreground">I am a…</span>
            <div class="grid grid-cols-1 gap-2">
              {#each accountTypeOptions as opt}
                <button
                  type="button"
                  onclick={() => (accountType = opt.value)}
                  class={[
                    'flex items-start gap-3 rounded-lg border px-4 py-3 text-left transition-all',
                    accountType === opt.value
                      ? 'border-primary bg-primary/5 ring-1 ring-primary/30'
                      : 'border-border hover:border-primary/40 hover:bg-accent/30'
                  ].join(' ')}
                >
                  <div class={['size-4 rounded-full border-2 shrink-0 mt-0.5 flex items-center justify-center', accountType === opt.value ? 'border-primary bg-primary' : 'border-muted-foreground/40'].join(' ')}>
                    {#if accountType === opt.value}<Check class="size-2.5 text-primary-foreground" />{/if}
                  </div>
                  <div>
                    <p class="text-sm font-medium text-foreground">{opt.label}</p>
                    <p class="text-xs text-muted-foreground">{opt.desc}</p>
                  </div>
                </button>
              {/each}
            </div>
          </div>

          <!-- Conditional org fields -->
          {#if accountType === 'company-new'}
            <div class="space-y-1.5">
              <label for="orgName" class="text-sm font-medium text-foreground">Company name <span class="text-destructive">*</span></label>
              <div class="relative">
                <Building2 class="absolute left-3 top-1/2 -translate-y-1/2 size-4 text-muted-foreground pointer-events-none" />
                <input id="orgName" type="text" bind:value={organizationName} placeholder="Seaways Shipping Pvt Ltd" class={['w-full pl-10 pr-4 py-2.5 rounded-lg border bg-background text-sm outline-none focus:ring-2 focus:ring-ring focus:border-transparent transition-all', step2Errors.organizationName ? 'border-destructive' : 'border-input'].join(' ')} />
              </div>
              {#if step2Errors.organizationName}<p class="text-xs text-destructive">{step2Errors.organizationName}</p>{/if}
            </div>
          {/if}

          {#if accountType === 'company-join' || accountType === 'institute'}
            <div class="space-y-1.5">
              <label for="orgId" class="text-sm font-medium text-foreground">Organization ID <span class="text-destructive">*</span></label>
              <input id="orgId" type="text" bind:value={organizationId} placeholder="UUID provided by your admin" class={['w-full px-4 py-2.5 rounded-lg border bg-background text-sm outline-none focus:ring-2 focus:ring-ring focus:border-transparent transition-all font-mono text-xs', step2Errors.organizationId ? 'border-destructive' : 'border-input'].join(' ')} />
              {#if step2Errors.organizationId}<p class="text-xs text-destructive">{step2Errors.organizationId}</p>{/if}
            </div>
          {/if}

          <div class="flex gap-3 pt-1">
            <button type="button" onclick={() => (step = 1)} class="flex items-center gap-2 py-2.5 px-4 rounded-lg border border-border bg-secondary/50 text-secondary-foreground font-medium text-sm hover:bg-secondary transition-all">
              <ArrowLeft class="size-4" /> Back
            </button>
            <button type="submit" disabled={loading} class="flex-1 flex items-center justify-center gap-2 py-2.5 px-4 rounded-lg bg-primary text-primary-foreground font-semibold text-sm hover:bg-primary/90 disabled:opacity-60 transition-all shadow-sm shadow-primary/30 active:scale-[0.98]">
              {#if loading}
                <Loader2 class="size-4 animate-spin" />Setting up…
              {:else}
                Complete Setup <Check class="size-4" />
              {/if}
            </button>
          </div>
        </form>
      {/if}

      <div class="mt-6 text-center">
        <a href="/login" class="text-sm text-muted-foreground hover:text-foreground transition-colors">
          Already have an account? <span class="font-medium text-primary">Sign in</span>
        </a>
      </div>
    </div>
  </div>
</div>
