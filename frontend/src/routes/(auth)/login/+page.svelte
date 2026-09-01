<script lang="ts">
  import { goto } from '$app/navigation';
  import { authStore } from '$lib/stores/auth.svelte';
  import { toast } from 'svelte-sonner';
  import { Shield, Mail, Lock, Eye, EyeOff, ArrowRight, Loader2 } from '@lucide/svelte';

  let email = $state('');
  let password = $state('');
  let showPassword = $state(false);
  let loading = $state(false);
  let errors = $state<{ email?: string; password?: string; form?: string }>({});

  function validate(): boolean {
    errors = {};
    if (!email || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
      errors.email = 'Valid email required';
    }
    if (!password || password.length < 6) {
      errors.password = 'Password must be at least 6 characters';
    }
    return Object.keys(errors).length === 0;
  }

  async function handleSubmit(e: SubmitEvent) {
    e.preventDefault();
    if (!validate()) return;
    loading = true;
    errors = {};
    try {
      await authStore.signIn(email, password);
      // Profile already loaded in signIn
      if (authStore.profile) {
        goto('/dashboard');
      } else {
        // Authenticated but no profile yet — go register profile
        goto('/register');
      }
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Sign in failed';
      if (message.toLowerCase().includes('invalid') || message.toLowerCase().includes('credentials')) {
        errors.form = 'Invalid email or password';
      } else {
        errors.form = message;
      }
      toast.error(errors.form ?? 'Sign in failed');
    } finally {
      loading = false;
    }
  }
</script>

<svelte:head>
  <title>Sign In — Artemis</title>
  <meta name="description" content="Sign in to Artemis maritime management platform" />
</svelte:head>

<div class="w-full max-w-md">
  <!-- Card -->
  <div class="bg-card border border-border rounded-2xl shadow-xl shadow-black/5 overflow-hidden">
    <!-- Header gradient band -->
    <div class="h-1.5 bg-gradient-to-r from-primary via-primary/70 to-amber-400"></div>

    <div class="p-8">
      <!-- Logo + title -->
      <div class="flex flex-col items-center gap-3 mb-8">
        <div class="size-14 rounded-2xl bg-primary/10 flex items-center justify-center ring-4 ring-primary/10">
          <Shield class="size-7 text-primary" />
        </div>
        <div class="text-center">
          <h1 class="text-2xl font-bold text-foreground tracking-tight">Welcome back</h1>
          <p class="text-sm text-muted-foreground mt-1">Sign in to Artemis DG Shipping Platform</p>
        </div>
      </div>

      <!-- Form -->
      <form onsubmit={handleSubmit} class="space-y-4" novalidate>
        <!-- Email -->
        <div class="space-y-1.5">
          <label for="email" class="text-sm font-medium text-foreground">Email address</label>
          <div class="relative">
            <Mail class="absolute left-3 top-1/2 -translate-y-1/2 size-4 text-muted-foreground pointer-events-none" />
            <input
              id="email"
              type="email"
              bind:value={email}
              autocomplete="email"
              placeholder="you@example.com"
              class={[
                'w-full pl-10 pr-4 py-2.5 rounded-lg border bg-background text-sm outline-none transition-all',
                'focus:ring-2 focus:ring-ring focus:border-transparent',
                errors.email ? 'border-destructive' : 'border-input'
              ].join(' ')}
            />
          </div>
          {#if errors.email}<p class="text-xs text-destructive">{errors.email}</p>{/if}
        </div>

        <!-- Password -->
        <div class="space-y-1.5">
          <label for="password" class="text-sm font-medium text-foreground">Password</label>
          <div class="relative">
            <Lock class="absolute left-3 top-1/2 -translate-y-1/2 size-4 text-muted-foreground pointer-events-none" />
            <input
              id="password"
              type={showPassword ? 'text' : 'password'}
              bind:value={password}
              autocomplete="current-password"
              placeholder="••••••••"
              class={[
                'w-full pl-10 pr-10 py-2.5 rounded-lg border bg-background text-sm outline-none transition-all',
                'focus:ring-2 focus:ring-ring focus:border-transparent',
                errors.password ? 'border-destructive' : 'border-input'
              ].join(' ')}
            />
            <button
              type="button"
              onclick={() => (showPassword = !showPassword)}
              class="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground transition-colors"
            >
              {#if showPassword}
                <EyeOff class="size-4" />
              {:else}
                <Eye class="size-4" />
              {/if}
            </button>
          </div>
          {#if errors.password}<p class="text-xs text-destructive">{errors.password}</p>{/if}
        </div>

        <!-- Form-level error -->
        {#if errors.form}
          <div class="rounded-lg bg-destructive/10 border border-destructive/20 px-3 py-2.5 text-sm text-destructive">
            {errors.form}
          </div>
        {/if}

        <!-- Submit -->
        <button
          type="submit"
          disabled={loading}
          class="w-full flex items-center justify-center gap-2 py-2.5 px-4 rounded-lg bg-primary text-primary-foreground font-semibold text-sm hover:bg-primary/90 disabled:opacity-60 disabled:cursor-not-allowed transition-all shadow-sm shadow-primary/30 active:scale-[0.98]"
        >
          {#if loading}
            <Loader2 class="size-4 animate-spin" />
            Signing in…
          {:else}
            Sign In
            <ArrowRight class="size-4" />
          {/if}
        </button>
      </form>

      <!-- Divider -->
      <div class="flex items-center gap-3 my-6">
        <hr class="flex-1 border-border" />
        <span class="text-xs text-muted-foreground">Don't have an account?</span>
        <hr class="flex-1 border-border" />
      </div>

      <a
        href="/register"
        class="w-full flex items-center justify-center gap-2 py-2.5 px-4 rounded-lg border border-border bg-secondary/50 text-secondary-foreground font-medium text-sm hover:bg-secondary transition-all"
      >
        Create an account
      </a>
    </div>
  </div>

  <p class="text-center text-xs text-muted-foreground mt-6">
    Artemis Maritime Management Platform &copy; {new Date().getFullYear()}
  </p>
</div>
