# NorthStar transcript format

Each scenario is one JSON object in `transcripts/<scenario>.json`. The
required top-level keys are `scenario`, `description`, `actor`, `request`, and
`expected`. `request` contains `method`, a context-qualified `path` beginning
with `/claims`, and a form object. `expected`
contains `status`, `result`, `business_fields`, `validation_errors`, and
`db_state`.

`business_fields` is a flat string-to-string map extracted from
`<span id="f_NAME">VALUE</span>` elements. It contains semantic values, not
raw HTML. Money uses two decimal places and dates use `yyyy-MM-dd`.

`result` is exactly one of:

* `forward:<path>` for the JSP marker emitted by the layout
* `redirect:<path>` for a normalized HTTP redirect location
* `error:<key>` for the application error page

`validation_errors` is an ordered list of ApplicationResources keys emitted
by Struts. `db_state` is a flat map of dotted probe keys to string values.

Supported probe vocabulary:

* `claim.<id>.<field>` - workbench view endpoint
* `payment.count.claim.<id>` - payment history endpoint, counting payment
  field spans
* `payment.<id>.<field>` - payment history endpoint
* `settlement.claim.<id>.<field>` - settlement calculation endpoint
* `policy.<id>.<field>` - policy view endpoint

The capture tool resolves these through the running application's read pages;
it never opens the HSQLDB file while Jetty is running.

The harness strips `jsessionid` URL suffixes, excludes tokens and timestamps,
collapses extracted HTML whitespace, sorts JSON map keys, writes two-space
indentation, and ends every file with a newline. Run `make capture` to reset
the fixed seed, execute the ordered scenarios, and regenerate the fixtures.
Run it twice and `git diff -- transcripts/` must be empty.
