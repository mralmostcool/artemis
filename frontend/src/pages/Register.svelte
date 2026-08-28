<script>
  // Svelte 5 runes
  let { onBackToLogin } = $props();

  let firstName = $state('');
  let lastName = $state('');
  let email = $state('');
  let password = $state('');
  let confirmPassword = $state('');
  let errorMsg = $state('');
  let successMsg = $state('');

  /** @param {SubmitEvent} e */
  function handleSubmit(e) {
    e.preventDefault();
    if (!firstName || !lastName || !email || !password || !confirmPassword) {
      errorMsg = 'All fields are required';
      return;
    }
    if (password !== confirmPassword) {
      errorMsg = 'Passwords do not match';
      return;
    }
    errorMsg = '';
    successMsg = 'Registration successful! Redirecting to login...';

    setTimeout(() => {
      onBackToLogin();
    }, 1500);
  }
</script>

<div class="register-container">
  <div class="register-card">
    <div class="header">
      <h2>Create Account</h2>
      <p>Sign up to get started</p>
    </div>

    <form onsubmit={handleSubmit} class="register-form">
      {#if errorMsg}
        <div class="error-banner">
          {errorMsg}
        </div>
      {/if}

      {#if successMsg}
        <div class="success-banner">
          {successMsg}
        </div>
      {/if}

      <div class="row">
        <div class="input-group">
          <label for="firstName">First Name</label>
          <input 
            id="firstName" 
            type="text" 
            bind:value={firstName} 
            placeholder="John" 
            required 
          />
        </div>

        <div class="input-group">
          <label for="lastName">Last Name</label>
          <input 
            id="lastName" 
            type="text" 
            bind:value={lastName} 
            placeholder="Doe" 
            required 
          />
        </div>
      </div>

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

      <div class="input-group">
        <label for="confirmPassword">Confirm Password</label>
        <input 
          id="confirmPassword" 
          type="password" 
          bind:value={confirmPassword} 
          placeholder="••••••••" 
          required 
        />
      </div>

      <button type="submit" class="submit-btn" disabled={!!successMsg}>
        Register
      </button>
    </form>

    <div class="footer">
      <button class="back-btn" onclick={onBackToLogin}>
        Already have an account? Log In
      </button>
    </div>
  </div>
</div>

<style>
  .register-container {
    display: flex;
    justify-content: center;
    align-items: center;
    flex-grow: 1;
    padding: 20px;
    background: radial-gradient(circle at top left, var(--accent-bg), transparent 40%),
                radial-gradient(circle at bottom right, rgba(0,0,0,0.05), transparent 40%);
  }

  .register-card {
    background: var(--bg);
    border: 1px solid var(--border);
    border-radius: 12px;
    padding: 40px;
    width: 100%;
    max-width: 480px;
    box-shadow: var(--shadow);
    transition: transform 0.3s ease, box-shadow 0.3s ease;
  }

  .register-card:hover {
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

  .register-form {
    display: flex;
    flex-direction: column;
    gap: 20px;
  }

  .row {
    display: flex;
    gap: 16px;
  }

  .row .input-group {
    flex: 1;
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

  .success-banner {
    background: rgba(16, 185, 129, 0.1);
    border: 1px solid rgba(16, 185, 129, 0.3);
    color: #10b981;
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
    width: 100%;
    box-sizing: border-box;
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
    width: 100%;
  }

  .submit-btn:hover:not(:disabled) {
    filter: brightness(1.1);
  }

  .submit-btn:active:not(:disabled) {
    transform: scale(0.98);
  }

  .submit-btn:disabled {
    opacity: 0.6;
    cursor: not-allowed;
  }

  .footer {
    margin-top: 24px;
    text-align: center;
    border-top: 1px solid var(--border);
    padding-top: 16px;
  }

  .back-btn {
    background: none;
    border: none;
    color: var(--accent);
    font-size: 14px;
    cursor: pointer;
    text-decoration: underline;
    transition: opacity 0.2s;
  }

  .back-btn:hover {
    opacity: 0.8;
  }
</style>
