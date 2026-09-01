<script lang="ts">
  import { authStore } from '$lib/stores/auth.svelte';
  import { Moon, Sun, Bell } from '@lucide/svelte';
  import { toggleMode } from 'mode-watcher';

  const roleLabels: Record<string, string> = {
    DG_SHIPPING_ADMIN: 'DG Shipping Admin',
    DG_SHIPPING_L1: 'DG Shipping L1',
    DG_SHIPPING_L2: 'DG Shipping L2',
    COMPANY_ADMIN: 'Company Admin',
    COMPANY_USER: 'Company User',
    INSTITUTE_ADMIN: 'Institute Admin',
    INSTITUTE_USER: 'Institute User',
    CANDIDATE: 'Seafarer Candidate'
  };

  const initials = $derived(() => {
    const p = authStore.profile;
    if (!p) return '?';
    const f = p.firstName?.[0] ?? '';
    const l = p.lastName?.[0] ?? '';
    return (f + l).toUpperCase() || p.displayName?.[0]?.toUpperCase() || '?';
  });
</script>

<header class="h-14 flex items-center justify-between px-4 border-b border-border bg-background/80 backdrop-blur-sm sticky top-0 z-20">
  <!-- Left: breadcrumb slot placeholder -->
  <div class="flex items-center gap-2 text-sm text-muted-foreground">
    <span class="font-semibold text-foreground">
      {authStore.profile?.organizationName ?? 'Artemis Platform'}
    </span>
  </div>

  <!-- Right: actions -->
  <div class="flex items-center gap-2">
    <!-- Dark mode toggle -->
    <button
      onclick={toggleMode}
      class="size-8 rounded-lg flex items-center justify-center text-muted-foreground hover:text-foreground hover:bg-accent transition-colors"
    >
      <Sun class="size-4 dark:hidden" />
      <Moon class="size-4 hidden dark:block" />
    </button>

    <!-- Notifications placeholder -->
    <button class="size-8 rounded-lg flex items-center justify-center text-muted-foreground hover:text-foreground hover:bg-accent transition-colors relative">
      <Bell class="size-4" />
    </button>

    <!-- Avatar -->
    {#if authStore.profile}
      <div class="flex items-center gap-2.5 ml-1">
        <div class="size-8 rounded-full bg-primary/15 flex items-center justify-center ring-2 ring-primary/20">
          {#if authStore.profile.avatarUrl}
            <img src={authStore.profile.avatarUrl} alt="avatar" class="size-8 rounded-full object-cover" />
          {:else}
            <span class="text-xs font-bold text-primary">{initials()}</span>
          {/if}
        </div>
        <div class="hidden sm:flex flex-col leading-none">
          <span class="text-sm font-medium text-foreground">{authStore.profile.displayName}</span>
          <span class="text-[10px] text-muted-foreground">{roleLabels[authStore.profile.role] ?? authStore.profile.role}</span>
        </div>
      </div>
    {/if}
  </div>
</header>
