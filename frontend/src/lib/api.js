let accessToken = '';

export function getAccessToken() {
  return accessToken;
}

export function setAccessToken(token) {
  accessToken = token;
}

export function getRefreshToken() {
  return localStorage.getItem('refresh_token');
}

export function setRefreshToken(token) {
  if (token) {
    localStorage.setItem('refresh_token', token);
  } else {
    localStorage.removeItem('refresh_token');
  }
}

/**
 * Handles custom API requests
 * @param {string} url 
 * @param {any} [options] 
 */
export async function apiRequest(url, options = {}) {
  const headers = {
    'Content-Type': 'application/json',
    ...options.headers,
  };

  if (accessToken) {
    headers['Authorization'] = `Bearer ${accessToken}`;
  }

  const res = await fetch(url, { ...options, headers });
  
  if (res.status === 401 && getRefreshToken()) {
    // Attempt silent refresh
    const refreshed = await refreshTokens();
    if (refreshed) {
      // Retry with new token
      headers['Authorization'] = `Bearer ${accessToken}`;
      return await fetch(url, { ...options, headers });
    }
  }

  return res;
}

export async function login(email, password) {
  const res = await fetch('/api/v1/auth/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email, password }),
  });

  if (!res.ok) {
    const errData = await res.json().catch(() => ({}));
    throw new Error(errData.message || 'Login failed');
  }

  const data = await res.json();
  setAccessToken(data.accessToken);
  setRefreshToken(data.refreshToken);
  return data.user;
}

export async function register(firstName, lastName, email, password) {
  const res = await fetch('/api/v1/auth/register', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ firstName, lastName, email, password }),
  });

  if (!res.ok) {
    const errData = await res.json().catch(() => ({}));
    throw new Error(errData.message || 'Registration failed');
  }

  return await res.json();
}

export async function refreshTokens() {
  const token = getRefreshToken();
  if (!token) return false;

  try {
    const res = await fetch('/api/v1/auth/refresh', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ refreshToken: token }),
    });

    if (res.ok) {
      const data = await res.json();
      setAccessToken(data.accessToken);
      setRefreshToken(data.refreshToken);
      return true;
    }
  } catch (err) {
    console.error('Failed to refresh token:', err);
  }

  // If refresh failed, clear tokens
  setAccessToken('');
  setRefreshToken(null);
  return false;
}

export async function logout() {
  const token = getRefreshToken();
  if (token) {
    try {
      await fetch('/api/v1/auth/logout', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ refreshToken: token }),
      });
    } catch (err) {
      console.error('Logout request failed:', err);
    }
  }
  setAccessToken('');
  setRefreshToken(null);
}

export async function getMe() {
  const res = await apiRequest('/api/v1/auth/me');
  if (!res.ok) {
    throw new Error('Failed to fetch user info');
  }
  return await res.json();
}
