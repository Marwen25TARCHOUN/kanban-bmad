
- source_spec: `_bmad-output/specs/spec-kanban-bmad/stories/1-backend-scaffold-card-h2.md`
  summary: HTTP-level test pinning /actuator/health body shape to exact `{"status":"UP"}`
  evidence: AC in story 1 requires that literal body (protected by `management.endpoint.health.probes.enabled=false` + `show-components: never`), but story 1's Never list excludes `@WebMvcTest`. Verification-gap lens (pass 1, 2026-11-22) filed this as defer to story 2 where the web layer lands; a single `MockMvc` or `TestRestTemplate` assertion in story 2's test tree closes it.
