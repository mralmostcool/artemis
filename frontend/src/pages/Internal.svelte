<script>
  import { logout } from '../lib/api'

  let { user, onLogout } = $props();

  let activeGroup = $state(null);
  let activePage = $state('Dashboard');

  let navItems = $state([
    { type: 'page', name: 'Dashboard', icon: 'space_dashboard' },
    { type: 'page', name: 'Analytics', icon: 'bar_chart' },
    { type: 'page', name: 'Consultant', icon: 'psychology' },
    {
      type: 'group',
      name: 'Financials',
      icon: 'payments',
      expanded: false,
      pages: [
        { name: 'Overview' },
        { name: 'Revenue', badge: { type: 'number', value: 12 } },
        { name: 'Expenses', badge: { type: 'warning' } }
      ]
    },
    {
      type: 'group',
      name: 'Marketing',
      icon: 'campaign',
      expanded: false,
      pages: [
        { name: 'Campaigns', badge: { type: 'error' } },
        { name: 'Audiences', badge: { type: 'number', value: 3 } }
      ]
    },
    {
      type: 'group',
      name: 'Operations',
      icon: 'precision_manufacturing',
      expanded: false,
      pages: [
        { name: 'Workflows' },
        { name: 'Logs', badge: { type: 'number', value: 5 } }
      ]
    },
    {
      type: 'group',
      name: 'System',
      icon: 'dns',
      expanded: false,
      pages: [
        { name: 'Integrations', badge: { type: 'warning' } },
        { name: 'Security' }
      ]
    }
  ]);

  /** 
   * @param {string|null} group 
   * @param {string} page 
   */
  function selectPage(group, page) {
    activeGroup = group;
    activePage = page;
  }

  /** @param {number} index */
  function toggleGroup(index) {
    if (navItems[index].type === 'group') {
      // @ts-ignore
      navItems[index].expanded = !navItems[index].expanded;
    }
  }

  async function handleLogout() {
    try {
      await logout();
    } catch (err) {
      console.error('Error during logout:', err);
    }
    onLogout();
  }
</script>

<div class="dashboard-container">
  <!-- Sidebar -->
  <aside class="sidebar">
    <div class="sidebar-header">
      <span class="material-symbols-outlined brand-icon">blur_on</span>
      <span class="brand-name">Artemis</span>
    </div>
    
    <nav class="sidebar-nav">
      {#each navItems as item, index}
        {#if item.type === 'page'}
          <button 
            class="nav-group-header" 
            class:active={activePage === item.name && activeGroup === null}
            onclick={() => selectPage(null, item.name)}
          >
            <span class="material-symbols-outlined group-icon">{item.icon}</span>
            <span class="group-title">{item.name}</span>
          </button>
        {:else if item.type === 'group'}
          <div class="nav-group" class:collapsed={!item.expanded}>
            <button class="nav-group-header" onclick={() => toggleGroup(index)}>
              <span class="material-symbols-outlined group-icon">{item.icon}</span>
              <span class="group-title">{item.name}</span>
              <span class="material-symbols-outlined expand-chevron">
                {item.expanded ? 'expand_more' : 'chevron_right'}
              </span>
            </button>
            
            {#if item.expanded}
              <ul class="nav-pages-list">
                {#each item.pages as page}
                  <li>
                    <button 
                      class="nav-page-btn" 
                      class:active={activeGroup === item.name && activePage === page.name}
                      onclick={() => selectPage(item.name, page.name)}
                    >
                      <span class="page-name">{page.name}</span>
                      
                      {#if page.badge}
                        {#if page.badge.type === 'number'}
                          <span class="number-badge">{page.badge.value}</span>
                        {:else if page.badge.type === 'warning'}
                          <span class="material-symbols-outlined warning-badge">warning</span>
                        {:else if page.badge.type === 'error'}
                          <span class="material-symbols-outlined error-badge">error</span>
                        {/if}
                      {/if}
                    </button>
                  </li>
                {/each}
              </ul>
            {/if}
          </div>
        {/if}
      {/each}
    </nav>
    
    <div class="sidebar-footer">
      <div class="user-card">
        <div class="user-avatar">
          {user ? user.firstName[0].toUpperCase() : 'G'}
        </div>
        <div class="user-info">
          <div class="user-display-name">{user ? `${user.firstName} ${user.lastName}` : 'Guest User'}</div>
          <div class="user-email">{user ? user.email : 'guest@example.com'}</div>
        </div>
        <button class="logout-icon-btn" onclick={handleLogout} title="Log Out">
          <span class="material-symbols-outlined">logout</span>
        </button>
      </div>
    </div>
  </aside>

  <!-- Main Content Panel -->
  <main class="main-panel">
    <header class="main-header">
      <div class="breadcrumbs">
        {#if activeGroup}
          <span class="breadcrumb-item">{activeGroup}</span>
          <span class="breadcrumb-separator">/</span>
        {/if}
        <span class="breadcrumb-active">{activePage}</span>
      </div>
      
      <div class="greeting-bar">
        <h1>Hello, {user ? user.firstName : 'Guest'}!</h1>
        <p>Track your sales and performance of your strategy</p>
      </div>
    </header>
    
    <div class="content-body">
      <div class="classy-hero-box">
        <h2 class="classy-page-title">{activePage}</h2>
      </div>
    </div>
  </main>
</div>

<style>
  .dashboard-container {
    display: flex;
    height: 100vh;
    width: 100vw;
    overflow: hidden;
    background-color: var(--bg-main);
  }

  /* Sidebar styling */
  .sidebar {
    width: 280px;
    flex-shrink: 0;
    background: var(--bg-sidebar);
    border-right: 1px solid var(--border-color);
    display: flex;
    flex-direction: column;
    height: 100%;
  }

  .sidebar-header {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 24px;
    border-bottom: 1px solid var(--border-color);
  }

  .brand-icon {
    font-size: 28px;
    color: var(--primary);
  }

  .brand-name {
    font-size: 18px;
    font-weight: 700;
    color: var(--text-primary);
    letter-spacing: -0.5px;
  }

  .sidebar-nav {
    flex-grow: 1;
    overflow-y: auto;
    padding: 16px 14px;
    display: flex;
    flex-direction: column;
    gap: 12px;
  }

  .nav-group {
    display: flex;
    flex-direction: column;
    gap: 4px;
  }

  button.nav-group-header {
    width: 100%;
    background: transparent;
    border: none;
    display: flex;
    align-items: center;
    gap: 10px;
    font-size: 13px;
    font-weight: 600;
    color: var(--text-primary);
    padding: 6px 8px;
    cursor: pointer;
    border-radius: 6px;
    text-align: left;
    transition: background-color 0.2s, color 0.2s;
  }

  button.nav-group-header:hover {
    background-color: var(--border-color);
  }

  button.nav-group-header.active {
    background-color: var(--primary-light);
    color: var(--primary);
  }

  button.nav-group-header.active .group-icon {
    color: var(--primary);
  }

  .group-title {
    flex-grow: 1;
  }

  .expand-chevron {
    font-size: 18px;
    color: var(--text-secondary);
  }

  .group-icon {
    font-size: 18px;
    color: var(--text-secondary);
    display: flex;
    align-items: center;
  }

  .nav-pages-list {
    list-style: none;
    padding: 0;
    margin: 4px 0 0 28px;
    display: flex;
    flex-direction: column;
    gap: 2px;
  }

  .nav-page-btn {
    width: 100%;
    background: transparent;
    border: none;
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 8px 12px;
    font-size: 13px;
    font-weight: 500;
    color: var(--text-secondary);
    border-radius: 6px;
    cursor: pointer;
    text-align: left;
    transition: background-color 0.2s, color 0.2s;
  }

  .nav-page-btn:hover {
    background-color: var(--border-color);
    color: var(--text-primary);
  }

  .nav-page-btn.active {
    background-color: var(--primary-light);
    color: var(--primary);
    font-weight: 600;
  }

  .page-name {
    flex-grow: 1;
  }

  /* Badges */
  .number-badge {
    background-color: var(--primary-light);
    color: var(--primary);
    font-size: 11px;
    font-weight: 600;
    padding: 2px 6px;
    border-radius: 10px;
  }

  .warning-badge {
    color: #ffb020;
    font-size: 16px;
    font-variation-settings: 'FILL' 1;
  }

  .error-badge {
    color: var(--danger);
    font-size: 16px;
    font-variation-settings: 'FILL' 1;
  }

  /* Sidebar footer / User profile card */
  .sidebar-footer {
    padding: 16px;
    border-top: 1px solid var(--border-color);
    background: var(--bg-sidebar);
  }

  .user-card {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 12px;
    background: var(--bg-main);
    border-radius: 10px;
    border: 1px solid var(--border-color);
  }

  .user-avatar {
    width: 36px;
    height: 36px;
    border-radius: 50%;
    background-color: var(--primary);
    color: #fff;
    display: flex;
    align-items: center;
    justify-content: center;
    font-weight: 600;
    font-size: 14px;
    flex-shrink: 0;
  }

  .user-info {
    flex-grow: 1;
    min-width: 0;
    text-align: left;
  }

  .user-display-name {
    font-size: 13px;
    font-weight: 600;
    color: var(--text-primary);
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  .user-email {
    font-size: 11px;
    color: var(--text-secondary);
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  .logout-icon-btn {
    background: transparent;
    border: none;
    color: var(--text-secondary);
    cursor: pointer;
    padding: 4px;
    border-radius: 6px;
    display: flex;
    align-items: center;
    justify-content: center;
    transition: background-color 0.2s, color 0.2s;
  }

  .logout-icon-btn:hover {
    background-color: rgba(237, 76, 120, 0.08);
    color: var(--danger);
  }

  .logout-icon-btn .material-symbols-outlined {
    font-size: 20px;
  }

  /* Main content panel */
  .main-panel {
    flex-grow: 1;
    display: flex;
    flex-direction: column;
    height: 100%;
    overflow: hidden;
  }

  .main-header {
    background: #fff;
    border-bottom: 1px solid var(--border-color);
    padding: 24px 32px;
    text-align: left;
  }

  .breadcrumbs {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 12px;
    font-weight: 500;
    color: var(--text-secondary);
    margin-bottom: 12px;
  }

  .breadcrumb-separator {
    color: var(--text-muted);
  }

  .breadcrumb-active {
    color: var(--text-primary);
    font-weight: 600;
  }

  .greeting-bar h1 {
    font-size: 24px;
    font-weight: 600;
    color: var(--text-primary);
    margin: 0 0 4px;
  }

  .greeting-bar p {
    font-size: 13px;
    color: var(--text-secondary);
    margin: 0;
  }

  .content-body {
    flex-grow: 1;
    padding: 32px;
    display: flex;
    justify-content: center;
    align-items: center;
    overflow-y: auto;
  }

  .classy-hero-box {
    text-align: center;
  }

  .classy-page-title {
    font-size: 48px;
    font-weight: 700;
    color: var(--text-primary);
    letter-spacing: -1.5px;
    margin: 0;
  }
</style>
