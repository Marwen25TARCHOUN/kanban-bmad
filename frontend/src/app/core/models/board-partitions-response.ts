import { Card } from './card';

/**
 * Wire shape of every `PATCH /api/cards/{id}` response (architecture AD-4,
 * story 2 Decision B1).
 *
 * Mirrors the backend `BoardPartitionsResponse` record. Each partition is a
 * fully renumbered `0..n-1` slice of its status column, or absent entirely
 * when that partition didn't change. The store replaces only the partitions
 * the server names:
 *
 * - Inline edit (title / description / priority): exactly one key — the
 *   card's current status partition updated in place.
 * - Within-column reorder: exactly one key — the renumbered partition.
 * - Cross-column move: exactly two keys — the source and target partitions,
 *   both renumbered.
 *
 * Keys are camelCase on the wire (Jackson default): `inProgress`, not
 * `IN_PROGRESS`.
 */
export interface BoardPartitionsResponse {
  todo?: Card[];
  inProgress?: Card[];
  done?: Card[];
}
