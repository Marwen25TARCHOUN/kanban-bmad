import { CdkDropListGroup } from '@angular/cdk/drag-drop';
import { ChangeDetectionStrategy, Component } from '@angular/core';

import { STATUS_ORDER } from '../../core/models/card';
import { ColumnContainerComponent } from './column-container.component';

/**
 * Board surface — three columns side-by-side, left-to-right:
 * TODO → IN_PROGRESS → DONE (FR-1 locked).
 *
 * The outer `<main>` carries `cdkDropListGroup` so CDK recognizes the three
 * sibling `cdkDropList`s (one per column) as valid cross-column drop
 * targets — this is what makes a drag from TODO land in IN_PROGRESS with
 * no additional `cdkDropListConnectedTo` wiring per column.
 */
@Component({
  selector: 'app-board',
  standalone: true,
  imports: [ColumnContainerComponent, CdkDropListGroup],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <main class="board" cdkDropListGroup>
      @for (status of statuses; track status) {
        <app-column-container [status]="status" />
      }
    </main>
  `,
  styleUrls: ['./board.component.scss'],
})
export class BoardComponent {
  readonly statuses = STATUS_ORDER;
}
