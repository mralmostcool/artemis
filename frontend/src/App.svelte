<script>
  import { onMount } from 'svelte'
  import Login from './pages/Login.svelte'
  import Register from './pages/Register.svelte'
  import Internal from './pages/Internal.svelte'
  import { refreshTokens, getMe } from './lib/api'

  let currentPage = $state('login');
  let currentUser = $state(null);
  let loading = $state(true);

  onMount(async () => {
    try {
      const refreshed = await refreshTokens();
      if (refreshed) {
        currentUser = await getMe();
        currentPage = 'internal';
      }
    } catch (err) {
      console.error('Failed to auto-login:', err);
    } finally {
      loading = false;
    }
  });

  /** @param {any} user */
  function handleLogin(user) {
    currentUser = user;
    currentPage = 'internal';
  }

  function handleLogout() {
    currentUser = null;
    currentPage = 'login';
  }

  function handleRegister() {
    currentPage = 'register';
  }

  function handleBackToLogin() {
    currentPage = 'login';
  }
</script>

{#if loading}
  <div class="loading-screen">
    <p>Loading session...</p>
  </div>
{:else if currentPage === 'login'}
  <Login onLogin={handleLogin} onRegister={handleRegister} />
{:else if currentPage === 'register'}
  <Register onBackToLogin={handleBackToLogin} />
{:else}
  <Internal user={currentUser} onLogout={handleLogout} />
{/if}

<style>
  .loading-screen {
    display: flex;
    justify-content: center;
    align-items: center;
    height: 100vh;
    color: var(--text-primary);
    font-size: 18px;
  }
</style>
