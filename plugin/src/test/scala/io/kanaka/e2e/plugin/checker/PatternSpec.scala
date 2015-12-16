package io.kanaka.e2e.plugin.checker

import org.specs2.matcher.ShouldMatchers
import org.specs2.mutable.Specification

/**
  * @author Valentin Kasas
  */
class PatternSpec extends Specification with ShouldMatchers {

  "Pattern" should {

    "Detect suspicious quotes" in {
      Pattern.suspiciousQuotes("C'est la fete")           should_== Some(SuspiciousQuotesInPattern("C'est la fete", 1 :: Nil))
      Pattern.suspiciousQuotes("C''est la fete")          should_== None
      Pattern.suspiciousQuotes("'")                       should_== Some(SuspiciousQuotesInPattern("'", 0 :: Nil))
      Pattern.suspiciousQuotes("''")                      should_== None
      Pattern.suspiciousQuotes("This '{0}' is a pattern") should_== Some(SuspiciousQuotesInPattern("This '{0}' is a pattern", 9 :: Nil))
    }

  }

}
