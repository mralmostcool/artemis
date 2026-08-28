<script>
  // Svelte 5 runes
  let { onLogin, onRegister } = $props();

  let email = $state('');
  let password = $state('');
  let errorMsg = $state('');

  /** @param {SubmitEvent} e */
  function handleSubmit(e) {
    e.preventDefault();
    if (!email || !password) {
      errorMsg = 'Please enter both email and password';
      return;
    }
    errorMsg = '';
    onLogin();
  }
</script>

<div class="login-container">
  <div class="login-card">
    <div class="header">
      <h2>Welcome Back</h2>
      <p>Log in to access your dashboard</p>
    </div>

    <form onsubmit={handleSubmit} class="login-form">
      {#if errorMsg}
        <div class="error-banner">
          {errorMsg}
        </div>
      {/if}

      <div class="input-group">
        <label for="email">Email</label>
        <input 
          id="email" 
          type="email" 
          bind:value={email} 
          placeholder="you@example.com" 
          required 
        />
      </div>

      <div class="input-group">
        <label for="password">Password</label>
        <input 
          id="password" 
          type="password" 
          bind:value={password} 
          placeholder="••••••••" 
          required 
        />
      </div>

      <button type="submit" class="submit-btn">
        Sign In
      </button>
    </form>

    <div class="footer">
      <button class="bypass-btn" onclick={onLogin}>
        Bypass / Preview Dashboard
      </button>
      <div style="margin-top: 12px; font-size: 14px;">
        Don't have an account? 
        <button class="bypass-btn" onclick={onRegister} style="display: inline; padding: 0; text-decoration: underline;">
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
    flex-grow: 1;
    padding: 20px;
    background: radial-gradient(circle at top left, var(--accent-bg), transparent 40%),
                radial-gradient(circle at bottom right, rgba(0,0,0,0.05), transparent 40%);
  }

  .login-card {
    background: var(--bg);
    border: 1px solid var(--border);
    border-radius: 12px;
    padding: 40px;
    width: 100%;
    max-width: 420px;
    box-shadow: var(--shadow);
    transition: transform 0.3s ease, box-shadow 0.3s ease;
  }

  .login-card:hover {
    transform: translateY(-2px);
    box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 10px 10px -5px rgba(0, 0, 0, 0.04);
  }

  .header {
    text-align: center;
    margin-bottom: 30px;
  }

  .header h2 {
    margin: 0 0 8px;
    font-size: 28px;
    font-weight: 500;
  }

  .header p {
    color: var(--text);
    font-size: 15px;
  }

  .login-form {
    display: flex;
    flex-direction: column;
    gap: 20px;
  }

  .error-banner {
    background: rgba(239, 68, 68, 0.1);
    border: 1px solid rgba(239, 68, 68, 0.3);
    color: #ef4444;
    padding: 10px 12px;
    border-radius: 6px;
    font-size: 14px;
    text-align: center;
  }

  .input-group {
    display: flex;
    flex-direction: column;
    gap: 8px;
    text-align: left;
  }

  .input-group label {
    font-size: 14px;
    font-weight: 500;
    color: var(--text-h);
  }

  .input-group input {
    padding: 12px 16px;
    border-radius: 8px;
    border: 1px solid var(--border);
    background: var(--bg);
    color: var(--text-h);
    font-size: 15px;
    outline: none;
    transition: border-color 0.2s, box-shadow 0.2s;
  }

  .input-group input:focus {
    border-color: var(--accent);
    box-shadow: 0 0 0 3px var(--accent-bg);
  }

  .submit-btn {
    background: var(--accent);
    color: #fff;
    padding: 12px;
    border-radius: 8px;
    border: none;
    font-size: 16px;
    font-weight: 500;
    cursor: pointer;
    transition: filter 0.2s, transform 0.1s;
  }

  .submit-btn:hover {
    filter: brightness(1.1);
  }

  .submit-btn:active {
    transform: scale(0.98);
  }

  .footer {
    margin-top: 24px;
    text-align: center;
    border-top: 1px solid var(--border);
    padding-top: 16px;
  }

  .bypass-btn {
    background: none;
    border: none;
    color: var(--accent);
    font-size: 14px;
    cursor: pointer;
    text-decoration: underline;
    transition: opacity 0.2s;
  }

  .bypass-btn:hover {
    opacity: 0.8;
  }
</style>
