# Claims Boot

Standalone Spring Boot 3.5.16 / Java 21 Maven application, beside the legacy
WAR (not a reactor module). Run `mvn spring-boot:run` here and open
`http://localhost:8081/claims/login.do`. Verify with `mvn -B verify`.
Controllers map full `/xxx.do` paths and retain legacy request parameters.
See the root README for the legacy application and capture harness.

## Shared Thymeleaf fragments

Authenticated pages use the complete table layout; Thymeleaf fragments cannot
split opening and closing table tags across separate includes. Keep the view
marker outside the layout and remove the caller's content wrapper:

```html
<th:block th:replace="~{fragments/ns :: view('/WEB-INF/jsp/home.jsp')}"></th:block>
<th:block th:replace="~{fragments/layout :: page(~{::main})}">
  <main th:remove="tag">
    <h2 th:text="#{home.heading}"></h2>
    <!-- page content; quick links are already included by page(...) -->
    <th:block th:replace="~{fragments/layout :: screenAudit}"></th:block>
  </main>
</th:block>
```

`fragments/layout` exports `page(content)`, `header(title)` (banner and navigation
rows), `nav` (quick links), `footer` (copyright row), `footerLinks`, and
`screenAudit`. `page` assembles everything except the optional screen audit.
The model supplies `asOfDate`; session attributes `user` and `displayName` have
the legacy meaning. Text comes from the unchanged `ApplicationResources` bundle.
Login remains a standalone table with a link to `@{/northstar.css}`.
The stylesheet is copied byte for byte; no CSS classes or wrappers are added.

`fragments/ns` exports the following wrapper-free helpers:

| Fragment | Arguments and output |
| --- | --- |
| `field(name,value)` | Escaped text inside the exact `<span id="f_NAME">` marker |
| `fieldTyped(name,value,type)` | Same, with legacy `money`, `integer`, `date`, or `text` formatting |
| `message(content)` | Trimmed rendered HTML/text, matching the buffered legacy message tag; pass trusted page markup or `#{key}` |
| `view(path)` | Exact `<!-- ns:view /WEB-INF/jsp/xxx.jsp -->` comment |
| `error(key)` | Exact `<!-- ns:error errors.xxx -->` comment; call where the legacy page emitted a marker |
| `errors(errors)` | Visible messages from a list of objects/maps with `key` and `message`; null/empty lists emit nothing |
| `formRow(label,control)` | Two-cell row; `label` is translated text and `control` is a fragment expression |

```html
<th:block th:replace="~{fragments/ns :: field('signedInUser',${session.user})}"></th:block>
<th:block th:replace="~{fragments/ns :: fieldTyped('reserve',${reserve},'money')}"></th:block>
<th:block th:replace="~{fragments/ns :: errors(${errors})}"></th:block>
<th:block th:each="key : ${nsValidationErrors}">
  <th:block th:replace="~{fragments/ns :: error(${key})}"></th:block>
</th:block>
<table class="form" cellpadding="3" cellspacing="0" border="0">
  <tr th:replace="~{fragments/ns :: formRow(#{login.username},~{::input})}">
    <td><input type="text" name="username" size="25"></td>
  </tr>
</table>
```

`errors` does not invent validation markers on pages (such as login) that never
emitted them. Formatting intentionally retains double rounding, long truncation,
lenient date parsing, null-to-empty values, and the tag's escaping rules so the
existing capture regular expressions keep working.
