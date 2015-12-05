# e2e

## Motivation

Internationalization can be a painful process. There are many libraries out there that do their best to tackle this issue 
and manage to do a perfectly fine job, but there is one issue that most seem to overlook : ensure that every i18n key
used in the source code has an actual label defined for it in each translation file.

[The Daily WTF](http://www.thedailywtf.com/) is filled with screenshots of applications displaying some shameful placeholder
to the end-user, just because someone forgot to update `messages.en` or mistakenly typed `Messages("gret.user")` instead of 
`Messages("greet.user")`.
 
Yet, the information needed to check that consistency between the code and the i18n files is statically deductible. Thus,
this should be the task of out build tool to verify and/or guaranty this consistency.
 
## Solution

We define a macro that gathers all the i18n keys that are effectively used in the source code and stores that usage data on disk.

Next, we provide a sbt task that check every message file against that usage data to find the missing translations.

