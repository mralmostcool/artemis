<script>
  import Login from './pages/Login.svelte'
  import Register from './pages/Register.svelte'
  import Internal from './pages/Internal.svelte'

  let currentPage = $state('login');

  function handleLogin() {
    currentPage = 'internal';
  }

  function handleLogout() {
    currentPage = 'login';
  }

  function handleRegister() {
    currentPage = 'register';
  }

  function handleBackToLogin() {
    currentPage = 'login';
  }
</script>

{#if currentPage === 'login'}
  <Login onLogin={handleLogin} onRegister={handleRegister} />
{:else if currentPage === 'register'}
  <Register onBackToLogin={handleBackToLogin} />
{:else}
  <Internal onLogout={handleLogout} />
{/if}
