# Parity decisions awaiting signoff

## UNT-9: authenticated admin list and reference screens

Freshly seeded legacy Jetty on 8080 returns HTTP 404 and
`ns:view /WEB-INF/jsp/error.jsp` for authenticated requests to
`/claims/admin/adjusters.do` and `/claims/admin/reference.do`.
Boot currently renders the ported admin templates with HTTP 200 and their
original field spans. This difference is **not signed off**.

The legacy admin module is mounted at `/admin` in `WEB-INF/web.xml`.
Its forwards in `WEB-INF/struts-config-admin.xml` point to
`/WEB-INF/jsp/admin/adjusters.jsp` and `/WEB-INF/jsp/admin/reference.jsp`.
Struts 1.3.10 resolves these through its module-relative forward machinery;
there are no corresponding resources under `/admin/WEB-INF/jsp/`.
The actions execute (observed in the Jetty log); the HTTP responses above
are the baseline, not an assumed successful admin screen.

Decision needed: preserve the observed 404/error views, or approve exposing
the existing admin JSP contents through the new templates. No legacy code
was changed to make the comparison pass. UNT-9's admin parity acceptance
remains open until this decision is made.

The separate `/admin/editAdjuster.do` path has no action mapping in the
legacy module and returns HTTP 500 with the error view. Boot preserves that
response; its edit template is ported but intentionally not exposed as a
working edit workflow.

Reproduce with both servers running on fresh seed data:

```sh
python3 claims-boot/parity/compare.py
```

Observed result: 14/16 comparisons match; only the two authenticated admin
requests above differ. Login (good, bad, empty and initial GET), home,
logout, unauthenticated redirects, edit's error response, and
`transcripts/login.json` match.
