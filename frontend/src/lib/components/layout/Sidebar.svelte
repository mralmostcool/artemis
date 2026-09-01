<script lang="ts">
  import { page } from '$app/state';
  import { goto } from '$app/navigation';
  import { authStore } from '$lib/stores/auth.svelte';
  import {
    LayoutDashboard,
    Users,
    Ship,
    FileText,
    Award,
    GraduationCap,
    Banknote,
    UserCircle,
    Anchor,
    Receipt,
    BookOpen,
    LogOut,
    ChevronLeft,
    ChevronRight,
    Shield
  } from '@lucide/svelte';

  let { collapsed = $bindable(false) }: { collapsed: boolean } = $props();

  interface NavItem {
    label: string;
    href: string;
    icon: typeof LayoutDashboard;
    roles?: string[];
  }

  const navItems: NavItem[] = [
    { label: 'Dashboard', href: '/dashboard', icon: LayoutDashboard },
    {
      label: 'Seafarers',
      href: '/seafarers',
      icon: Users,
      roles: ['DG_SHIPPING_ADMIN', 'DG_SHIPPING_L1', 'DG_SHIPPING_L2', 'COMPANY_ADMIN', 'COMPANY_USER', 'INSTITUTE_ADMIN', 'INSTITUTE_USER']
    },
    {
      label: 'Vessels & Companies',
      href: '/vessels',
      icon: Ship,
      roles: ['DG_SHIPPING_ADMIN', 'COMPANY_ADMIN', 'COMPANY_USER']
    },
    {
      label: 'Contracts',
      href: '/contracts',
      icon: FileText,
      roles: ['DG_SHIPPING_ADMIN', 'COMPANY_ADMIN', 'COMPANY_USER']
    },
    {
      label: 'Certificates',
      href: '/certificates',
      icon: Award,
      roles: ['DG_SHIPPING_ADMIN', 'DG_SHIPPING_L1', 'DG_SHIPPING_L2', 'COMPANY_ADMIN']
    },
    {
      label: 'Institutes',
      href: '/institutes',
      icon: GraduationCap,
      roles: ['DG_SHIPPING_ADMIN', 'INSTITUTE_ADMIN', 'INSTITUTE_USER']
    },
    {
      label: 'Payroll',
      href: '/payroll',
      icon: Banknote,
      roles: ['DG_SHIPPING_ADMIN', 'COMPANY_ADMIN']
    }
  ];

  const candidateItems: NavItem[] = [
    { label: 'My Profile', href: '/profile', icon: UserCircle },
    { label: 'My INDoS', href: '/profile', icon: Anchor },
    { label: 'My Contracts', href: '/seafarers/my-contracts', icon: FileText },
    { label: 'My Pay Slips', href: '/payroll/my-slips', icon: Receipt },
    { label: 'My Enrollments', href: '/enrollments', icon: BookOpen }
  ];

  const isCandidate = $derived(authStore.profile?.role === 'CANDIDATE');

  const visibleNavItems = $derived(
    isCandidate
      ? candidateItems
      : navItems.filter((item) => {
          if (!item.roles) return true;
          return authStore.hasRole(...(item.roles as Parameters<typeof authStore.hasRole>));
        })
  );

  function isActive(href: string) {
    return page.url.pathname.startsWith(href);
  }

  async function handleSignOut() {
    await authStore.signOut();
    goto('/login');
  }
</script>

<aside
  class={[
    'flex flex-col h-screen bg-sidebar border-r border-sidebar-border transition-all duration-300 ease-in-out relative',
    collapsed ? 'w-16' : 'w-64'
  ].join(' ')}
>
  <!-- Logo -->
  <div class="flex items-center gap-3 px-4 py-5 border-b border-sidebar-border overflow-hidden">
    <div class="size-8 rounded-lg bg-primary flex items-center justify-center shrink-0">
      <Shield class="size-4 text-primary-foreground" />
    </div>
    {#if !collapsed}
      <div class="flex flex-col leading-tight">
        <span class="font-bold text-sidebar-foreground text-sm tracking-wide">Artemis</span>
        <span class="text-[10px] text-sidebar-primary font-medium uppercase tracking-widest">DG Shipping</span>
      </div>
    {/if}
  </div>

  <!-- Nav -->
  <nav class="flex-1 overflow-y-auto overflow-x-hidden py-4 space-y-0.5 px-2">
    {#each visibleNavItems as item}
      {@const active = isActive(item.href)}
      <a
        href={item.href}
        title={collapsed ? item.label : undefined}
        class={[
          'flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium transition-all duration-150 group relative',
          active
            ? 'bg-sidebar-accent text-sidebar-accent-foreground shadow-sm'
            : 'text-sidebar-foreground/70 hover:bg-sidebar-accent/50 hover:text-sidebar-accent-foreground'
        ].join(' ')}
      >
        {#if active}
          <div class="absolute left-0 top-1/2 -translate-y-1/2 w-0.5 h-5 rounded-r bg-sidebar-primary"></div>
        {/if}
        <item.icon class={['size-4 shrink-0', active ? 'text-sidebar-primary' : 'text-sidebar-foreground/50 group-hover:text-sidebar-primary/70'].join(' ')} />
        {#if !collapsed}
          <span class="truncate">{item.label}</span>
        {/if}
      </a>
    {/each}
  </nav>

  <!-- Bottom actions -->
  <div class="border-t border-sidebar-border p-2 space-y-0.5">
    {#if !isCandidate}
      <a
        href="/profile"
        title={collapsed ? 'Profile' : undefined}
        class="flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium text-sidebar-foreground/70 hover:bg-sidebar-accent/50 hover:text-sidebar-accent-foreground transition-all"
      >
        <UserCircle class="size-4 shrink-0 text-sidebar-foreground/50" />
        {#if !collapsed}<span>Profile</span>{/if}
      </a>
    {/if}
    <button
      onclick={handleSignOut}
      title={collapsed ? 'Sign Out' : undefined}
      class="w-full flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium text-sidebar-foreground/70 hover:bg-destructive/10 hover:text-destructive transition-all"
    >
      <LogOut class="size-4 shrink-0" />
      {#if !collapsed}<span>Sign Out</span>{/if}
    </button>
  </div>

  <!-- Collapse toggle -->
  <button
    onclick={() => (collapsed = !collapsed)}
    class="absolute -right-3 top-16 size-6 rounded-full bg-sidebar border border-sidebar-border flex items-center justify-center shadow-sm hover:bg-sidebar-accent transition-colors z-10"
  >
    {#if collapsed}
      <ChevronRight class="size-3.5 text-sidebar-foreground/60" />
    {:else}
      <ChevronLeft class="size-3.5 text-sidebar-foreground/60" />
    {/if}
  </button>
</aside>
