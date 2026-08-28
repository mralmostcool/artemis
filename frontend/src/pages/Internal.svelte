<script>
  import svelteLogo from '../assets/svelte.svg'
  import viteLogo from '../assets/vite.svg'
  import heroImg from '../assets/hero.png'
  import Counter from '../components/Counter.svelte'
  import { logout } from '../lib/api'

  let { user, onLogout } = $props();

  async function handleLogout() {
    try {
      await logout();
    } catch (err) {
      console.error('Error during logout:', err);
    }
    onLogout();
  }
</script>

<header class="dashboard-header">
  <div class="logo-area">
    <img src={svelteLogo} height="24" alt="Svelte" />
    <span>Artemis Portal</span>
  </div>
  <div class="user-profile">
    {#if user}
      <span class="user-name">Welcome, {user.firstName} {user.lastName}!</span>
    {/if}
    <button class="logout-btn" onclick={handleLogout}>Log Out</button>
  </div>
</header>

<section id="center">
  <div class="hero">
    <img src={heroImg} class="base" width="170" height="179" alt="" />
    <img src={svelteLogo} class="framework" alt="Svelte logo" />
    <img src={viteLogo} class="vite" alt="Vite logo" />
  </div>
  <div>
    <h1>Get started</h1>
    <p>Edit <code>src/lib/Internal.svelte</code> and save to test <code>HMR</code></p>
  </div>
  <Counter />
</section>

<div class="ticks"></div>

<section id="next-steps">
  <div id="docs">
    <svg class="icon" role="presentation" aria-hidden="true">
      <use href="/icons.svg#documentation-icon"></use>
    </svg>
    <h2>Documentation</h2>
    <p>Your questions, answered</p>
    <ul>
      <li>
        <a href="https://vite.dev/" target="_blank" rel="noreferrer">
          <img class="logo" src={viteLogo} alt="" />
          Explore Vite
        </a>
      </li>
      <li>
        <a href="https://svelte.dev/" target="_blank" rel="noreferrer">
          <img class="button-icon" src={svelteLogo} alt="" />
          Learn more
        </a>
      </li>
    </ul>
  </div>
  <div id="social">
    <svg class="icon" role="presentation" aria-hidden="true">
      <use href="/icons.svg#social-icon"></use>
    </svg>
    <h2>Connect with us</h2>
    <p>Join the Vite community</p>
    <ul>
      <li>
        <a href="https://github.com/vitejs/vite" target="_blank" rel="noreferrer">
          <svg class="button-icon" role="presentation" aria-hidden="true">
            <use href="/icons.svg#github-icon"></use>
          </svg>
          GitHub
        </a>
      </li>
      <li>
        <a href="https://chat.vite.dev/" target="_blank" rel="noreferrer">
          <svg class="button-icon" role="presentation" aria-hidden="true">
            <use href="/icons.svg#discord-icon"></use>
          </svg>
          Discord
        </a>
      </li>
      <li>
        <a href="https://x.com/vite_js" target="_blank" rel="noreferrer">
          <svg class="button-icon" role="presentation" aria-hidden="true">
            <use href="/icons.svg#x-icon"></use>
          </svg>
          X.com
        </a>
      </li>
      <li>
        <a href="https://bsky.app/profile/vite.dev" target="_blank" rel="noreferrer">
          <svg class="button-icon" role="presentation" aria-hidden="true">
            <use href="/icons.svg#bluesky-icon"></use>
          </svg>
          Bluesky
        </a>
      </li>
    </ul>
  </div>
</section>

<div class="ticks"></div>
<section id="spacer"></section>

<style>
  .dashboard-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 16px 32px;
    border-bottom: 1px solid var(--border);
    width: 100%;
    box-sizing: border-box;
  }

  .user-profile {
    display: flex;
    align-items: center;
    gap: 16px;
  }

  .user-name {
    font-size: 14px;
    color: var(--text);
  }

  .logo-area {
    display: flex;
    align-items: center;
    gap: 12px;
    font-weight: 600;
    color: var(--text-h);
  }

  .logout-btn {
    background: transparent;
    border: 1px solid var(--border);
    color: var(--text);
    padding: 8px 16px;
    border-radius: 6px;
    cursor: pointer;
    font-size: 14px;
    transition: background 0.2s, color 0.2s;
  }

  .logout-btn:hover {
    background: var(--code-bg);
    color: var(--text-h);
  }
</style>
