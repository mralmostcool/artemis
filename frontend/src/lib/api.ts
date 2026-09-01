import { supabase } from '$lib/supabase';
import { PUBLIC_API_URL } from '$env/static/public';
import { toast } from 'svelte-sonner';

type HttpMethod = 'GET' | 'POST' | 'PUT' | 'DELETE' | 'PATCH';

async function getToken(): Promise<string | null> {
	const { data } = await supabase.auth.getSession();
	return data.session?.access_token ?? null;
}

async function request<T>(
	method: HttpMethod,
	path: string,
	body?: unknown,
	params?: Record<string, string | number | boolean | undefined>
): Promise<T> {
	const token = await getToken();

	let url = `${PUBLIC_API_URL}${path}`;
	if (params) {
		const searchParams = new URLSearchParams();
		for (const [k, v] of Object.entries(params)) {
			if (v !== undefined && v !== null) {
				searchParams.set(k, String(v));
			}
		}
		const qs = searchParams.toString();
		if (qs) url += `?${qs}`;
	}

	const headers: Record<string, string> = {
		'Content-Type': 'application/json'
	};
	if (token) headers['Authorization'] = `Bearer ${token}`;

	const res = await fetch(url, {
		method,
		headers,
		body: body !== undefined ? JSON.stringify(body) : undefined
	});

	if (res.status === 401) {
		// Force re-login
		await supabase.auth.signOut();
		window.location.href = '/login';
		throw new Error('Unauthorized');
	}

	if (res.status === 403) {
		toast.error('Access denied');
		throw new Error('Forbidden');
	}

	if (!res.ok) {
		let message = `Error ${res.status}`;
		try {
			const err = await res.json();
			message = err.message ?? err.error ?? message;
		} catch {
			// ignore
		}
		toast.error(message);
		throw new Error(message);
	}

	if (res.status === 204) return undefined as T;

	return res.json() as Promise<T>;
}

export const api = {
	get<T>(path: string, params?: Record<string, string | number | boolean | undefined>): Promise<T> {
		return request<T>('GET', path, undefined, params);
	},
	post<T>(
		path: string,
		body?: unknown,
		params?: Record<string, string | number | boolean | undefined>
	): Promise<T> {
		return request<T>('POST', path, body, params);
	},
	put<T>(
		path: string,
		body?: unknown,
		params?: Record<string, string | number | boolean | undefined>
	): Promise<T> {
		return request<T>('PUT', path, body, params);
	},
	delete<T>(
		path: string,
		params?: Record<string, string | number | boolean | undefined>
	): Promise<T> {
		return request<T>('DELETE', path, undefined, params);
	},
	patch<T>(
		path: string,
		body?: unknown,
		params?: Record<string, string | number | boolean | undefined>
	): Promise<T> {
		return request<T>('PATCH', path, body, params);
	}
};
