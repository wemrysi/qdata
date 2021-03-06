/*
 * Copyright 2014–2018 SlamData Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package qdata

import org.scalacheck.{Arbitrary, Gen}
import qdata.time.TimeGenerators
import slamdata.Predef.{Char, Int, List, Long, String}
import spire.math.Real

object TestDataGenerators {

  implicit val testDataArbitrary: Arbitrary[TestData] =
    Arbitrary(genTestData(6))

  // generates nested data with depth at most `maxDepth` - 1
  def genTestData(maxDepth: Int): Gen[TestData] =
    if (maxDepth < 1)
      genNonNested
    else
      Gen.oneOf(genNested(maxDepth - 1), genNonNested)

  ////

  private def genPrimitive: Gen[TestData] = Gen.oneOf[TestData](
    Gen.choose(Long.MinValue, Long.MaxValue) map TestData._Long,
    Arbitrary.arbDouble.arbitrary map TestData._Double,
    Arbitrary.arbBigDecimal.arbitrary map (dec => TestData._Real(Real(dec))),
    genUnicodeString map TestData._String,
    TestData._Null(),
    TestData._Boolean(true),
    TestData._Boolean(false))

  private def genDateTime: Gen[TestData] = Gen.oneOf[TestData](
    TimeGenerators.genLocalDateTime map TestData._LocalDateTime,
    TimeGenerators.genLocalDate map TestData._LocalDate,
    TimeGenerators.genLocalTime map TestData._LocalTime,
    TimeGenerators.genOffsetDateTime map TestData._OffsetDateTime,
    TimeGenerators.genOffsetDate map TestData._OffsetDate,
    TimeGenerators.genOffsetTime map TestData._OffsetTime,
    TimeGenerators.genInterval map TestData._Interval)

  private def genNonNested: Gen[TestData] = Gen.oneOf[TestData](
    genPrimitive,
    genDateTime)

  private def genNested(max: Int): Gen[TestData] = Gen.oneOf[TestData](
    listOfUpTo16(genTestData(max)).map(data => TestData._Array(data.toVector)),
    listOfUpTo16(Gen.zip(genUnicodeString, genTestData(max))).map(data => TestData._Object(data.toVector)),
    Gen.zip(genTestData(max), genTestData(max)) map { case (d1, d2) => TestData._Meta(d1, d2) })

  private def listOfUpTo16[A](gen: Gen[A]): Gen[List[A]] =
    for {
      n <- Gen.choose(0, 16)
      c <- Gen.listOfN[A](n, gen)
    } yield c

  // FIXME generate from the space of all valid utf8-encodable characters
  private def genUnicodeChar: Gen[Char] =
    Gen.frequency(
      (100, Gen.asciiChar),
      (1, Gen.choose('λ', '貗')))

  private def genUnicodeString: Gen[String] =
    Gen.listOf(genUnicodeChar).map(_.mkString)
}
