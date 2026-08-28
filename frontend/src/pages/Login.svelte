<script>
  import { login } from '../lib/api'

  // Svelte 5 runes
  let { onLogin, onRegister } = $props();

  let email = $state('');
  let password = $state('');
  let errorMsg = $state('');
  let loading = $state(false);

  /** @param {SubmitEvent} e */
  async function handleSubmit(e) {
    e.preventDefault();
    if (!email || !password) {
      errorMsg = 'Please enter both email and password';
      return;
    }
    
    try {
      errorMsg = '';
      loading = true;
      const user = await login(email, password);
      onLogin(user);
    } catch (err) {
      errorMsg = err instanceof Error ? err.message : 'Invalid credentials';
    } finally {
      loading = false;
    }
  }
</script>

<div class="login-container">
  <div class="login-card">
    <div class="header">
      <div class="logo-box">
        <span class="material-symbols-outlined logo-icon">blur_on</span>
      </div>
      <h2>Welcome Back</h2>
      <p>Log in to access your dashboard</p>
    </div>

    <form onsubmit={handleSubmit} class="login-form">
      {#if errorMsg}
        <div class="error-banner">
          <span class="material-symbols-outlined error-icon">error</span>
          {errorMsg}
        </div>
      {/if}

      <div class="input-group">
        <label for="email">Email</label>
        <div class="input-wrapper">
          <span class="material-symbols-outlined input-icon">mail</span>
          <input 
            id="email" 
            type="email" 
            bind:value={email} 
            placeholder="you@example.com" 
            required 
          />
        </div>
      </div>

      <div class="input-group">
        <label for="password">Password</label>
        <div class="input-wrapper">
          <span class="material-symbols-outlined input-icon">lock</span>
          <input 
            id="password" 
            type="password" 
            bind:value={password} 
            placeholder="••••••••" 
            required 
          />
        </div>
      </div>

      <button type="submit" class="submit-btn" disabled={loading}>
        {loading ? 'Signing In...' : 'Sign In'}
      </button>
    </form>

    <div class="footer">
      <button class="bypass-btn" onclick={() => onLogin({ firstName: 'Guest', lastName: 'User', email: 'guest@example.com' })}>
        Bypass / Preview Dashboard
      </button>
      <div class="signup-prompt">
        Don't have an account? 
        <button class="signup-link" onclick={onRegister}>
          Sign Up
        </button>
      </div>
    </div>
  </div>
</div>

<style>
  .login-container {
    display: flex;
    justify-content: center;
    align-items: center;
    min-height: 100vh;
    padding: 24px;
    background: radial-gradient(120% 120% at 0% 0%, rgba(55, 125, 255, 0.05) 0%, rgba(255, 255, 255, 0) 100%), var(--bg-main);
  }

  .login-card {
    background: var(--bg-card);
    border: 1px solid var(--border-color);
    border-radius: 16px;
    padding: 40px;
    width: 100%;
    max-width: 440px;
    box-shadow: var(--shadow-md);
    transition: transform 0.3s ease, box-shadow 0.3s ease;
  }

  .login-card:hover {
    transform: translateY(-2px);
    box-shadow: var(--shadow-lg);
  }

  .logo-box {
    display: inline-flex;
    justify-content: center;
    align-items: center;
    width: 48px;
    height: 48px;
    border-radius: 12px;
    background: var(--primary-light);
    color: var(--primary);
    margin-bottom: 20px;
  }

  .logo-icon {
    font-size: 28px;
  }

  .header {
    text-align: center;
    margin-bottom: 32px;
  }

  .header h2 {
    margin: 0 0 8px;
    font-size: 24px;
    font-weight: 600;
    color: var(--text-primary);
  }

  .header p {
    color: var(--text-secondary);
    font-size: 14px;
    margin: 0;
  }

  .login-form {
    display: flex;
    flex-direction: column;
    gap: 20px;
  }

  .error-banner {
    display: flex;
    align-items: center;
    gap: 8px;
    background: rgba(237, 76, 120, 0.08);
    border: 1px solid rgba(237, 76, 120, 0.2);
    color: var(--danger);
    padding: 12px;
    border-radius: 8px;
    font-size: 14px;
    text-align: left;
  }

  .error-icon {
    font-size: 18px;
  }

  .input-group {
    display: flex;
    flex-direction: column;
    gap: 8px;
    text-align: left;
  }

  .input-group label {
    font-size: 13px;
    font-weight: 500;
    color: var(--text-primary);
  }

  .input-wrapper {
    position: relative;
    display: flex;
    align-items: center;
  }

  .input-icon {
    position: absolute;
    left: 12px;
    color: var(--text-muted);
    font-size: 18px;
  }

  .input-group input {
    width: 100%;
    padding: 12px 12px 12px 40px;
    border-radius: 8px;
    border: 1px solid var(--border-color);
    background: var(--bg-card);
    color: var(--text-primary);
    font-size: 14px;
    outline: none;
    transition: border-color 0.2s, box-shadow 0.2s;
  }

  .input-group input:focus {
    border-color: var(--primary);
    box-shadow: 0 0 0 3px var(--primary-light);
  }

  .submit-btn {
    background: var(--primary);
    color: #fff;
    padding: 12px;
    border-radius: 8px;
    border: none;
    font-size: 14px;
    font-weight: 600;
    cursor: pointer;
    transition: background-color 0.2s, transform 0.1s;
    margin-top: 8px;
  }

  .submit-btn:hover:not(:disabled) {
    background-color: var(--primary-hover);
  }

  .submit-btn:active:not(:disabled) {
    transform: scale(0.98);
  }

  .submit-btn:disabled {
    opacity: 0.6;
    cursor: not-allowed;
  }

  .footer {
    margin-top: 32px;
    text-align: center;
    border-top: 1px solid var(--border-color);
    padding-top: 20px;
  }

  .bypass-btn {
    background: transparent;
    border: 1px solid var(--border-color);
    color: var(--text-secondary);
    font-size: 13px;
    font-weight: 500;
    padding: 8px 16px;
    border-radius: 6px;
    cursor: pointer;
    transition: background-color 0.2s, color 0.2s;
    width: 100%;
  }

  .bypass-btn:hover {
    background-color: var(--border-color);
    color: var(--text-primary);
  }

  .signup-prompt {
    margin-top: 16px;
    font-size: 13px;
    color: var(--text-secondary);
  }

  .signup-link {
    background: none;
    border: none;
    color: var(--primary);
    font-size: 13px;
    font-weight: 600;
    cursor: pointer;
    text-decoration: none;
    padding: 0 4px;
  }

  .signup-link:hover {
    text-decoration: underline;
  }
</style>
