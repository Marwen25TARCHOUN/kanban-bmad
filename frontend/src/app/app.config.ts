import { ApplicationConfig, provideZonelessChangeDetection } from '@angular/core';
import { provideHttpClient, withFetch } from '@angular/common/http';

/**
 * Root application configuration.
 *
 * - `provideZonelessChangeDetection()` — Angular 22 zoneless (architecture
 *   portfolio signal; story 3 Decisions). No `zone.js` import in main.ts.
 * - `provideHttpClient(withFetch())` — modern fetch-backed HttpClient.
 *   `ApiService` is the sole consumer (AD-9).
 */
export const appConfig: ApplicationConfig = {
  providers: [
    provideZonelessChangeDetection(),
    provideHttpClient(withFetch()),
  ],
};
