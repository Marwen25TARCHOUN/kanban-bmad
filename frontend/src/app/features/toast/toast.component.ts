import { ChangeDetectionStrategy, Component, inject } from '@angular/core';

import { ToastService } from '../../core/toast.service';

/**
 * Bottom-right toast stack (EXPERIENCE.md → Toast; Accessibility Floor).
 *
 * Mounted once in `AppComponent`. Reads its stack from {@link ToastService}
 * (a signal) and renders one `<li>` per active toast. English strings this
 * story; localization lands in story 7 via ngx-translate.
 *
 * ARIA: the container carries `role="status"` + `aria-live="polite"` so a
 * new toast is announced without stealing focus (EXPERIENCE.md
 * Accessibility Floor — "not `assertive`"). The Dismiss icon-only button
 * carries an explicit `aria-label`.
 */
@Component({
  selector: 'app-toast',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <!--
      role="status" (implicit aria-live=polite) lives on the wrapping
      <div> so the inner <ul> keeps its native list semantics — screen
      readers still announce "List, N items" for users navigating by
      list. aria-live is NOT duplicated on the <ul> (role="status"
      already implies polite).
    -->
    <div role="status">
      <ul class="toast-stack">
        @for (toast of toasts(); track toast.id) {
          <li class="toast toast-error">
            <span class="toast-message">{{ toast.message }}</span>
            <button
              type="button"
              class="toast-dismiss"
              aria-label="Dismiss notification"
              (click)="dismiss(toast.id)"
            >
              <span aria-hidden="true">&times;</span>
            </button>
          </li>
        }
      </ul>
    </div>
  `,
  styleUrls: ['./toast.component.scss'],
})
export class ToastComponent {
  private readonly toastService = inject(ToastService);

  readonly toasts = this.toastService.toasts;

  dismiss(id: number): void {
    this.toastService.dismiss(id);
  }
}
