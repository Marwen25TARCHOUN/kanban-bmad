import { ChangeDetectionStrategy, Component } from '@angular/core';

/**
 * Minimal header — brand text only this story.
 *
 * The right-hand slot (LocaleSwitcher, ThemeToggle) lands in story 7 and
 * the AIBreakdownBar at header-center lands in story 6. Empty `<div>`
 * placeholders are kept in the DOM so those stories drop in without
 * re-doing the layout.
 */
@Component({
  selector: 'app-header',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <header class="app-header" role="banner">
      <div class="header-left">
        <span class="brand">Local AI Kanban</span>
      </div>
      <div class="header-center" aria-hidden="true">
        <!-- AI breakdown bar slot — story 6 -->
      </div>
      <div class="header-right" aria-hidden="true">
        <!-- LocaleSwitcher + ThemeToggle slot — story 7 -->
      </div>
    </header>
  `,
  styleUrls: ['./app-header.component.scss'],
})
export class AppHeaderComponent {}
