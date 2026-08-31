import adapter from '@sveltejs/adapter-static';
import { vitePreprocess } from '@sveltejs/vite-plugin-svelte';

/** @type {import('@sveltejs/kit').Config} */
const config = {
	preprocess: vitePreprocess(),
	compilerOptions: {
		// Force runes mode for the project, except for libraries.
		runes: true
	},
	kit: {
		adapter: adapter(),
		prerender: {
			handleHttpError: 'warn'
		}
	}
};

export default config;
