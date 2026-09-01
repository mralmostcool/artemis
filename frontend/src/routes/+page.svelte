<script lang="ts">
  import { goto } from '$app/navigation';
  import { onMount } from 'svelte';
  import { authStore } from '$lib/stores/auth.svelte';

  onMount(() => {
    if (!authStore.loading) {
      if (authStore.isAuthenticated()) {
        goto('/dashboard');
      } else {
        goto('/login');
      }
    }
  });

  $effect(() => {
    if (!authStore.loading) {
      if (authStore.isAuthenticated()) {
        goto('/dashboard');
      } else {
        goto('/login');
      }
    }
  });
</script>

{#if authStore.loading}
  <div class="min-h-screen bg-background flex items-center justify-center">
    <div class="flex flex-col items-center gap-4">
      <div class="size-12 rounded-full border-4 border-primary border-t-transparent animate-spin"></div>
      <p class="text-muted-foreground text-sm font-medium">Loading Artemis…</p>
    </div>
  </div>
{/if}
