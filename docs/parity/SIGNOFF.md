# Parity decisions awaiting signoff

## UNT-9: authenticated admin list and reference screens

Freshly seeded legacy Jetty on 8080 returns HTTP 404 and
`ns:view /WEB-INF/jsp/error.jsp` for authenticated requests to
`/claims/admin/adjusters.do` and `/claims/admin/reference.do`.
Boot is **ported as observed (404); exposing the admin screens needs signoff**.
The controllers still execute the action logic, then forward to the missing
module-relative resources and render the 404 error page without field spans.
The ported admin templates remain in the repository but are unreachable.

The legacy admin module is mounted at `/admin` in `WEB-INF/web.xml`.
Its forwards in `WEB-INF/struts-config-admin.xml` point to
`/WEB-INF/jsp/admin/adjusters.jsp` and `/WEB-INF/jsp/admin/reference.jsp`.
Struts 1.3.10 resolves these through its module-relative forward machinery;
there are no corresponding resources under `/admin/WEB-INF/jsp/`.
The actions execute (observed in the Jetty log); the HTTP responses above
are the baseline, not an assumed successful admin screen.

Exposing the existing admin JSP contents through the new templates still
requires signoff; this entry remains open for that decision, not resolved.
No legacy code was changed to make the comparison pass.

The separate `/admin/editAdjuster.do` path has no action mapping in the
legacy module and returns HTTP 500 with the error view. Boot preserves that
response; its edit template is ported but intentionally not exposed as a
working edit workflow.

Reproduce with both servers running on fresh seed data:

```sh
python3 claims-boot/parity/compare.py
```

Observed result: 16/16 comparisons match. Login (good, bad, empty and initial GET), home,
logout, unauthenticated redirects, edit's error response, and
`transcripts/login.json` match.
