# e2e

[![Build Status](https://travis-ci.org/Kanaka-io/e2e.svg?branch=master)](https://travis-ci.org/Kanaka-io/e2e)

e2e is the contraction of "ease", like i18n is the contraction of "internationalization".

It is a sbt plugin that helps you keep your i18n files in sync with your code.

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

## How to use it

There has been no release yet, so you'll first have to clone this repo and publish it : 

```
$ > git clone https://github.com/Kanaka-io/e2e.git
$ > cd e2e
$ > sbt publishLocal
```

Next you'll have to add the plugin to your project, by adding the following line to your `project/plugins.sbt` :

```scala
addSbtPlugin("io.kanaka" % "e2e-plugin"  % "0.1-SNAPSHOT")
```

Finally, you will need to replace `play.api.i18n.Messages` by `io.kanaka.e2e.play.Messages` wherever it is used. 
This is basically achieved by replacing this kind of import :

```scala
import play.api.i18n.{Messages, I18nSupport, MessagesApi}
```

by

```scala
import play.api.i18n.{I18nSupport, MessagesApi}
import io.kanaka.e2e.play.Messages
```

Now you're ready to check and fix your translation files. The plugin provides your project with a new sbt task `checkI18N`.
The task will allow you to interactively fix the detected problems (you may have to `clean` your project before the first 
use of `checkI18N`).

Here is what a fixing session looks like when run on the `sample` project in this repository

![Example session](http://i.imgur.com/ewjGM9p.gif)

## Caveats

e2e is only capable of verifying keys whose value is statically known, i.e. string literals.
In other words, you can write :

```scala
Messages(s"display.name.$variable")
```

But e2e will not be able to make sure that there is a `display.name.XYZ` for each possible value of `variable`.
In such case, the macro would emit a warning.
