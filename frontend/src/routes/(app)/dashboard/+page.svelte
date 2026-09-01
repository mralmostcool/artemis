<script lang="ts">
  import { onMount } from 'svelte';
  import { authStore } from '$lib/stores/auth.svelte';
  import { api } from '$lib/api';
  import StatusBadge from '$lib/components/shared/StatusBadge.svelte';
  import {
    Users, Ship, FileText, Award, GraduationCap, Banknote,
    TrendingUp, Clock, CheckCircle2, AlertCircle, ArrowRight,
    Anchor, BookOpen, Receipt, Activity
  } from '@lucide/svelte';

  // ─── Stat card data (fetched per role) ───────────────────────────────────────
  interface StatCard {
    label: string;
    value: string | number;
    sub?: string;
    icon: typeof Users;
    color: string;
    href?: string;
    loading: boolean;
  }

  let stats = $state<StatCard[]>([]);
  let recentItems = $state<{ label: string; sub: string; status: string; href: string }[]>([]);
  let pageLoading = $state(true);

  const role = $derived(authStore.profile?.role ?? '');

  const greetingTime = () => {
    const h = new Date().getHours();
    if (h < 12) return 'Good morning';
    if (h < 17) return 'Good afternoon';
    return 'Good evening';
  };

  async function loadDGAdmin() {
    const [certificates, contracts, companies] = await Promise.allSettled([
      api.get<unknown[]>('/api/v1/certificates?status=INITIATED'),
      api.get<unknown[]>('/api/v1/contracts'),
      api.get<unknown[]>('/api/v1/companies')
    ]);

    stats = [
      {
        label: 'Pending Certificates',
        value: certificates.status === 'fulfilled' ? (certificates.value as unknown[]).length : '—',
        sub: 'Awaiting L1 review',
        icon: Award,
        color: 'text-amber-500 bg-amber-500/10',
        href: '/certificates',
        loading: false
      },
      {
        label: 'Total Contracts',
        value: contracts.status === 'fulfilled' ? (contracts.value as unknown[]).length : '—',
        sub: 'All time',
        icon: FileText,
        color: 'text-blue-500 bg-blue-500/10',
        href: '/contracts',
        loading: false
      },
      {
        label: 'Shipping Companies',
        value: companies.status === 'fulfilled' ? (companies.value as unknown[]).length : '—',
        sub: 'Registered on platform',
        icon: Ship,
        color: 'text-violet-500 bg-violet-500/10',
        href: '/vessels',
        loading: false
      },
      {
        label: 'Platform Status',
        value: 'Operational',
        sub: 'All systems normal',
        icon: Activity,
        color: 'text-emerald-500 bg-emerald-500/10',
        loading: false
      }
    ];
  }

  async function loadDGReviewer() {
    const certs = await api.get<unknown[]>('/api/v1/certificates?status=INITIATED').catch(() => []);
    const l1 = await api.get<unknown[]>('/api/v1/certificates?status=L1_REVIEWED').catch(() => []);
    stats = [
      {
        label: 'Awaiting My Review',
        value: (certs as unknown[]).length,
        sub: role === 'DG_SHIPPING_L1' ? 'Needs L1 sign-off' : 'Needs L2 approval',
        icon: Clock,
        color: 'text-amber-500 bg-amber-500/10',
        href: '/certificates',
        loading: false
      },
      {
        label: role === 'DG_SHIPPING_L1' ? 'L1 Reviewed' : 'L2 Approved',
        value: (l1 as unknown[]).length,
        sub: 'This period',
        icon: CheckCircle2,
        color: 'text-emerald-500 bg-emerald-500/10',
        href: '/certificates',
        loading: false
      }
    ];
  }

  async function loadCompanyAdmin() {
    const orgId = authStore.profile?.organizationId;
    if (!orgId) return;

    const [vessels, contracts, slips] = await Promise.allSettled([
      api.get<unknown[]>(`/api/v1/companies/${orgId}/vessels`),
      api.get<unknown[]>(`/api/v1/companies/${orgId}/contracts`),
      api.get<unknown[]>(`/api/v1/payroll/companies/${orgId}/slips`)
    ]);

    stats = [
      {
        label: 'Fleet Size',
        value: vessels.status === 'fulfilled' ? (vessels.value as unknown[]).length : '—',
        sub: 'Active vessels',
        icon: Ship,
        color: 'text-blue-500 bg-blue-500/10',
        href: '/vessels',
        loading: false
      },
      {
        label: 'Active Contracts',
        value: contracts.status === 'fulfilled' ? (contracts.value as unknown[]).length : '—',
        sub: 'Seafarers on board',
        icon: FileText,
        color: 'text-violet-500 bg-violet-500/10',
        href: '/contracts',
        loading: false
      },
      {
        label: 'Pay Slips',
        value: slips.status === 'fulfilled' ? (slips.value as unknown[]).length : '—',
        sub: 'Pending payment',
        icon: Banknote,
        color: 'text-amber-500 bg-amber-500/10',
        href: '/payroll',
        loading: false
      }
    ];
  }

  async function loadInstituteAdmin() {
    const orgId = authStore.profile?.organizationId;
    if (!orgId) return;
    const institutes = await api.get<{ id: string }[]>('/api/v1/institutes').catch(() => []);
    const myInstitute = institutes.find((i) => i.id === orgId) ?? institutes[0];

    if (myInstitute) {
      const courses = await api.get<unknown[]>(`/api/v1/institutes/${myInstitute.id}/courses`).catch(() => []);
      stats = [
        {
          label: 'Active Courses',
          value: (courses as unknown[]).length,
          sub: 'Offered this term',
          icon: GraduationCap,
          color: 'text-emerald-500 bg-emerald-500/10',
          href: `/institutes/${myInstitute.id}`,
          loading: false
        }
      ];
    }
  }

  async function loadCandidate() {
    const indosLink = await api.get<{ indosMaster: { id: string } }>('/api/v1/seafarers/link').catch(() => null);
    const indosId = (indosLink as { indosMaster?: { id: string } } | null)?.indosMaster?.id;

    const linked: StatCard = {
      label: 'INDoS Status',
      value: indosId ? 'Linked' : 'Not Linked',
      sub: indosId ? 'Your seafarer record is connected' : 'Link your INDoS number to proceed',
      icon: Anchor,
      color: indosId ? 'text-emerald-500 bg-emerald-500/10' : 'text-amber-500 bg-amber-500/10',
      href: '/profile',
      loading: false
    };

    const cards: StatCard[] = [linked];

    if (indosId) {
      const [contracts, slips, enrollments] = await Promise.allSettled([
        api.get<unknown[]>(`/api/v1/seafarers/${indosId}/contracts`),
        api.get<unknown[]>(`/api/v1/payroll/seafarers/${indosId}/slips`),
        api.get<unknown[]>(`/api/v1/institutes/candidates/${indosId}/enrollments`)
      ]);
      cards.push({
        label: 'My Contracts',
        value: contracts.status === 'fulfilled' ? (contracts.value as unknown[]).length : '—',
        sub: 'Sea service records',
        icon: FileText,
        color: 'text-blue-500 bg-blue-500/10',
        href: '/contracts',
        loading: false
      });
      cards.push({
        label: 'Pay Slips',
        value: slips.status === 'fulfilled' ? (slips.value as unknown[]).length : '—',
        sub: 'Earnings history',
        icon: Receipt,
        color: 'text-violet-500 bg-violet-500/10',
        href: '/payroll',
        loading: false
      });
      cards.push({
        label: 'Enrollments',
        value: enrollments.status === 'fulfilled' ? (enrollments.value as unknown[]).length : '—',
        sub: 'Training courses',
        icon: BookOpen,
        color: 'text-emerald-500 bg-emerald-500/10',
        href: '/enrollments',
        loading: false
      });
    }

    stats = cards;
  }

  onMount(async () => {
    pageLoading = true;
    try {
      switch (role) {
        case 'DG_SHIPPING_ADMIN': await loadDGAdmin(); break;
        case 'DG_SHIPPING_L1':
        case 'DG_SHIPPING_L2': await loadDGReviewer(); break;
        case 'COMPANY_ADMIN': await loadCompanyAdmin(); break;
        case 'COMPANY_USER':
          stats = [{ label: 'Quick Access', value: 'Vessels', sub: 'Manage crew assignments', icon: Ship, color: 'text-blue-500 bg-blue-500/10', href: '/vessels', loading: false }];
          break;
        case 'INSTITUTE_ADMIN':
        case 'INSTITUTE_USER': await loadInstituteAdmin(); break;
        case 'CANDIDATE': await loadCandidate(); break;
      }
    } finally {
      pageLoading = false;
    }
  });
</script>

<svelte:head>
  <title>Dashboard — Artemis</title>
</svelte:head>

<div class="space-y-8 max-w-7xl mx-auto">
  <!-- Hero greeting -->
  <div class="relative overflow-hidden rounded-2xl bg-gradient-to-br from-primary/20 via-primary/10 to-amber-400/5 border border-primary/20 p-6">
    <div class="absolute inset-0 bg-grid-white/5 [mask-image:linear-gradient(0deg,transparent,white)]"></div>
    <div class="relative">
      <p class="text-sm text-primary/80 font-medium">{greetingTime()},</p>
      <h1 class="text-2xl font-bold text-foreground mt-1">
        {authStore.profile?.displayName ?? 'Welcome'}
      </h1>
      <p class="text-sm text-muted-foreground mt-1">
        {#if authStore.profile?.organizationName}
          {authStore.profile.organizationName} ·
        {/if}
        Artemis Maritime Management Platform
      </p>
    </div>
    <!-- Decorative ship silhouette -->
    <div class="absolute right-6 top-1/2 -translate-y-1/2 opacity-10">
      <Ship class="size-24 text-primary" />
    </div>
  </div>

  <!-- Stats grid -->
  {#if pageLoading}
    <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
      {#each Array(4) as _}
        <div class="rounded-xl border border-border bg-card p-5 animate-pulse space-y-3">
          <div class="flex justify-between">
            <div class="h-4 w-24 rounded bg-muted"></div>
            <div class="size-10 rounded-lg bg-muted"></div>
          </div>
          <div class="h-8 w-16 rounded bg-muted"></div>
          <div class="h-3 w-32 rounded bg-muted"></div>
        </div>
      {/each}
    </div>
  {:else}
    <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
      {#each stats as stat}
        <div class={['group rounded-xl border border-border bg-card p-5 transition-all duration-200 hover:shadow-md hover:shadow-black/5 hover:-translate-y-0.5', stat.href ? 'cursor-pointer' : ''].join(' ')}
          role={stat.href ? 'link' : undefined}
          onclick={() => stat.href && (window.location.href = stat.href)}
          onkeydown={(e) => e.key === 'Enter' && stat.href && (window.location.href = stat.href)}
          tabindex={stat.href ? 0 : undefined}
        >
          <div class="flex items-start justify-between mb-3">
            <p class="text-sm font-medium text-muted-foreground">{stat.label}</p>
            <div class={['size-10 rounded-lg flex items-center justify-center shrink-0', stat.color].join(' ')}>
              <stat.icon class="size-5" />
            </div>
          </div>
          <p class="text-3xl font-bold text-foreground tabular-nums">{stat.value}</p>
          {#if stat.sub}
            <p class="text-xs text-muted-foreground mt-1.5">{stat.sub}</p>
          {/if}
          {#if stat.href}
            <div class="flex items-center gap-1 mt-3 text-xs font-medium text-primary opacity-0 group-hover:opacity-100 transition-opacity">
              View details <ArrowRight class="size-3" />
            </div>
          {/if}
        </div>
      {/each}
    </div>
  {/if}

  <!-- Quick Actions -->
  <div>
    <h2 class="text-sm font-semibold text-muted-foreground uppercase tracking-wider mb-4">Quick Actions</h2>
    <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3">
      {#if authStore.hasRole('DG_SHIPPING_ADMIN')}
        <a href="/seafarers" class="flex items-center gap-4 rounded-xl border border-border bg-card p-4 hover:bg-accent/30 transition-all group">
          <div class="size-10 rounded-lg bg-blue-500/10 flex items-center justify-center">
            <Users class="size-5 text-blue-500" />
          </div>
          <div class="flex-1 min-w-0">
            <p class="font-medium text-foreground text-sm">Manage Seafarers</p>
            <p class="text-xs text-muted-foreground">Search & manage INDoS records</p>
          </div>
          <ArrowRight class="size-4 text-muted-foreground opacity-0 group-hover:opacity-100 group-hover:translate-x-0.5 transition-all" />
        </a>
        <a href="/certificates" class="flex items-center gap-4 rounded-xl border border-border bg-card p-4 hover:bg-accent/30 transition-all group">
          <div class="size-10 rounded-lg bg-amber-500/10 flex items-center justify-center">
            <Award class="size-5 text-amber-500" />
          </div>
          <div class="flex-1 min-w-0">
            <p class="font-medium text-foreground text-sm">Certificate Queue</p>
            <p class="text-xs text-muted-foreground">Review pending certificates</p>
          </div>
          <ArrowRight class="size-4 text-muted-foreground opacity-0 group-hover:opacity-100 group-hover:translate-x-0.5 transition-all" />
        </a>
        <a href="/payroll" class="flex items-center gap-4 rounded-xl border border-border bg-card p-4 hover:bg-accent/30 transition-all group">
          <div class="size-10 rounded-lg bg-emerald-500/10 flex items-center justify-center">
            <Banknote class="size-5 text-emerald-500" />
          </div>
          <div class="flex-1 min-w-0">
            <p class="font-medium text-foreground text-sm">Run Payroll</p>
            <p class="text-xs text-muted-foreground">Generate monthly pay slips</p>
          </div>
          <ArrowRight class="size-4 text-muted-foreground opacity-0 group-hover:opacity-100 group-hover:translate-x-0.5 transition-all" />
        </a>
      {/if}

      {#if authStore.hasRole('DG_SHIPPING_L1', 'DG_SHIPPING_L2')}
        <a href="/certificates" class="flex items-center gap-4 rounded-xl border border-amber-200 dark:border-amber-900/40 bg-amber-50/50 dark:bg-amber-900/10 p-4 hover:bg-amber-100/50 dark:hover:bg-amber-900/20 transition-all group">
          <div class="size-10 rounded-lg bg-amber-500/15 flex items-center justify-center">
            <Award class="size-5 text-amber-500" />
          </div>
          <div class="flex-1 min-w-0">
            <p class="font-medium text-foreground text-sm">Review Certificates</p>
            <p class="text-xs text-muted-foreground">Pending your {role === 'DG_SHIPPING_L1' ? 'L1' : 'L2'} review</p>
          </div>
          <ArrowRight class="size-4 text-amber-500 opacity-0 group-hover:opacity-100 group-hover:translate-x-0.5 transition-all" />
        </a>
      {/if}

      {#if authStore.hasRole('COMPANY_ADMIN', 'COMPANY_USER')}
        <a href="/vessels" class="flex items-center gap-4 rounded-xl border border-border bg-card p-4 hover:bg-accent/30 transition-all group">
          <div class="size-10 rounded-lg bg-blue-500/10 flex items-center justify-center">
            <Ship class="size-5 text-blue-500" />
          </div>
          <div class="flex-1 min-w-0">
            <p class="font-medium text-foreground text-sm">Fleet Management</p>
            <p class="text-xs text-muted-foreground">Vessels & crew allocations</p>
          </div>
          <ArrowRight class="size-4 text-muted-foreground opacity-0 group-hover:opacity-100 group-hover:translate-x-0.5 transition-all" />
        </a>
        <a href="/contracts" class="flex items-center gap-4 rounded-xl border border-border bg-card p-4 hover:bg-accent/30 transition-all group">
          <div class="size-10 rounded-lg bg-violet-500/10 flex items-center justify-center">
            <FileText class="size-5 text-violet-500" />
          </div>
          <div class="flex-1 min-w-0">
            <p class="font-medium text-foreground text-sm">Contracts</p>
            <p class="text-xs text-muted-foreground">Draft, sign-on, sign-off</p>
          </div>
          <ArrowRight class="size-4 text-muted-foreground opacity-0 group-hover:opacity-100 group-hover:translate-x-0.5 transition-all" />
        </a>
      {/if}

      {#if authStore.hasRole('INSTITUTE_ADMIN', 'INSTITUTE_USER')}
        <a href="/institutes" class="flex items-center gap-4 rounded-xl border border-border bg-card p-4 hover:bg-accent/30 transition-all group">
          <div class="size-10 rounded-lg bg-emerald-500/10 flex items-center justify-center">
            <GraduationCap class="size-5 text-emerald-500" />
          </div>
          <div class="flex-1 min-w-0">
            <p class="font-medium text-foreground text-sm">Training Courses</p>
            <p class="text-xs text-muted-foreground">Manage courses & enrollments</p>
          </div>
          <ArrowRight class="size-4 text-muted-foreground opacity-0 group-hover:opacity-100 group-hover:translate-x-0.5 transition-all" />
        </a>
      {/if}

      {#if authStore.hasRole('CANDIDATE')}
        <a href="/profile" class="flex items-center gap-4 rounded-xl border border-border bg-card p-4 hover:bg-accent/30 transition-all group">
          <div class="size-10 rounded-lg bg-primary/10 flex items-center justify-center">
            <Anchor class="size-5 text-primary" />
          </div>
          <div class="flex-1 min-w-0">
            <p class="font-medium text-foreground text-sm">Link INDoS Number</p>
            <p class="text-xs text-muted-foreground">Connect your seafarer record</p>
          </div>
          <ArrowRight class="size-4 text-muted-foreground opacity-0 group-hover:opacity-100 group-hover:translate-x-0.5 transition-all" />
        </a>
        <a href="/institutes" class="flex items-center gap-4 rounded-xl border border-border bg-card p-4 hover:bg-accent/30 transition-all group">
          <div class="size-10 rounded-lg bg-emerald-500/10 flex items-center justify-center">
            <GraduationCap class="size-5 text-emerald-500" />
          </div>
          <div class="flex-1 min-w-0">
            <p class="font-medium text-foreground text-sm">Browse Training</p>
            <p class="text-xs text-muted-foreground">Find & enroll in pre-sea courses</p>
          </div>
          <ArrowRight class="size-4 text-muted-foreground opacity-0 group-hover:opacity-100 group-hover:translate-x-0.5 transition-all" />
        </a>
        <a href="/enrollments" class="flex items-center gap-4 rounded-xl border border-border bg-card p-4 hover:bg-accent/30 transition-all group">
          <div class="size-10 rounded-lg bg-blue-500/10 flex items-center justify-center">
            <BookOpen class="size-5 text-blue-500" />
          </div>
          <div class="flex-1 min-w-0">
            <p class="font-medium text-foreground text-sm">My Enrollments</p>
            <p class="text-xs text-muted-foreground">Track training progress</p>
          </div>
          <ArrowRight class="size-4 text-muted-foreground opacity-0 group-hover:opacity-100 group-hover:translate-x-0.5 transition-all" />
        </a>
      {/if}

      <!-- Always: profile -->
      <a href="/profile" class="flex items-center gap-4 rounded-xl border border-border bg-card p-4 hover:bg-accent/30 transition-all group">
        <div class="size-10 rounded-lg bg-muted flex items-center justify-center">
          {#if authStore.profile?.avatarUrl}
            <img src={authStore.profile.avatarUrl} alt="avatar" class="size-10 rounded-lg object-cover" />
          {:else}
            <span class="text-sm font-bold text-muted-foreground">
              {(authStore.profile?.firstName?.[0] ?? '') + (authStore.profile?.lastName?.[0] ?? '')}
            </span>
          {/if}
        </div>
        <div class="flex-1 min-w-0">
          <p class="font-medium text-foreground text-sm">My Profile</p>
          <p class="text-xs text-muted-foreground truncate">{authStore.profile?.email ?? ''}</p>
        </div>
        <ArrowRight class="size-4 text-muted-foreground opacity-0 group-hover:opacity-100 group-hover:translate-x-0.5 transition-all" />
      </a>
    </div>
  </div>
</div>
