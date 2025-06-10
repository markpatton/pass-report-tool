# PASS report tool

Read JSON retrieved from PASS like `/data/submission?filter=submitted==true;source==pass&page[totals]&page[size]=500&include=submitter,repositories,publication,grants,grants.directFunder,grants.primaryFunder`

and format it into a spreadsheet.

It also can take an argument of a blacklist file which contains email addresses, one per line. If the submitter email address matches the blacklist, that submission is not output.

Columns:
  * PASS Id (submission id)
  * Submission date
  * Submitter name
  * Submitter email
  * Repository names
  * Article title
  * DOI
  * Journal name
  * Funder name
  * Publisher name







