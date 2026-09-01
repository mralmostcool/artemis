<script lang="ts">
  import { goto } from '$app/navigation';
  import { onMount } from 'svelte';
  import { authStore } from '$lib/stores/auth.svelte';
  import Sidebar from '$lib/components/layout/Sidebar.svelte';
  import Navbar from '$lib/components/layout/Navbar.svelte';
  import type { Snippet } from 'svelte';

  let { children }: { children: Snippet } = $props();
  let sidebarCollapsed = $state(false);

  onMount(() => {
    // Restore sidebar state from localStorage
    const saved = localStorage.getItem('sidebar-collapsed');
    if (saved !== null) sidebarCollapsed = saved === 'true';
  });

  $effect(() => {
    // Guard: redirect if not authenticated once loading is done
    if (!authStore.loading && !authStore.isAuthenticated()) {
      goto('/login');
    }
  });

  $effect(() => {
    localStorage.setItem('sidebar-collapsed', String(sidebarCollapsed));
  });
</script>

{#if authStore.loading}
  <!-- Full-screen loading spinner while auth initializes -->
  <div class="min-h-screen bg-background flex items-center justify-center">
    <div class="flex flex-col items-center gap-4">
      <div class="size-12 rounded-full border-4 border-primary border-t-transparent animate-spin"></div>
      <p class="text-muted-foreground text-sm font-medium">Loading…</p>
    </div>
  </div>
{:else if authStore.isAuthenticated()}
  <div class="flex h-screen overflow-hidden bg-background">
    <Sidebar bind:collapsed={sidebarCollapsed} />

    <div class="flex flex-col flex-1 overflow-hidden">
      <Navbar />
      <main class="flex-1 overflow-y-auto p-6">
        {@render children()}
      </main>
    </div>
  </div>
{/if}
