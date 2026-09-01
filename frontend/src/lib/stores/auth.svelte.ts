import { supabase } from '$lib/supabase';
import { api } from '$lib/api';
import type { Profile, Role } from '$lib/types';
import type { Session } from '@supabase/supabase-js';

function createAuthStore() {
	let session = $state<Session | null>(null);
	let profile = $state<Profile | null>(null);
	let loading = $state(true);

	async function loadProfile() {
		try {
			const p = await api.get<Profile>('/api/v1/auth');
			profile = p;
		} catch {
			profile = null;
		}
	}

	async function initialize() {
		loading = true;
		const { data } = await supabase.auth.getSession();
		session = data.session;
		if (session) {
			await loadProfile();
		}
		loading = false;

		supabase.auth.onAuthStateChange(async (_event, newSession) => {
			session = newSession;
			if (newSession) {
				await loadProfile();
			} else {
				profile = null;
			}
		});
	}

	async function signIn(email: string, password: string) {
		const { error } = await supabase.auth.signInWithPassword({ email, password });
		if (error) throw error;
		await loadProfile();
	}

	async function signUp(email: string, password: string) {
		const { error } = await supabase.auth.signUp({ email, password });
		if (error) throw error;
	}

	async function signOut() {
		await supabase.auth.signOut();
		session = null;
		profile = null;
	}

	function hasRole(...roles: Role[]): boolean {
		if (!profile) return false;
		return roles.includes(profile.role);
	}

	function isAuthenticated(): boolean {
		return session !== null;
	}

	return {
		get session() { return session; },
		get profile() { return profile; },
		get loading() { return loading; },
		initialize,
		signIn,
		signUp,
		signOut,
		hasRole,
		isAuthenticated,
		loadProfile
	};
}

export const authStore = createAuthStore();
