package ca.seibelnet

import sbt._
import Keys._
import scala.collection._

/**
 * User: bseibel
 * Date: 12-04-25
 * Time: 12:02 PM
 */

object JUnitTestReporting extends Plugin {
  override def settings = Seq(
    testListeners += new JUnitTestListener("./test-reports/")
  )
}

class JUnitTestListener(val targetPath: String) extends TestReportListener {

  val outputFactory: mutable.Map[String, TestGroupXmlWriter] = mutable.Map()

  def testEvent(event: TestEvent) {
    event.detail.headOption flatMap {ev =>
      outputFactory.get(ev.fullyQualifiedName())
    } foreach { _.addEvent(event) }
  }

  def endGroup(name: String, result: TestResult.Value) {
    flushOutput(name)
  }

  def endGroup(name: String, t: Throwable) {
    flushOutput(name)
  }

  def startGroup(name: String) {
    outputFactory(name) = TestGroupXmlWriter(name)
  }

  private def flushOutput(name: String) {
    val file = new File(targetPath)
    file.mkdirs()

    outputFactory.get(name).foreach(_.write(targetPath))
  }

}
