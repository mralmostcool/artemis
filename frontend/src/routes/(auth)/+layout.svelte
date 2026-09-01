<script lang="ts">
  import type { Snippet } from 'svelte';
  let { children }: { children: Snippet } = $props();
</script>

<div class="min-h-screen bg-background flex items-center justify-center p-4">
  {@render children()}
</div>
