<script lang="ts">
  import { authStore } from '$lib/stores/auth.svelte';
  import type { Role } from '$lib/types';
  import type { Snippet } from 'svelte';

  let {
    roles,
    children,
    fallback
  }: {
    roles: Role[];
    children: Snippet;
    fallback?: Snippet;
  } = $props();

  const allowed = $derived(authStore.hasRole(...roles));
</script>

{#if allowed}
  {@render children()}
{:else if fallback}
  {@render fallback()}
{/if}
